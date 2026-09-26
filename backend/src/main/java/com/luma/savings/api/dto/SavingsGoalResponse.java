package com.luma.savings.api.dto;

import com.luma.common.web.MoneyDto;
import com.luma.savings.domain.SavingsGoal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Una meta de ahorro.
 *
 * @param progress proporcion alcanzada, de 0 a 1. Se calcula en el servidor
 *     igual que todas las cifras del producto: el cliente presenta, no deriva.
 * @param remaining lo que falta. Nunca negativo: si te pasaste, es cero.
 * @param affectsBudget la meta resta del presupuesto de cada ciclo. Las metas en
 *     modo manual no lo hacen, y la interfaz tiene que poder decirlo sin
 *     reimplementar la regla.
 */
public record SavingsGoalResponse(
        String id,
        String name,
        MoneyDto target,
        MoneyDto saved,
        MoneyDto remaining,
        BigDecimal progress,
        LocalDate targetDate,
        String contributionMode,
        MoneyDto plannedPerCycle,
        int priority,
        String status,
        boolean affectsBudget,
        String icon,
        String color,
        Instant createdAt) {

    /** Cuatro decimales: los mismos que usan las proporciones del balance. */
    private static final int ESCALA = 4;

    public static SavingsGoalResponse from(SavingsGoal goal, String currency) {
        return new SavingsGoalResponse(
                goal.getPublicId(),
                goal.getName(),
                MoneyDto.from(goal.target(currency)),
                MoneyDto.from(goal.saved(currency)),
                MoneyDto.from(goal.remaining(currency)),
                progreso(goal),
                goal.getTargetDate(),
                goal.getContributionMode().name(),
                MoneyDto.from(goal.plannedContribution(currency)),
                goal.getPriority(),
                goal.getStatus().name(),
                goal.affectsBudget(),
                goal.getIcon(),
                goal.getColor(),
                goal.getCreatedAt());
    }

    private static BigDecimal progreso(SavingsGoal goal) {
        BigDecimal objetivo = goal.getTargetAmount();

        // La meta nunca puede ser cero (lo valida el dominio), pero dividir sin
        // comprobarlo seria confiar en que esa regla no cambie nunca.
        if (objetivo.signum() <= 0) {
            return BigDecimal.ZERO.setScale(ESCALA);
        }

        BigDecimal razon = goal.getCurrentAmount().divide(objetivo, ESCALA, RoundingMode.HALF_UP);

        // Se recorta a 1: una barra de progreso al 130% no significa nada.
        return razon.min(BigDecimal.ONE.setScale(ESCALA));
    }
}
