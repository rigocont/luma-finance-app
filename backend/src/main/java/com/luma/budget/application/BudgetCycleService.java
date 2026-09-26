package com.luma.budget.application;

import com.luma.budget.domain.BudgetCalculator;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.BudgetPeriod;
import com.luma.budget.domain.BudgetResult;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.CycleDeficit;
import com.luma.budget.domain.CyclePlanner;
import com.luma.budget.domain.CycleStatus;
import com.luma.budget.domain.DueSoonItem;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.domain.PlannedItem;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.expenses.infrastructure.ExpenseRepository;
import com.luma.income.infrastructure.IncomeRepository;
import com.luma.savings.application.SavingsGoalService;
import com.luma.savings.infrastructure.SavingsGoalRepository;
import com.luma.users.application.UserPreferencesService;
import com.luma.users.domain.UserPreferences;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final SavingsGoalService savings;
    private final Clock clock;

    public BudgetCycleService(
            BudgetCycleRepository cycles,
            CycleItemRepository items,
            IncomeRepository incomes,
            ExpenseRepository expenses,
            SavingsGoalRepository goals,
            UserPreferencesService preferences,
            CycleMaterializer materializer,
            SavingsGoalService savings,
            Clock clock) {
        this.cycles = cycles;
        this.items = items;
        this.incomes = incomes;
        this.expenses = expenses;
        this.goals = goals;
        this.preferences = preferences;
        this.materializer = materializer;
        this.savings = savings;
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

    /**
     * Un ciclo con su resultado y como cambio respecto al anterior.
     *
     * <p>La diferencia se calcula AQUI y no en el cliente. Es una cifra de
     * dinero, y en LUMA ninguna se deriva en la interfaz: si la restara el
     * navegador habria dos formas de redondear el mismo numero.
     *
     * @param outflowChange como cambio lo que sale respecto al ciclo anterior.
     *     Nulo en el mas antiguo de la lista, que no tiene con que compararse.
     */
    public record CycleTrend(BudgetCycle cycle, BudgetResult result, Change outflowChange) {}

    /**
     * Un cambio entre dos ciclos.
     *
     * @param direction UP, DOWN o SAME. La interfaz elige la frase; no compara.
     * @param amount la MAGNITUD, siempre positiva. El signo ya lo dice la
     *     direccion, y mandar un negativo obligaria al cliente a sacarle el
     *     valor absoluto, que es aritmetica de dinero en el lugar equivocado.
     */
    public record Change(String direction, Money amount) {

        static Change between(Money current, Money previous) {
            Money diferencia = current.subtract(previous);

            if (diferencia.isZero()) {
                return new Change("SAME", Money.zero(current.currency()));
            }
            return diferencia.isPositive()
                    ? new Change("UP", diferencia)
                    : new Change("DOWN", diferencia.negate());
        }
    }

    /**
     * Los ultimos ciclos con su balance, del MAS ANTIGUO al mas reciente.
     *
     * <p>El orden es al contrario del historial a proposito. El historial se lee
     * como una lista —lo ultimo primero—, pero una comparacion se lee como una
     * linea de tiempo, de izquierda a derecha. Invertirlo en el cliente seria
     * pedirle que sepa para que va a usar el dato.
     *
     * <p>Los renglones de todos los ciclos se traen en UNA consulta y se agrupan
     * en memoria. Un balance por ciclo dentro de un bucle serian seis consultas
     * para responder una sola pregunta.
     */
    @Transactional(readOnly = true)
    public List<CycleTrend> trends(Long userId, int howMany, String currency) {
        List<BudgetCycle> ultimos = cycles
                .findByUserIdOrderByStartDateDesc(userId, PageRequest.of(0, Math.max(howMany, 1)))
                .getContent();

        if (ultimos.isEmpty()) {
            return List.of();
        }

        Map<Long, List<CycleItem>> porCiclo =
                items.findByBudgetCycleIdIn(ultimos.stream().map(BudgetCycle::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(CycleItem::getBudgetCycleId));

        List<CycleTrend> tendencia = new ArrayList<>(ultimos.size());
        BudgetResult anterior = null;

        for (BudgetCycle cycle : ultimos.reversed()) {
            List<PlannedItem> planeados =
                    porCiclo.getOrDefault(cycle.getId(), List.of()).stream()
                            .map(item -> item.toPlannedItem(currency))
                            .toList();

            BudgetResult resultado = BudgetCalculator.calculate(planeados, currency);

            tendencia.add(new CycleTrend(
                    cycle,
                    resultado,
                    anterior == null
                            ? null
                            : Change.between(
                                    resultado.planned().totalOutflow(),
                                    anterior.planned().totalOutflow())));

            anterior = resultado;
        }

        return tendencia;
    }

    /**
     * Los renglones activos que vencen exactamente esa fecha, de todos los
     * usuarios.
     *
     * <p>Lectura publica para {@code PaymentAlertsJob}, del modulo de
     * notificaciones: es la puerta correcta segun {@code docs/architecture.md}
     * para que un modulo pregunte algo de otro, en lugar de que le abra sus
     * repositorios.
     */
    @Transactional(readOnly = true)
    public List<DueSoonItem> itemsDueOn(LocalDate date) {
        return items.findDueOn(
                date, CycleStatus.ACTIVE, List.of(ItemStatus.PENDING, ItemStatus.NEEDS_REVIEW));
    }

    /**
     * Los ciclos activos, de cualquier usuario, cuyo balance PRESUPUESTADO es
     * negativo.
     *
     * <p>Misma tecnica que {@link #trends}: los renglones de todos los ciclos
     * activos se traen en una sola consulta y se agrupan en memoria, en lugar
     * de calcular un ciclo a la vez. La moneda usada para el calculo es
     * irrelevante aqui —solo se necesita la MAGNITUD del faltante, nunca sale
     * de este metodo un {@code Money}— asi que se usa una constante en vez de
     * pedir la preferencia de cada usuario.
     */
    @Transactional(readOnly = true)
    public List<CycleDeficit> activeCyclesInDeficit() {
        List<BudgetCycle> activos = cycles.findByStatus(CycleStatus.ACTIVE);

        if (activos.isEmpty()) {
            return List.of();
        }

        Map<Long, List<CycleItem>> porCiclo =
                items.findByBudgetCycleIdIn(activos.stream().map(BudgetCycle::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(CycleItem::getBudgetCycleId));

        List<CycleDeficit> deficits = new ArrayList<>();

        for (BudgetCycle cycle : activos) {
            List<PlannedItem> planeados =
                    porCiclo.getOrDefault(cycle.getId(), List.of()).stream()
                            .map(item -> item.toPlannedItem(Money.DEFAULT_CURRENCY))
                            .toList();

            BudgetResult resultado = BudgetCalculator.calculate(planeados, Money.DEFAULT_CURRENCY);

            if (resultado.planned().balance().isNegative()) {
                deficits.add(new CycleDeficit(
                        cycle.getUserId(),
                        cycle.getPublicId(),
                        resultado.planned().balance().negate().amount()));
            }
        }

        return deficits;
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

    /**
     * Confirma un renglon.
     *
     * @param registerInGoal solo aplica a los renglones de ahorro. Cuando es
     *     cierto, el monto confirmado se registra tambien como aporte a la meta
     *     y sube su progreso. La interfaz lo propone marcado y deja desmarcarlo,
     *     porque apartar el dinero y registrarlo en la meta son la misma accion
     *     casi siempre, pero no siempre.
     */
    @Transactional
    public CycleItem settleItem(
            BudgetCycle cycle,
            String itemPublicId,
            Money actualAmount,
            LocalDate settledOn,
            boolean registerInGoal) {

        CycleItem item = requireMutableItem(cycle, itemPublicId);
        // Sin fecha explicita, ocurrio hoy: es el caso normal — se registra el
        // pago al hacerlo.
        LocalDate occurredOn = settledOn != null ? settledOn : LocalDate.now(clock);
        item.settle(actualAmount, occurredOn, clock.instant());
        CycleItem saved = items.save(item);

        if (registerInGoal
                && item.getItemType() == CycleItemType.SAVING
                && item.getSourceId() != null) {
            savings.registerFromCycle(
                    cycle.getUserId(), item.getSourceId(), saved.getId(), actualAmount, occurredOn);
        }

        return saved;
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
