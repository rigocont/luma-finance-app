package com.luma.notifications.application;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.domain.CycleDeficit;
import com.luma.budget.domain.DueSoonItem;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Las dos alertas que no nacen de una accion puntual, sino de revisar el
 * estado de todos los ciclos activos: pagos que se acercan y ciclos que ya no
 * alcanzan.
 *
 * <p>La de pagos vencidos NO esta aqui: esa es una transicion de estado que
 * {@link com.luma.budget.application.OverdueItemsJob} ya detecta al marcar
 * {@code OVERDUE}, y se avisa por evento en cuanto pasa, no en un segundo
 * barrido separado.
 *
 * <p>Corre unos minutos despues de {@code OverdueItemsJob} para que un renglon
 * que hoy cruza a vencido no se cuente tambien como "proximo" en la misma
 * pasada. Llama a {@link BudgetCycleService}, la capa de aplicacion publica de
 * presupuesto, en lugar de tocar sus repositorios: es exactamente la regla que
 * describe {@code docs/architecture.md} para la comunicacion entre modulos.
 */
@Component
public class PaymentAlertsJob {

    private static final Logger log = LoggerFactory.getLogger(PaymentAlertsJob.class);

    /**
     * Cuantos dias antes del vencimiento se avisa. Constante y no configurable
     * por usuario: es la decision mas simple que cumple el escenario, y §18
     * pide evitar una preferencia nueva hasta que alguien la pida de verdad.
     */
    private static final int DUE_SOON_DAYS = 3;

    private final BudgetCycleService cycles;
    private final NotificationService notifications;
    private final Clock clock;

    public PaymentAlertsJob(BudgetCycleService cycles, NotificationService notifications, Clock clock) {
        this.cycles = cycles;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Scheduled(cron = "0 20 4 * * *")
    public void generate() {
        LocalDate hoy = LocalDate.now(clock);

        int proximos = 0;
        for (DueSoonItem item : cycles.itemsDueOn(hoy.plusDays(DUE_SOON_DAYS))) {
            notifications.notifyPaymentDueSoon(
                    item.userId(), item.itemPublicId(), item.itemName(), item.plannedAmount(), item.dueDate());
            proximos++;
        }

        int deficits = 0;
        for (CycleDeficit deficit : cycles.activeCyclesInDeficit()) {
            notifications.notifyCycleDeficit(
                    deficit.userId(), deficit.cyclePublicId(), deficit.missingAmount());
            deficits++;
        }

        if (proximos > 0 || deficits > 0) {
            log.info(
                    "Alertas revisadas: {} pago(s) proximo(s) y {} ciclo(s) en deficit (pueden ya existir)",
                    proximos,
                    deficits);
        }
    }
}
