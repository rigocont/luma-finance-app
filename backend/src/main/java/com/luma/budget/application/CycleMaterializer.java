package com.luma.budget.application;

import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.BudgetPeriod;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.CyclePlanner;
import com.luma.budget.domain.ExpenseAllocationPolicy;
import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.ItemSource;
import com.luma.budget.domain.ItemStatus;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.model.Money;
import com.luma.expenses.domain.Expense;
import com.luma.income.domain.Income;
import com.luma.savings.domain.ContributionMode;
import com.luma.savings.domain.SavingsGoal;
import com.luma.savings.domain.SavingsPlanCalculator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Convierte las plantillas del usuario en los renglones de un ciclo.
 *
 * <p>Es el momento en que el presupuesto deja de ser una configuracion y se
 * vuelve un plan concreto con fechas y montos. Lo que se genera aqui es una
 * copia: a partir de este punto, cambiar la plantilla no altera este ciclo.
 *
 * <p>Un ingreso o gasto puede producir VARIOS renglones en un mismo ciclo. Una
 * renta mensual cae dos veces en un bimestre, y un sueldo quincenal dos veces en
 * un ciclo mensual. Cada ocurrencia es su propio renglon, con su propia fecha.
 */
@Component
public class CycleMaterializer {

    public List<CycleItem> materialize(
            BudgetCycle cycle,
            String currency,
            ExpenseAllocationPolicy policy,
            CyclePlanner planner,
            List<Income> incomes,
            List<Expense> expenses,
            List<SavingsGoal> goals) {

        if (policy == ExpenseAllocationPolicy.PRORATE) {
            // Fallar de forma explicita en lugar de caer en silencio a la otra
            // politica: un presupuesto calculado con una regla distinta de la que
            // la persona eligio es peor que un error claro.
            throw new BusinessRuleException(
                    "La politica de prorrateo todavia no esta disponible. "
                            + "Cambia la preferencia a asignacion por fecha de vencimiento.");
        }

        BudgetPeriod period = cycle.period();
        List<CycleItem> items = new ArrayList<>();
        int order = 0;

        for (Income income : incomes) {
            if (!income.isActive()) {
                continue;
            }
            for (LocalDate date : income.schedule().occurrencesIn(period)) {
                items.add(CycleItem.materialize(
                        cycle.getId(),
                        CycleItemType.INCOME,
                        ItemSource.INCOME,
                        income.getId(),
                        income.getName(),
                        null,
                        income.money(currency),
                        date,
                        // Un ingreso de monto variable (comisiones, ventas) no
                        // se da por seguro: pide confirmacion, igual que un
                        // gasto variable. Un presupuesto que promete dinero que
                        // puede no llegar es justo el error que LUMA evita.
                        income.requiresReview()
                                ? ItemStatus.NEEDS_REVIEW
                                : ItemStatus.PENDING,
                        null,
                        order++));
            }
        }

        for (Expense expense : expenses) {
            if (!expense.isActive()) {
                continue;
            }
            for (LocalDate date : expense.schedule().occurrencesIn(period)) {
                items.add(CycleItem.materialize(
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
                        // Un gasto variable no tiene monto confiable hasta que la
                        // persona lo confirma. Se materializa esperando revision.
                        expense.isVariable() ? ItemStatus.NEEDS_REVIEW : ItemStatus.PENDING,
                        expense.getFlexibility(),
                        order++));
            }
        }

        for (SavingsGoal goal : goals) {
            if (!goal.affectsBudget()) {
                continue;
            }

            Money contribution = plannedContribution(goal, currency, period, planner);
            if (!contribution.isPositive()) {
                continue;
            }

            items.add(CycleItem.materialize(
                    cycle.getId(),
                    CycleItemType.SAVING,
                    ItemSource.SAVINGS_GOAL,
                    goal.getId(),
                    goal.getName(),
                    null,
                    contribution,
                    // El aporte se espera al cierre del ciclo, no en una fecha
                    // concreta: no es una factura con vencimiento.
                    period.end(),
                    ItemStatus.PENDING,
                    Flexibility.FLEXIBLE,
                    order++));
        }

        return items;
    }

    /**
     * Cuanto aportar a una meta en este ciclo.
     *
     * <p>Con fecha objetivo, lo calcula el sistema a partir de lo que falta y los
     * ciclos restantes. Con monto fijo, es lo que decidio la persona.
     *
     * <p>Es publico porque lo necesitan dos caminos: abrir un ciclo completo y
     * meter una meta recien creada al ciclo que ya estaba abierto. Tener dos
     * copias de esta formula seria pedir que se separaran.
     */
    public Money plannedContribution(
            SavingsGoal goal, String currency, BudgetPeriod period, CyclePlanner planner) {

        if (goal.getContributionMode() == ContributionMode.FIXED_PER_CYCLE) {
            return goal.plannedContribution(currency);
        }

        if (goal.getTargetDate() == null) {
            // Sin fecha no hay nada que repartir. No es un error: es una meta
            // mal configurada que simplemente no entra en este ciclo.
            return Money.zero(currency);
        }

        return SavingsPlanCalculator.contributionPerCycle(
                goal.target(currency), goal.saved(currency), goal.getTargetDate(), period, planner);
    }
}
