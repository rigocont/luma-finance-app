package com.luma.budget.application;

import com.luma.budget.domain.BudgetCalculator;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.BudgetPeriod;
import com.luma.budget.domain.BudgetResult;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.CyclePlanner;
import com.luma.budget.domain.CycleStatus;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.domain.PlannedItem;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.expenses.infrastructure.ExpenseRepository;
import com.luma.income.infrastructure.IncomeRepository;
import com.luma.savings.infrastructure.SavingsGoalRepository;
import com.luma.users.application.UserPreferencesService;
import com.luma.users.domain.UserPreferences;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de los ciclos presupuestales. */
@Service
public class BudgetCycleService {

    private static final Logger log = LoggerFactory.getLogger(BudgetCycleService.class);

    private final BudgetCycleRepository cycles;
    private final CycleItemRepository items;
    private final IncomeRepository incomes;
    private final ExpenseRepository expenses;
    private final SavingsGoalRepository goals;
    private final UserPreferencesService preferences;
    private final CycleMaterializer materializer;
    private final Clock clock;

    public BudgetCycleService(
            BudgetCycleRepository cycles,
            CycleItemRepository items,
            IncomeRepository incomes,
            ExpenseRepository expenses,
            SavingsGoalRepository goals,
            UserPreferencesService preferences,
            CycleMaterializer materializer,
            Clock clock) {
        this.cycles = cycles;
        this.items = items;
        this.incomes = incomes;
        this.expenses = expenses;
        this.goals = goals;
        this.preferences = preferences;
        this.materializer = materializer;
        this.clock = clock;
    }

    /**
     * Abre el siguiente ciclo y materializa sus renglones.
     *
     * <p>Si hay un ciclo activo cuyo periodo ya termino, se cierra solo: dejarlo
     * abierto obligaria a la persona a cerrarlo antes de seguir, sin ganar nada.
     * Si el periodo del ciclo activo NO ha terminado, se rechaza: abrir el
     * siguiente seria saltarse el actual.
     */
    @Transactional
    public BudgetCycle openNextCycle(Long userId) {
        UserPreferences prefs = preferences.getOrCreate(userId);
        CyclePlanner planner =
                CyclePlanner.of(prefs.getBudgetCycleType(), prefs.getCycleAnchorDay());
        LocalDate today = LocalDate.now(clock);

        Optional<BudgetCycle> active =
                cycles.findFirstByUserIdAndStatusOrderByStartDateDesc(userId, CycleStatus.ACTIVE);

        BudgetPeriod period;
        if (active.isPresent()) {
            BudgetCycle current = active.get();

            if (!current.hasEnded(today)) {
                throw new BusinessRuleException(
                        "Ya tienes un ciclo en curso. Cierralo antes de abrir el siguiente.");
            }

            current.close();
            cycles.save(current);
            log.info("Ciclo {} cerrado automaticamente: su periodo ya habia terminado",
                    current.getPublicId());

            period = planner.next(current.period());
        } else {
            period = planner.periodContaining(today);
        }

        if (cycles.existsByUserIdAndStartDate(userId, period.start())) {
            throw new BusinessRuleException("Ya existe un ciclo que empieza el " + period.start());
        }

        int sequence = cycles.findFirstByUserIdOrderBySequenceNumberDesc(userId)
                .map(last -> last.getSequenceNumber() + 1)
                .orElse(1);

        BudgetCycle cycle = cycles.save(
                BudgetCycle.open(userId, prefs.getBudgetCycleType(), period, sequence));

        List<CycleItem> materialized = materializer.materialize(
                cycle,
                prefs.getCurrency(),
                prefs.getExpenseAllocationPolicy(),
                planner,
                incomes.findByUserIdAndActiveTrueAndDeletedAtIsNull(userId),
                expenses.findByUserIdAndActiveTrueAndDeletedAtIsNull(userId),
                goals.findByUserIdAndDeletedAtIsNullOrderByPriorityAsc(userId));

        items.saveAll(materialized);

        log.info(
                "Ciclo {} abierto para el usuario {} con {} renglones ({})",
                cycle.getPublicId(),
                userId,
                materialized.size(),
                period);

        return cycle;
    }

    @Transactional(readOnly = true)
    public Optional<BudgetCycle> currentCycle(Long userId) {
        return cycles.findFirstByUserIdAndStatusOrderByStartDateDesc(userId, CycleStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public BudgetCycle requireCycle(String publicId, Long userId) {
        return cycles.findByPublicIdAndUserId(publicId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Ciclo", publicId));
    }

    @Transactional(readOnly = true)
    public Page<BudgetCycle> history(Long userId, Pageable pageable) {
        return cycles.findByUserIdOrderByStartDateDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<CycleItem> itemsOf(BudgetCycle cycle, CycleItemType type, ItemStatus status) {
        if (type != null) {
            return items.findByBudgetCycleIdAndItemTypeOrderByDueDateAscIdAsc(cycle.getId(), type);
        }
        if (status != null) {
            return items.findByBudgetCycleIdAndStatusOrderByDueDateAscIdAsc(cycle.getId(), status);
        }
        return items.findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(cycle.getId());
    }

    /** El resultado del motor presupuestal para un ciclo. */
    @Transactional(readOnly = true)
    public BudgetResult balanceOf(BudgetCycle cycle, String currency) {
        List<PlannedItem> planned =
                items.findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(cycle.getId())
                        .stream()
                        .map(item -> item.toPlannedItem(currency))
                        .toList();

        return BudgetCalculator.calculate(planned, currency);
    }

    /**
     * La moneda del usuario. Es una LECTURA y no crea las preferencias.
     *
     * <p>Delega en el modulo de usuarios, que es donde vive la preferencia. Se
     * conserva aqui porque el controlador de ciclos ya la usaba y porque leer la
     * moneda es parte de armar cualquier respuesta con importes.
     *
     * <p>Antes llamaba a {@code getOrCreate}, que escribe: desde esta
     * transaccion de solo lectura el insert fallaba con "Connection is
     * read-only" y tumbaba todos los endpoints del modulo, porque todos empiezan
     * pidiendo la moneda.
     */
    @Transactional(readOnly = true)
    public String currencyOf(Long userId) {
        return preferences.currencyOf(userId);
    }

    @Transactional
    public CycleItem settleItem(
            BudgetCycle cycle, String itemPublicId, Money actualAmount, LocalDate settledOn) {
        CycleItem item = requireMutableItem(cycle, itemPublicId);
        // Sin fecha explicita, ocurrio hoy: es el caso normal — se registra el
        // pago al hacerlo.
        LocalDate occurredOn = settledOn != null ? settledOn : LocalDate.now(clock);
        item.settle(actualAmount, occurredOn, clock.instant());
        return items.save(item);
    }

    @Transactional
    public CycleItem skipItem(BudgetCycle cycle, String itemPublicId) {
        CycleItem item = requireMutableItem(cycle, itemPublicId);
        item.skip();
        return items.save(item);
    }

    @Transactional
    public CycleItem reopenItem(BudgetCycle cycle, String itemPublicId) {
        CycleItem item = requireMutableItem(cycle, itemPublicId);
        item.reopen();
        return items.save(item);
    }

    @Transactional
    public CycleItem updateItem(
            BudgetCycle cycle,
            String itemPublicId,
            Money plannedAmount,
            Integer displayOrder,
            String notes) {

        CycleItem item = requireMutableItem(cycle, itemPublicId);

        if (plannedAmount != null) {
            item.changePlannedAmount(plannedAmount);
        }
        if (displayOrder != null) {
            item.changeDisplayOrder(displayOrder);
        }
        if (notes != null) {
            item.changeNotes(notes);
        }

        return items.save(item);
    }

    @Transactional
    public BudgetCycle closeCycle(BudgetCycle cycle) {
        if (cycle.getStatus() == CycleStatus.CLOSED) {
            throw new BusinessRuleException("Este ciclo ya esta cerrado.");
        }
        cycle.close();
        log.info("Ciclo {} cerrado", cycle.getPublicId());
        return cycles.save(cycle);
    }

    /**
     * Un renglon de un ciclo cerrado no se toca.
     *
     * <p>Hallazgo 1.2 del analisis, aplicado en el unico lugar donde puede
     * aplicarse de verdad: la escritura.
     */
    private CycleItem requireMutableItem(BudgetCycle cycle, String itemPublicId) {
        if (!cycle.isMutable()) {
            throw new BusinessRuleException(
                    "Este ciclo esta cerrado y no se puede modificar.");
        }
        return items.findByPublicIdAndBudgetCycleId(itemPublicId, cycle.getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Renglon del ciclo", itemPublicId));
    }
}
