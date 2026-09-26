package com.luma.savings.domain;

import com.luma.common.error.BusinessRuleException;
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
import java.util.Objects;
import java.util.UUID;

/** Una meta de ahorro. */
@Entity
@Table(name = "savings_goals")
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_mode", nullable = false, length = 24)
    private ContributionMode contributionMode;

    @Column(name = "planned_per_cycle", precision = 15, scale = 2)
    private BigDecimal plannedPerCycle;

    @Column(nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GoalStatus status;

    @Column(length = 40)
    private String icon;

    @Column(length = 9)
    private String color;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected SavingsGoal() {
        // Requerido por JPA.
    }

    public static SavingsGoal create(
            Long userId,
            String name,
            Money targetAmount,
            LocalDate targetDate,
            ContributionMode mode,
            Money plannedPerCycle,
            int priority) {

        if (!targetAmount.isPositive()) {
            throw new BusinessRuleException("La meta tiene que ser mayor que cero.");
        }
        if (mode == ContributionMode.FIXED_PER_CYCLE
                && (plannedPerCycle == null || !plannedPerCycle.isPositive())) {
            throw new BusinessRuleException(
                    "Con aporte fijo por ciclo hay que indicar cuanto vas a apartar.");
        }
        if (mode == ContributionMode.AUTO_BY_TARGET_DATE && targetDate == null) {
            // Sin fecha no hay nada que repartir: el calculo del aporte por
            // ciclo necesita saber en cuantos ciclos cabe lo que falta.
            throw new BusinessRuleException(
                    "Para calcular el aporte hace falta una fecha objetivo.");
        }

        SavingsGoal goal = new SavingsGoal();
        goal.publicId = UUID.randomUUID().toString();
        goal.userId = Objects.requireNonNull(userId, "userId");
        goal.name = requireName(name);
        goal.targetAmount = targetAmount.amount();
        goal.currentAmount = BigDecimal.ZERO;
        goal.targetDate = targetDate;
        goal.contributionMode = mode;
        goal.plannedPerCycle = plannedPerCycle != null ? plannedPerCycle.amount() : null;
        goal.priority = priority;
        goal.status = GoalStatus.ACTIVE;
        return goal;
    }

    public Money target(String currency) {
        return Money.of(targetAmount, currency);
    }

    public Money saved(String currency) {
        return Money.of(currentAmount, currency);
    }

    public Money plannedContribution(String currency) {
        return plannedPerCycle != null ? Money.of(plannedPerCycle, currency) : Money.zero(currency);
    }

    public boolean isActive() {
        return status == GoalStatus.ACTIVE && deletedAt == null;
    }

    /** Solo las metas con un plan comprometido restan del presupuesto. */
    public boolean affectsBudget() {
        return isActive() && contributionMode.affectsBudget();
    }

    /** Lo que falta para llegar. Nunca negativo: si te pasaste, es cero. */
    public Money remaining(String currency) {
        Money falta = target(currency).subtract(saved(currency));
        return falta.isNegative() ? Money.zero(currency) : falta;
    }

    public boolean isCompleted() {
        return status == GoalStatus.COMPLETED;
    }

    // -----------------------------------------------------------------------
    // Cambios
    // -----------------------------------------------------------------------

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void changeTarget(Money targetAmount) {
        if (!targetAmount.isPositive()) {
            throw new BusinessRuleException("La meta tiene que ser mayor que cero.");
        }
        this.targetAmount = targetAmount.amount();
        refreshCompletion();
    }

    public void changeTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public void changePlan(ContributionMode mode, Money plannedPerCycle) {
        Objects.requireNonNull(mode, "mode");

        // Un cambio que no trae monto conserva el que ya habia. Editar solo el
        // nombre no puede borrar el aporte: en una edicion parcial, "no mande
        // el campo" significa "no lo toques", no "ponlo en nada".
        BigDecimal monto =
                plannedPerCycle != null ? plannedPerCycle.amount() : this.plannedPerCycle;

        if (mode == ContributionMode.FIXED_PER_CYCLE && (monto == null || monto.signum() <= 0)) {
            throw new BusinessRuleException(
                    "Con aporte fijo por ciclo hay que indicar cuanto vas a apartar.");
        }
        if (mode == ContributionMode.AUTO_BY_TARGET_DATE && targetDate == null) {
            throw new BusinessRuleException(
                    "Para calcular el aporte hace falta una fecha objetivo.");
        }

        this.contributionMode = mode;
        this.plannedPerCycle = monto;
    }

    public void changePriority(int priority) {
        this.priority = priority;
    }

    /**
     * Icono y color. Lo que llega nulo no se toca.
     *
     * <p>Mandar el icono sin el color no puede borrar el color: son dos campos
     * independientes que viajan en la misma llamada, no un par.
     */
    public void changeLook(String icon, String color) {
        if (icon != null) {
            this.icon = icon;
        }
        if (color != null) {
            this.color = color;
        }
    }

    /**
     * Suma una aportacion al progreso. Un retiro llega con monto negativo.
     *
     * <p>El progreso nunca baja de cero: no se puede sacar mas de lo que hay, y
     * un numero negativo en una barra de progreso no significa nada.
     */
    public void applyContribution(Money amount) {
        Money nuevo = saved(amount.currency()).add(amount);

        if (nuevo.isNegative()) {
            throw new BusinessRuleException(
                    "No puedes retirar mas de lo que llevas ahorrado en esta meta.");
        }

        this.currentAmount = nuevo.amount();
        refreshCompletion();
    }

    /**
     * Marca o desmarca la meta como alcanzada segun el progreso.
     *
     * <p>Se revisa en los dos sentidos a proposito: subir la meta despues de
     * haberla alcanzado la vuelve a poner en marcha, y eso es justo lo que la
     * persona esta pidiendo al subirla.
     */
    private void refreshCompletion() {
        boolean alcanzada = currentAmount.compareTo(targetAmount) >= 0;

        if (alcanzada && status == GoalStatus.ACTIVE) {
            this.status = GoalStatus.COMPLETED;
        } else if (!alcanzada && status == GoalStatus.COMPLETED) {
            this.status = GoalStatus.ACTIVE;
        }
    }

    public void pause() {
        if (status == GoalStatus.ACTIVE) {
            this.status = GoalStatus.PAUSED;
        }
    }

    public void resume() {
        if (deletedAt != null) {
            throw new BusinessRuleException("Una meta eliminada no se puede reanudar.");
        }
        if (status == GoalStatus.PAUSED) {
            this.status = GoalStatus.ACTIVE;
            refreshCompletion();
        }
    }

    public void softDelete(Instant when) {
        this.deletedAt = when;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("El nombre de la meta es obligatorio.");
        }
        return name.trim();
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public ContributionMode getContributionMode() {
        return contributionMode;
    }

    public int getPriority() {
        return priority;
    }

    public GoalStatus getStatus() {
        return status;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public BigDecimal getPlannedPerCycle() {
        return plannedPerCycle;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
