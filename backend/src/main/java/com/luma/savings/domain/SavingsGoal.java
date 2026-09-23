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

        SavingsGoal goal = new SavingsGoal();
        goal.publicId = UUID.randomUUID().toString();
        goal.userId = userId;
        goal.name = name;
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
}
