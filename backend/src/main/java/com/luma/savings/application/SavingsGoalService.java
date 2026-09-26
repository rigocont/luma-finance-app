package com.luma.savings.application;

import com.luma.common.error.BusinessRuleException;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.savings.domain.ContributionMode;
import com.luma.savings.domain.ContributionType;
import com.luma.savings.domain.GoalStatus;
import com.luma.savings.domain.SavingsContribution;
import com.luma.savings.domain.SavingsGoal;
import com.luma.savings.domain.SavingsGoalCreatedEvent;
import com.luma.savings.infrastructure.SavingsContributionRepository;
import com.luma.savings.infrastructure.SavingsGoalRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de las metas de ahorro y sus movimientos. */
@Service
public class SavingsGoalService {

    private static final Logger log = LoggerFactory.getLogger(SavingsGoalService.class);

    private final SavingsGoalRepository goals;
    private final SavingsContributionRepository contributions;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public SavingsGoalService(
            SavingsGoalRepository goals,
            SavingsContributionRepository contributions,
            ApplicationEventPublisher events,
            Clock clock) {
        this.goals = goals;
        this.contributions = contributions;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public SavingsGoal create(
            Long userId,
            String name,
            Money target,
            LocalDate targetDate,
            ContributionMode mode,
            Money plannedPerCycle,
            String icon,
            String color) {

        // La meta nueva va al final: reordenar es una decision explicita, no
        // algo que pase solo por capturar una meta mas.
        int priority = goals.maxPriority(userId) + 1;

        SavingsGoal goal = SavingsGoal.create(
                userId, name, target, targetDate, mode, plannedPerCycle, priority);
        goal.changeLook(icon, color);

        SavingsGoal saved = goals.save(goal);
        log.info("Meta {} creada para el usuario {}", saved.getPublicId(), userId);

        events.publishEvent(new SavingsGoalCreatedEvent(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoal> search(Long userId, GoalStatus status) {
        return goals.search(userId, status);
    }

    @Transactional(readOnly = true)
    public SavingsGoal require(Long userId, String publicId) {
        return find(userId, publicId);
    }

    @Transactional(readOnly = true)
    public List<SavingsContribution> movements(Long userId, String publicId) {
        SavingsGoal goal = find(userId, publicId);
        return contributions.findBySavingsGoalIdOrderByContributionDateDescIdDesc(goal.getId());
    }

    @Transactional
    public SavingsGoal update(
            Long userId,
            String publicId,
            String name,
            Money target,
            boolean targetDateChanged,
            LocalDate targetDate,
            ContributionMode mode,
            Money plannedPerCycle,
            String icon,
            String color) {

        SavingsGoal goal = find(userId, publicId);

        if (name != null) {
            goal.rename(name);
        }
        if (target != null) {
            goal.changeTarget(target);
        }
        // La fecha se aplica ANTES del plan: cambiar a "calculado por fecha"
        // valida que exista una, y tiene que ver la nueva, no la vieja.
        if (targetDateChanged) {
            goal.changeTargetDate(targetDate);
        }
        if (mode != null) {
            goal.changePlan(mode, plannedPerCycle);
        }
        if (icon != null || color != null) {
            goal.changeLook(icon, color);
        }

        return goals.save(goal);
    }

    /**
     * Registra una aportacion o un retiro.
     *
     * @param cycleItemId el renglon del ciclo que lo origina, o nulo si es un
     *     movimiento suelto. Si ese renglon ya genero un movimiento, se rechaza:
     *     confirmar dos veces no puede sumar dos veces.
     */
    @Transactional
    public SavingsGoal registerMovement(
            Long userId,
            String publicId,
            Money amount,
            LocalDate date,
            ContributionType type,
            Long cycleItemId,
            String notes) {

        if (!amount.isPositive()) {
            throw new BusinessRuleException("El monto del movimiento tiene que ser mayor que cero.");
        }

        SavingsGoal goal = find(userId, publicId);

        if (cycleItemId != null && contributions.findByCycleItemId(cycleItemId).isPresent()) {
            throw new BusinessRuleException(
                    "Ese renglon del ciclo ya se registro en la meta.");
        }

        LocalDate cuando = date != null ? date : LocalDate.now(clock);

        contributions.save(
                SavingsContribution.of(goal.getId(), cycleItemId, amount, cuando, type, notes));

        // El retiro baja el progreso; la aportacion lo sube.
        goal.applyContribution(
                type == ContributionType.WITHDRAWAL ? amount.negate() : amount);

        return goals.save(goal);
    }

    /**
     * Registra el aporte que nacio de confirmar el renglon de ahorro de un ciclo.
     *
     * <p>Entra por el id INTERNO de la meta porque quien llama es el renglon del
     * ciclo, que guarda ese id y no el publico. Se verifica el usuario de todos
     * modos: un servicio que confia en quien lo llama deja de ser una barrera.
     *
     * <p>Si el renglon ya se registro, no hace nada en lugar de fallar. Es
     * idempotente a proposito: quien confirma un renglon quiere que quede
     * confirmado, no enterarse de que ya lo estaba.
     */
    @Transactional
    public void registerFromCycle(
            Long userId, Long goalId, Long cycleItemId, Money amount, LocalDate date) {

        if (!amount.isPositive()) {
            return;
        }
        if (contributions.findByCycleItemId(cycleItemId).isPresent()) {
            return;
        }

        SavingsGoal goal = goals.findById(goalId)
                .filter(g -> g.getUserId().equals(userId) && !g.isDeleted())
                .orElse(null);

        if (goal == null) {
            // La meta se borro despues de materializar el ciclo. El renglon
            // sigue siendo valido para el presupuesto; solo no hay meta a la
            // cual sumarle.
            log.info("Renglon de ahorro {} sin meta viva: no se registra aporte", cycleItemId);
            return;
        }

        contributions.save(SavingsContribution.of(
                goal.getId(), cycleItemId, amount, date, ContributionType.PLANNED, null));

        goal.applyContribution(amount);
        goals.save(goal);
    }

    @Transactional
    public SavingsGoal setPaused(Long userId, String publicId, boolean paused) {
        SavingsGoal goal = find(userId, publicId);

        if (paused) {
            goal.pause();
        } else {
            goal.resume();
        }

        return goals.save(goal);
    }

    /**
     * Reordena las metas segun la lista que llega.
     *
     * <p>Se recibe el orden COMPLETO y no "mueve esta al lugar N": con una lista
     * entera el resultado no depende de en que estado creia el cliente que
     * estaban las metas, y dos pestanas abiertas no pueden dejarlas intercaladas.
     */
    @Transactional
    public List<SavingsGoal> reorder(Long userId, List<String> publicIdsInOrder) {
        List<SavingsGoal> actuales = goals.findByUserIdAndDeletedAtIsNullOrderByPriorityAsc(userId);

        // Se comprueban las DOS cosas. Solo con el tamano, una lista con un id
        // repetido pasaria: el bucle le asignaria dos prioridades a la misma
        // meta y dejaria a otra con la suya vieja, duplicada.
        long distintos = publicIdsInOrder.stream().distinct().count();

        if (publicIdsInOrder.size() != actuales.size() || distintos != actuales.size()) {
            throw new BusinessRuleException(
                    "El nuevo orden tiene que incluir todas tus metas, una sola vez.");
        }

        int priority = 1;
        for (String publicId : publicIdsInOrder) {
            SavingsGoal goal = actuales.stream()
                    .filter(g -> g.getPublicId().equals(publicId))
                    .findFirst()
                    .orElseThrow(() -> ResourceNotFoundException.of("Meta", publicId));

            goal.changePriority(priority++);
        }

        return goals.saveAll(actuales).stream()
                .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                .toList();
    }

    @Transactional
    public void delete(Long userId, String publicId) {
        SavingsGoal goal = find(userId, publicId);
        goal.softDelete(clock.instant());
        goals.save(goal);

        log.info("Meta {} eliminada (borrado logico) por el usuario {}", publicId, userId);
    }

    private SavingsGoal find(Long userId, String publicId) {
        return goals.findByPublicIdAndUserIdAndDeletedAtIsNull(publicId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Meta", publicId));
    }

    public static Money parseAmount(String raw, String currency) {
        return raw != null ? Money.of(new BigDecimal(raw), currency) : null;
    }
}
