package com.luma.budget.application;

import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleStatus;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.budget.infrastructure.CycleItemRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Clock clock;

    public OverdueItemsJob(
            BudgetCycleRepository cycles, CycleItemRepository items, Clock clock) {
        this.cycles = cycles;
        this.items = items;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 4 * * *")
    @Transactional
    public void markOverdueItems() {
        LocalDate today = LocalDate.now(clock);

        List<Long> activeCycleIds = cycles.findByStatus(CycleStatus.ACTIVE).stream()
                .map(BudgetCycle::getId)
                .toList();

        if (activeCycleIds.isEmpty()) {
            return;
        }

        List<CycleItem> pending = items.findByBudgetCycleIdIn(activeCycleIds).stream()
                .filter(item -> item.becameOverdue(today))
                .toList();

        pending.forEach(CycleItem::markOverdue);
        items.saveAll(pending);

        if (!pending.isEmpty()) {
            log.info("Renglones marcados como vencidos: {}", pending.size());
        }
    }
}
