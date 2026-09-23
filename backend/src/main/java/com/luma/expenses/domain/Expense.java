package com.luma.expenses.domain;

import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.budget.domain.RecurrenceSchedule;
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
 * Un gasto, fijo o variable.
 *
 * <p>Una sola tabla para ambos (hallazgo 1.3 del analisis): tenerlos separados
 * duplicaba el calculo sin resolver nada. La diferencia real esta en como se
 * materializan, no en como se guardan.
 */
@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_kind", nullable = false, length = 16)
    private ExpenseKind expenseKind;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Frequency frequency;

    @Column(name = "due_day")
    private Integer dueDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Flexibility flexibility;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Expense() {
        // Requerido por JPA.
    }

    public static Expense create(
            Long userId,
            Long categoryId,
            String name,
            ExpenseKind kind,
            Money amount,
            Frequency frequency,
            Integer dueDay,
            Flexibility flexibility,
            LocalDate startDate) {

        Expense expense = new Expense();
        expense.publicId = UUID.randomUUID().toString();
        expense.userId = userId;
        expense.categoryId = categoryId;
        expense.name = name;
        expense.expenseKind = kind;
        expense.amount = amount.amount();
        expense.frequency = frequency;
        expense.dueDay = dueDay;
        expense.flexibility = flexibility;
        expense.startDate = startDate;
        expense.active = true;
        return expense;
    }

    public RecurrenceSchedule schedule() {
        return new RecurrenceSchedule(frequency, dueDay, startDate, endDate);
    }

    public Money money(String currency) {
        return Money.of(amount, currency);
    }

    public boolean isVariable() {
        return expenseKind == ExpenseKind.VARIABLE;
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

    public Long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public ExpenseKind getExpenseKind() {
        return expenseKind;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public Flexibility getFlexibility() {
        return flexibility;
    }

    public boolean isActive() {
        return active && deletedAt == null;
    }
}
