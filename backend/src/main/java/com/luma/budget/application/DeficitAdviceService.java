package com.luma.budget.application;

import com.luma.budget.domain.BudgetCalculator;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CutCandidate;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.DeficitAdvisor;
import com.luma.budget.domain.ItemSource;
import com.luma.budget.domain.PlannedItem;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.model.Money;
import com.luma.savings.domain.SavingsGoal;
import com.luma.savings.infrastructure.SavingsGoalRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Que hacer cuando el ciclo no cierra.
 *
 * <p>Vive aparte de {@link BudgetCycleService} porque responde otra pregunta.
 * Aquel dice COMO VA el presupuesto; este dice QUE SE PUEDE HACER, y para eso
 * necesita datos que al balance no le hacen falta: la flexibilidad de cada gasto
 * y la prioridad de cada meta.
 *
 * <p>Su trabajo es armar el snapshot y pasarlo a {@link DeficitAdvisor}, que es
 * donde vive la decision y donde se puede probar sin base de datos.
 */
@Service
public class DeficitAdviceService {

    private final CycleItemRepository items;
    private final SavingsGoalRepository goals;

    public DeficitAdviceService(CycleItemRepository items, SavingsGoalRepository goals) {
        this.items = items;
        this.goals = goals;
    }

    /**
     * El consejo para un ciclo.
     *
     * <p>Los renglones se leen UNA vez y sirven para las dos cosas: calcular el
     * balance y armar los candidatos. Pedirle el balance a otro servicio
     * significaria leerlos dos veces para responder una sola pregunta.
     */
    @Transactional(readOnly = true)
    public DeficitAdvisor.Advice adviceFor(BudgetCycle cycle, String currency) {
        List<CycleItem> renglones =
                items.findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(cycle.getId());

        List<PlannedItem> planeados = new ArrayList<>(renglones.size());
        for (CycleItem renglon : renglones) {
            planeados.add(renglon.toPlannedItem(currency));
        }

        Money balance = BudgetCalculator.calculate(planeados, currency).planned().balance();

        return DeficitAdvisor.adviseFor(balance, candidatosDe(renglones, currency));
    }

    private List<CutCandidate> candidatosDe(List<CycleItem> renglones, String currency) {
        Map<Long, Integer> prioridades = prioridadesDeLasMetas(renglones);
        List<CutCandidate> candidatos = new ArrayList<>();

        for (CycleItem renglon : renglones) {
            // Los ingresos no se recortan: recortar lo que entra empeora el
            // deficit, no lo resuelve.
            if (renglon.getItemType() == CycleItemType.INCOME) {
                continue;
            }

            candidatos.add(new CutCandidate(
                    renglon.getPublicId(),
                    renglon.getName(),
                    renglon.getItemType(),
                    renglon.getStatus(),
                    renglon.getFlexibility(),
                    renglon.getSourceId() != null ? prioridades.get(renglon.getSourceId()) : null,
                    Money.of(renglon.getPlannedAmount(), currency)));
        }

        return candidatos;
    }

    /**
     * La prioridad de cada meta que aparece en el ciclo.
     *
     * <p>Se piden todas de una consulta y no una por renglon: un ciclo tiene
     * pocas metas, pero una consulta por renglon dentro de un bucle es como
     * empiezan los problemas de rendimiento que nadie ve venir.
     */
    private Map<Long, Integer> prioridadesDeLasMetas(List<CycleItem> renglones) {
        Set<Long> ids = new HashSet<>();

        for (CycleItem renglon : renglones) {
            if (renglon.getSourceType() == ItemSource.SAVINGS_GOAL && renglon.getSourceId() != null) {
                ids.add(renglon.getSourceId());
            }
        }

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<Long, Integer> prioridades = new HashMap<>();
        for (SavingsGoal goal : goals.findAllById(ids)) {
            prioridades.put(goal.getId(), goal.getPriority());
        }

        return prioridades;
    }
}
