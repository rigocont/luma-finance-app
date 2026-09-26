package com.luma.savings.domain;

import com.luma.common.model.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Un movimiento de una meta: aportacion o retiro.
 *
 * <p>El progreso de la meta esta desnormalizado en {@code current_amount} y se
 * mantiene al registrar cada movimiento, en la misma transaccion. Recalcular la
 * suma en cada lectura del dashboard seria caro para un dato que cambia poco.
 * Estas filas son la fuente de verdad que permite reconstruirlo.
 */
@Entity
@Table(name = "savings_contributions")
public class SavingsContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "savings_goal_id", nullable = false)
    private Long savingsGoalId;

    /**
     * El renglon del ciclo que lo origino, cuando vino de un plan.
     *
     * <p>Nulo en aportaciones extra y retiros. No es solo trazabilidad: es lo
     * que permite saber si el renglon de un ciclo ya se registro en la meta, y
     * evitar contarlo dos veces.
     */
    @Column(name = "cycle_item_id")
    private Long cycleItemId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "contribution_date", nullable = false)
    private LocalDate contributionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ContributionType type;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected SavingsContribution() {
        // Requerido por JPA.
    }

    public static SavingsContribution of(
            Long savingsGoalId,
            Long cycleItemId,
            Money amount,
            LocalDate date,
            ContributionType type,
            String notes) {

        SavingsContribution contribution = new SavingsContribution();
        contribution.publicId = UUID.randomUUID().toString();
        contribution.savingsGoalId = savingsGoalId;
        contribution.cycleItemId = cycleItemId;
        // El retiro se guarda negativo: asi la suma de los movimientos siempre
        // es el progreso, sin casos especiales al sumar.
        contribution.amount = type == ContributionType.WITHDRAWAL
                ? amount.amount().abs().negate()
                : amount.amount().abs();
        contribution.contributionDate = date;
        contribution.type = type;
        contribution.notes = notes;
        return contribution;
    }

    public Money money(String currency) {
        return Money.of(amount, currency);
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getSavingsGoalId() {
        return savingsGoalId;
    }

    public Long getCycleItemId() {
        return cycleItemId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getContributionDate() {
        return contributionDate;
    }

    public ContributionType getType() {
        return type;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
