package com.luma.insights.application;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.application.BudgetCycleService.CycleCategoryTotals;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.BudgetResult;
import com.luma.common.model.Money;
import com.luma.expenses.application.ExpenseService;
import com.luma.expenses.domain.ExpenseCategory;
import com.luma.insights.domain.CategoryGrowth;
import com.luma.insights.domain.CategoryGrowthDetector;
import com.luma.insights.domain.DeficitCause;
import com.luma.insights.domain.DeficitCauseFinder;
import com.luma.insights.domain.FinancialInsights;
import com.luma.insights.domain.GoalShare;
import com.luma.insights.domain.SurplusAllocation;
import com.luma.insights.domain.SurplusAllocator;
import com.luma.savings.application.SavingsGoalService;
import com.luma.savings.domain.GoalStatus;
import com.luma.savings.domain.SavingsGoal;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El analisis financiero sin IA: reglas fijas y verificables sobre cifras que
 * ya calcularon otros modulos.
 *
 * <p>Esta es la version determinista de la Fase 13. Segun lo decidido, la capa
 * de redaccion/priorizacion con LLM (ver docs/00-arquitectura-fase-0.md, S8.4)
 * queda pendiente para una fase futura; este servicio ya entrega las tres
 * senales utiles sin depender de ella:
 *
 * <ul>
 *   <li>Por que hay un deficit este ciclo (que categoria subio mas).
 *   <li>Como repartir un remanente entre las metas activas.
 *   <li>Que categorias llevan una racha de 3 ciclos seguidos al alza.
 * </ul>
 *
 * <p>Ninguna cifra aqui se inventa: si no hay ciclo anterior con que comparar,
 * o no hay metas activas, o no hay 3 ciclos de historia, la senal
 * correspondiente simplemente no aparece.
 */
@Service
public class InsightsService {

    /** Ciclos que hacen falta para hablar de una racha (3 ciclos seguidos al alza). */
    private static final int GROWTH_WINDOW = 3;

    /** Ciclos que hacen falta para explicar un deficit: el actual y el anterior. */
    private static final int DEFICIT_WINDOW = 2;

    private final BudgetCycleService cycles;
    private final ExpenseService expenses;
    private final SavingsGoalService goals;

    public InsightsService(BudgetCycleService cycles, ExpenseService expenses, SavingsGoalService goals) {
        this.cycles = cycles;
        this.expenses = expenses;
        this.goals = goals;
    }

    @Transactional(readOnly = true)
    public FinancialInsights currentInsights(Long userId) {
        String currency = cycles.currencyOf(userId);
        Map<Long, String> nombres = nombresDeCategorias(userId);

        DeficitCause causaDeficit = null;
        SurplusAllocation reparto = null;

        Optional<BudgetCycle> cicloActual = cycles.currentCycle(userId);
        if (cicloActual.isPresent()) {
            BudgetCycle cycle = cicloActual.get();
            BudgetResult resultado = cycles.balanceOf(cycle, currency);
            Money balance = resultado.planned().balance();

            if (balance.isNegative()) {
                causaDeficit = causaDelDeficit(userId, cycle, balance, currency, nombres);
            } else if (balance.isPositive()) {
                reparto = repartoDelRemanente(userId, cycle, balance, currency);
            }
        }

        List<CategoryGrowth> crecimientos = crecimientoSostenido(userId, currency, nombres);

        return new FinancialInsights(causaDeficit, reparto, crecimientos);
    }

    /**
     * Nombre de cada categoria de gasto del usuario, para no ensenar un id
     * donde deberia ir un nombre.
     */
    private Map<Long, String> nombresDeCategorias(Long userId) {
        Map<Long, String> nombres = new HashMap<>();
        for (ExpenseCategory categoria : expenses.catalog(userId)) {
            nombres.put(categoria.getId(), categoria.getName());
        }
        return nombres;
    }

    /**
     * Que categoria subio mas respecto al ciclo anterior, si hay con que
     * comparar. Sin ciclo anterior no hay causa que afirmar: mejor no decir
     * nada que inventar una.
     */
    private DeficitCause causaDelDeficit(
            Long userId, BudgetCycle cycle, Money balance, String currency, Map<Long, String> nombres) {
        List<CycleCategoryTotals> ultimos = cycles.categoryTotalsOfRecentCycles(userId, DEFICIT_WINDOW);
        if (ultimos.size() < DEFICIT_WINDOW) {
            return null;
        }

        CycleCategoryTotals anterior = ultimos.get(ultimos.size() - 2);
        CycleCategoryTotals actual = ultimos.get(ultimos.size() - 1);

        if (!actual.cyclePublicId().equals(cycle.getPublicId())) {
            return null;
        }

        Money faltante = balance.negate();

        return DeficitCauseFinder.biggestIncrease(anterior.byCategoryId(), actual.byCategoryId(), currency)
                .map(delta -> new DeficitCause(
                        cycle.getPublicId(),
                        faltante,
                        nombres.getOrDefault(delta.categoryId(), "Categoria eliminada"),
                        delta.previousAmount(),
                        delta.currentAmount()))
                .orElse(null);
    }

    /**
     * Como repartir el remanente del ciclo entre las metas activas, en
     * cascada por prioridad. Sin metas activas el remanente sigue siendo
     * real; solo no hay a donde proponer que vaya.
     */
    private SurplusAllocation repartoDelRemanente(Long userId, BudgetCycle cycle, Money balance, String currency) {
        List<SavingsGoal> metasActivas = goals.search(userId, GoalStatus.ACTIVE);

        List<SurplusAllocator.GoalCandidate> candidatos = new ArrayList<>(metasActivas.size());
        for (SavingsGoal meta : metasActivas) {
            candidatos.add(new SurplusAllocator.GoalCandidate(
                    meta.getPublicId(), meta.getName(), meta.getPriority(), meta.remaining(currency)));
        }

        SurplusAllocator.Advice consejo = SurplusAllocator.allocate(balance, candidatos);

        List<GoalShare> repartos = consejo.allocations().stream()
                .map(allocation -> new GoalShare(allocation.goalId(), allocation.name(), allocation.amount()))
                .toList();

        return new SurplusAllocation(cycle.getPublicId(), consejo.surplus(), repartos);
    }

    /**
     * Categorias con 3 ciclos seguidos al alza. Sin 3 ciclos de historia no
     * hay racha que confirmar, asi que no se afirma nada.
     */
    private List<CategoryGrowth> crecimientoSostenido(Long userId, String currency, Map<Long, String> nombres) {
        List<CycleCategoryTotals> ultimos = cycles.categoryTotalsOfRecentCycles(userId, GROWTH_WINDOW);
        if (ultimos.size() < GROWTH_WINDOW) {
            return List.of();
        }

        Set<Long> categorias = new HashSet<>();
        for (CycleCategoryTotals total : ultimos) {
            categorias.addAll(total.byCategoryId().keySet());
        }

        List<CategoryGrowth> crecimientos = new ArrayList<>();
        for (Long categoriaId : categorias) {
            List<Money> montos = ultimos.stream()
                    .map(total -> Money.of(
                            total.byCategoryId().getOrDefault(categoriaId, BigDecimal.ZERO), currency))
                    .toList();

            if (CategoryGrowthDetector.isSustainedGrowth(montos)) {
                crecimientos.add(new CategoryGrowth(
                        nombres.getOrDefault(categoriaId, "Categoria eliminada"),
                        montos.get(0),
                        montos.get(montos.size() - 1),
                        montos.size()));
            }
        }

        return crecimientos.stream().sorted(Comparator.comparing(CategoryGrowth::categoryName)).toList();
    }
}
