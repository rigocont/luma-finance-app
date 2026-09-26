package com.luma.budget.application;

import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemOverdueEvent;
import com.luma.budget.domain.CycleStatus;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.budget.infrastructure.CycleItemRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marca como vencidos los renglones cuya fecha ya paso.
 *
 * <p>Sin esto, "pagos vencidos" no existiria: un renglon se quedaria pendiente
 * para siempre y el dashboard no tendria nada que destacar.
 *
 * <p>Corre una vez al dia, de madrugada. El estado vencido no cambia el importe
 * ni saca el renglon del presupuesto: lo que se debe se sigue debiendo.
 */
@Component
public class OverdueItemsJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueItemsJob.class);

    private final BudgetCycleRepository cycles;
    private final CycleItemRepository items;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OverdueItemsJob(
            BudgetCycleRepository cycles,
            CycleItemRepository items,
            ApplicationEventPublisher events,
            Clock clock) {
        this.cycles = cycles;
        this.items = items;
        this.events = events;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 4 * * *")
    @Transactional
    public void markOverdueItems() {
        LocalDate today = LocalDate.now(clock);

        List<BudgetCycle> activeCycles = cycles.findByStatus(CycleStatus.ACTIVE);

        if (activeCycles.isEmpty()) {
            return;
        }

        Map<Long, Long> userIdPorCiclo =
                activeCycles.stream().collect(Collectors.toMap(BudgetCycle::getId, BudgetCycle::getUserId));

        List<CycleItem> pending = items.findByBudgetCycleIdIn(userIdPorCiclo.keySet().stream().toList())
                .stream()
                .filter(item -> item.becameOverdue(today))
                .toList();

        pending.forEach(CycleItem::markOverdue);
        items.saveAll(pending);

        // Un evento por renglon, y solo por los que TRANSICIONAN hoy: es lo que
        // le permite a notifications avisar una vez, no una vez por dia
        // mientras el pago siga vencido.
        for (CycleItem item : pending) {
            events.publishEvent(new CycleItemOverdueEvent(
                    userIdPorCiclo.get(item.getBudgetCycleId()),
                    item.getPublicId(),
                    item.getName(),
                    item.getPlannedAmount(),
                    item.getDueDate()));
        }

        if (!pending.isEmpty()) {
            log.info("Renglones marcados como vencidos: {}", pending.size());
        }
    }
}
