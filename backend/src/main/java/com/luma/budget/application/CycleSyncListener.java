package com.luma.budget.application;

import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.CycleStatus;
import com.luma.budget.domain.ItemSource;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.income.domain.Income;
import com.luma.income.domain.IncomeCreatedEvent;
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
 *   <li><b>Lo nuevo SI entra.</b> Capturar el sueldo a media quincena y ver un
 *       presupuesto sin ingresos es desconcertante, y la explicacion —"es que el
 *       ciclo ya estaba abierto"— no le sirve de nada a quien solo quiere saber
 *       cuanto le queda.
 *   <li><b>Lo editado NO.</b> Cambiar el monto de un ingreso no reescribe el
 *       renglon que ya existe en el ciclo. Los renglones son copias, y eso es lo
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

    public CycleSyncListener(
            BudgetCycleRepository cycles,
            CycleItemRepository items,
            UserPreferencesService preferences) {
        this.cycles = cycles;
        this.items = items;
        this.preferences = preferences;
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

        Optional<BudgetCycle> active = cycles.findFirstByUserIdAndStatusOrderByStartDateDesc(
                income.getUserId(), CycleStatus.ACTIVE);

        if (active.isEmpty()) {
            // Sin ciclo abierto no hay nada que sincronizar. El ingreso entrara
            // cuando se abra el siguiente.
            return;
        }

        BudgetCycle cycle = active.get();
        List<LocalDate> occurrences = income.schedule().occurrencesIn(cycle.period());

        if (occurrences.isEmpty()) {
            // El ingreso existe pero no cae en este periodo: por ejemplo uno
            // anual cuyo mes ya paso. No es un error.
            return;
        }

        UserPreferences prefs = preferences.getOrCreate(income.getUserId());

        // Los renglones nuevos van al final. Se cuenta lo que ya hay en lugar de
        // consultar el maximo: la lista se necesita de todos modos y un ciclo
        // tiene decenas de renglones, no miles.
        int order = items.findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(cycle.getId())
                .size();

        List<CycleItem> nuevos = new ArrayList<>();
        for (LocalDate date : occurrences) {
            nuevos.add(CycleItem.materialize(
                    cycle.getId(),
                    CycleItemType.INCOME,
                    ItemSource.INCOME,
                    income.getId(),
                    income.getName(),
                    null,
                    income.money(prefs.getCurrency()),
                    date,
                    // Un ingreso de monto variable no se da por seguro: pide
                    // confirmacion, igual que un gasto variable.
                    income.requiresReview() ? ItemStatus.NEEDS_REVIEW : ItemStatus.PENDING,
                    null,
                    order++));
        }

        items.saveAll(nuevos);

        log.info(
                "Ingreso {} agregado al ciclo en curso {} con {} renglon(es)",
                income.getPublicId(),
                cycle.getPublicId(),
                nuevos.size());
    }
}
