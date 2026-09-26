package com.luma.budget.application;

import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.CycleStatus;
import com.luma.budget.domain.ItemSource;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.model.Money;
import com.luma.budget.domain.RecurrenceSchedule;
import com.luma.expenses.domain.Expense;
import com.luma.expenses.domain.ExpenseCreatedEvent;
import com.luma.budget.domain.CyclePlanner;
import com.luma.budget.domain.Flexibility;
import com.luma.income.domain.Income;
import com.luma.income.domain.IncomeCreatedEvent;
import com.luma.savings.domain.SavingsGoal;
import com.luma.savings.domain.SavingsGoalCreatedEvent;
import com.luma.users.application.UserPreferencesService;
import com.luma.users.domain.UserPreferences;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mete al ciclo en curso lo que se captura mientras ya esta abierto.
 *
 * <p>La regla del producto tiene dos mitades y las dos importan:
 *
 * <ul>
 *   <li><b>Lo nuevo SI entra.</b> Capturar el sueldo o la renta a media quincena y
 *       ver un presupuesto sin ellos es desconcertante, y la explicacion —"es
 *       que el ciclo ya estaba abierto"— no le sirve de nada a quien solo
 *       quiere saber cuanto le queda.
 *   <li><b>Lo editado NO.</b> Cambiar el monto no reescribe el renglon que ya
 *       existe en el ciclo. Los renglones son copias, y eso es lo
 *       que hace que un ciclo signifique algo: si se actualizaran solos, lo que
 *       revisaste ayer podria ser otra cosa hoy.
 * </ul>
 *
 * <p>Esta clase vive en el modulo de presupuesto, no en el de ingresos, para que
 * la dependencia siga yendo en un solo sentido. Presupuesto conoce a ingresos;
 * ingresos no sabe que existe el presupuesto.
 */
@Component
public class CycleSyncListener {

    private static final Logger log = LoggerFactory.getLogger(CycleSyncListener.class);

    private final BudgetCycleRepository cycles;
    private final CycleItemRepository items;
    private final UserPreferencesService preferences;
    private final CycleMaterializer materializer;

    public CycleSyncListener(
            BudgetCycleRepository cycles,
            CycleItemRepository items,
            UserPreferencesService preferences,
            CycleMaterializer materializer) {
        this.cycles = cycles;
        this.items = items;
        this.preferences = preferences;
        this.materializer = materializer;
    }

    /**
     * Corre dentro de la misma transaccion que creo el ingreso.
     *
     * <p>Deliberado: si materializar falla, tampoco se guarda el ingreso. Es
     * preferible un error claro a un ingreso guardado con un ciclo a medias.
     */
    @EventListener
    @Transactional
    public void onIncomeCreated(IncomeCreatedEvent event) {
        Income income = event.income();

        materializarEnCicloAbierto(
                income.getUserId(),
                income.schedule(),
                (cycle, currency, date, order) -> CycleItem.materialize(
                        cycle.getId(),
                        CycleItemType.INCOME,
                        ItemSource.INCOME,
                        income.getId(),
                        income.getName(),
                        null,
                        income.money(currency),
                        date,
                        // Un ingreso de monto variable no se da por seguro.
                        income.requiresReview() ? ItemStatus.NEEDS_REVIEW : ItemStatus.PENDING,
                        null,
                        order),
                income.getPublicId());
    }

    @EventListener
    @Transactional
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        Expense expense = event.expense();

        materializarEnCicloAbierto(
                expense.getUserId(),
                expense.schedule(),
                (cycle, currency, date, order) -> CycleItem.materialize(
                        cycle.getId(),
                        expense.isVariable()
                                ? CycleItemType.VARIABLE_EXPENSE
                                : CycleItemType.FIXED_EXPENSE,
                        ItemSource.EXPENSE,
                        expense.getId(),
                        expense.getName(),
                        expense.getCategoryId(),
                        expense.money(currency),
                        date,
                        // Un gasto variable no tiene monto confiable hasta que
                        // la persona lo confirma.
                        expense.isVariable() ? ItemStatus.NEEDS_REVIEW : ItemStatus.PENDING,
                        expense.getFlexibility(),
                        order),
                expense.getPublicId());
    }

    /**
     * Una meta nueva tambien entra al ciclo abierto.
     *
     * <p>No usa el ayudante comun porque una meta no tiene calendario: aporta
     * una vez por ciclo, al cierre, y su monto depende del periodo — con fecha
     * objetivo se reparte entre los ciclos que faltan.
     */
    @EventListener
    @Transactional
    public void onSavingsGoalCreated(SavingsGoalCreatedEvent event) {
        SavingsGoal goal = event.goal();

        if (!goal.affectsBudget()) {
            // Una meta en modo manual no resta del presupuesto.
            return;
        }

        Optional<BudgetCycle> active = cycles.findFirstByUserIdAndStatusOrderByStartDateDesc(
                goal.getUserId(), CycleStatus.ACTIVE);

        if (active.isEmpty()) {
            return;
        }

        BudgetCycle cycle = active.get();
        UserPreferences prefs = preferences.getOrCreate(goal.getUserId());
        CyclePlanner planner =
                CyclePlanner.of(prefs.getBudgetCycleType(), prefs.getCycleAnchorDay());

        Money aporte = materializer.plannedContribution(
                goal, prefs.getCurrency(), cycle.period(), planner);

        if (!aporte.isPositive()) {
            // Una meta sin plan calculable no entra. No es un error.
            return;
        }

        int order = items.findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(cycle.getId())
                .size();

        items.save(CycleItem.materialize(
                cycle.getId(),
                CycleItemType.SAVING,
                ItemSource.SAVINGS_GOAL,
                goal.getId(),
                goal.getName(),
                null,
                aporte,
                // El aporte se espera al cierre del ciclo, no en una fecha
                // concreta: no es una factura con vencimiento.
                cycle.period().end(),
                ItemStatus.PENDING,
                Flexibility.FLEXIBLE,
                order));

        log.info(
                "Meta {} agregada al ciclo en curso {}",
                goal.getPublicId(),
                cycle.getPublicId());
    }

    /** Construye el renglon que corresponde a una ocurrencia concreta. */
    @FunctionalInterface
    private interface ConstructorDeRenglon {
        CycleItem construir(BudgetCycle cycle, String currency, LocalDate date, int order);
    }

    /**
     * La parte comun a ingresos y gastos.
     *
     * <p>Lo unico que cambia entre los dos es COMO se arma cada renglon; el
     * resto —buscar el ciclo abierto, calcular las ocurrencias, decidir el
     * orden— es identico, y duplicarlo era pedir que se separaran con el tiempo.
     */
    private void materializarEnCicloAbierto(
            Long userId,
            RecurrenceSchedule schedule,
            ConstructorDeRenglon constructor,
            String origenParaElLog) {

        Optional<BudgetCycle> active =
                cycles.findFirstByUserIdAndStatusOrderByStartDateDesc(userId, CycleStatus.ACTIVE);

        if (active.isEmpty()) {
            // Sin ciclo abierto no hay nada que sincronizar: entrara cuando se
            // abra el siguiente.
            return;
        }

        BudgetCycle cycle = active.get();
        List<LocalDate> occurrences = schedule.occurrencesIn(cycle.period());

        if (occurrences.isEmpty()) {
            // Existe pero no cae en este periodo. No es un error.
            return;
        }

        UserPreferences prefs = preferences.getOrCreate(userId);

        // Los renglones nuevos van al final. Se cuenta lo que ya hay en lugar de
        // consultar el maximo: la lista se necesita de todos modos y un ciclo
        // tiene decenas de renglones, no miles.
        int order = items.findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(cycle.getId())
                .size();

        List<CycleItem> nuevos = new ArrayList<>();
        for (LocalDate date : occurrences) {
            nuevos.add(constructor.construir(cycle, prefs.getCurrency(), date, order++));
        }

        items.saveAll(nuevos);

        log.info(
                "{} agregado al ciclo en curso {} con {} renglon(es)",
                origenParaElLog,
                cycle.getPublicId(),
                nuevos.size());
    }
}
