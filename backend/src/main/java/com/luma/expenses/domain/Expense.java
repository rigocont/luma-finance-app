package com.luma.expenses.domain;

import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.budget.domain.RecurrenceSchedule;
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

        return create(
                userId, categoryId, name, kind, amount, frequency, dueDay, flexibility,
                startDate, null);
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
            LocalDate startDate,
            LocalDate endDate) {

        if (amount.isNegative()) {
            throw new BusinessRuleException("El monto de un gasto no puede ser negativo.");
        }
        // Valida el calendario antes de construir nada: mejor rechazar la
        // peticion que guardar un gasto que luego no se puede materializar.
        new RecurrenceSchedule(frequency, dueDay, startDate, endDate);

        Expense expense = new Expense();
        expense.publicId = UUID.randomUUID().toString();
        expense.userId = Objects.requireNonNull(userId, "userId");
        expense.categoryId = categoryId;
        expense.name = requireName(name);
        expense.expenseKind = Objects.requireNonNull(kind, "kind");
        expense.amount = amount.amount();
        expense.frequency = frequency;
        expense.dueDay = dueDay;
        expense.flexibility = Objects.requireNonNull(flexibility, "flexibility");
        expense.startDate = startDate;
        expense.endDate = endDate;
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

    // -----------------------------------------------------------------------
    // Cambios
    // -----------------------------------------------------------------------
    // Ninguno toca los ciclos ya abiertos: sus renglones son copias. Editar un
    // gasto aplica desde el siguiente ciclo.

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void changeAmount(Money amount) {
        if (amount.isNegative()) {
            throw new BusinessRuleException("El monto de un gasto no puede ser negativo.");
        }
        this.amount = amount.amount();
    }

    public void changeKind(ExpenseKind kind) {
        this.expenseKind = Objects.requireNonNull(kind, "kind");
    }

    /** Nulo deja el gasto sin categoria, que es un estado valido. */
    public void changeCategory(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void changeFlexibility(Flexibility flexibility) {
        this.flexibility = Objects.requireNonNull(flexibility, "flexibility");
    }

    public void changeSchedule(
            Frequency frequency, Integer dueDay, LocalDate startDate, LocalDate endDate) {

        new RecurrenceSchedule(frequency, dueDay, startDate, endDate);

        this.frequency = frequency;
        this.dueDay = dueDay;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void changeNotes(String notes) {
        this.notes = notes;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        if (deletedAt != null) {
            throw new BusinessRuleException("Un gasto eliminado no se puede reactivar.");
        }
        this.active = true;
    }

    /**
     * Borrado logico. La fila se queda porque los renglones de ciclos pasados
     * guardan su origen, y un historial sin explicacion no sirve de nada.
     */
    public void softDelete(Instant when) {
        this.deletedAt = when;
        this.active = false;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("El nombre del gasto es obligatorio.");
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

    public Integer getDueDay() {
        return dueDay;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Cuenta en el presupuesto.
     *
     * <p>{@code active} es "por ahora no lo cuentes" y la persona lo prende y
     * apaga; {@code deletedAt} es "ya no existe".
     */
    public boolean isActive() {
        return active && deletedAt == null;
    }

    /** El valor crudo de la bandera, sin considerar el borrado. Lo usa la API. */
    public boolean isActiveFlag() {
        return active;
    }
}
