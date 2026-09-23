package com.luma.income.domain;

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

/** Un ingreso recurrente o extraordinario. Plantilla de la que se materializan los ciclos. */
@Entity
@Table(name = "incomes")
public class Income {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_type", nullable = false, length = 24)
    private IncomeType incomeType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Frequency frequency;

    @Column(name = "expected_day")
    private Integer expectedDay;

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

    protected Income() {
        // Requerido por JPA.
    }

    public static Income create(
            Long userId,
            String name,
            IncomeType incomeType,
            Money amount,
            Frequency frequency,
            Integer expectedDay,
            LocalDate startDate) {

        return create(userId, name, incomeType, amount, frequency, expectedDay, startDate, null);
    }

    public static Income create(
            Long userId,
            String name,
            IncomeType incomeType,
            Money amount,
            Frequency frequency,
            Integer expectedDay,
            LocalDate startDate,
            LocalDate endDate) {

        if (amount.isNegative()) {
            throw new BusinessRuleException("El monto de un ingreso no puede ser negativo.");
        }
        // Valida el calendario antes de construir nada: es preferible rechazar
        // la peticion a guardar un ingreso que despues no se puede materializar.
        new RecurrenceSchedule(frequency, expectedDay, startDate, endDate);

        Income income = new Income();
        income.publicId = UUID.randomUUID().toString();
        income.userId = Objects.requireNonNull(userId, "userId");
        income.name = requireName(name);
        income.incomeType = Objects.requireNonNull(incomeType, "incomeType");
        income.amount = amount.amount();
        income.frequency = frequency;
        income.expectedDay = expectedDay;
        income.startDate = startDate;
        income.endDate = endDate;
        income.active = true;
        return income;
    }

    /** Como se reparte en el calendario. Lo usa la materializacion de ciclos. */
    public RecurrenceSchedule schedule() {
        return new RecurrenceSchedule(frequency, expectedDay, startDate, endDate);
    }

    public Money money(String currency) {
        return Money.of(amount, currency);
    }

    /** El monto es una estimacion: al materializarse pide confirmacion. */
    public boolean requiresReview() {
        return incomeType.requiresReview();
    }

    // -----------------------------------------------------------------------
    // Cambios
    // -----------------------------------------------------------------------
    // Ninguno toca los ciclos ya abiertos: los renglones son copias, y esa es
    // justo la propiedad que hace confiable el historial. Editar un ingreso
    // aplica desde el siguiente ciclo.

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void changeAmount(Money amount) {
        if (amount.isNegative()) {
            throw new BusinessRuleException("El monto de un ingreso no puede ser negativo.");
        }
        this.amount = amount.amount();
    }

    public void changeType(IncomeType incomeType) {
        this.incomeType = Objects.requireNonNull(incomeType, "incomeType");
    }

    /**
     * Cambia cuando y cada cuanto se espera.
     *
     * <p>Se valida construyendo un {@link RecurrenceSchedule} y descartandolo:
     * asi la regla de que el fin no puede ir antes del inicio, y la del dia
     * entre 1 y 31, viven en un solo lugar en vez de duplicarse en cada entidad
     * que las necesite.
     */
    public void changeSchedule(
            Frequency frequency, Integer expectedDay, LocalDate startDate, LocalDate endDate) {

        new RecurrenceSchedule(frequency, expectedDay, startDate, endDate);

        this.frequency = frequency;
        this.expectedDay = expectedDay;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void changeNotes(String notes) {
        this.notes = notes;
    }

    /** Deja de contar en los ciclos siguientes, sin perder el historial. */
    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        if (deletedAt != null) {
            throw new BusinessRuleException("Un ingreso eliminado no se puede reactivar.");
        }
        this.active = true;
    }

    /**
     * Borrado logico.
     *
     * <p>Nunca se borra la fila. Los renglones de ciclos pasados guardan su
     * {@code sourceId}, y aunque no es llave foranea a proposito, perder el
     * origen dejaria el historial sin explicacion. Ademas un borrado real no se
     * deshace, y aqui la persona se equivoca con un clic.
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
            throw new BusinessRuleException("El nombre del ingreso es obligatorio.");
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

    public IncomeType getIncomeType() {
        return incomeType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public Integer getExpectedDay() {
        return expectedDay;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Cuenta en el presupuesto.
     *
     * <p>Son dos cosas distintas: {@code active} es "por ahora no lo cuentes" y
     * el usuario lo prende y apaga; {@code deletedAt} es "ya no existe". Un
     * ingreso eliminado nunca esta activo, pero uno inactivo si puede volver.
     */
    public boolean isActive() {
        return active && deletedAt == null;
    }

    /** El valor crudo de la bandera, sin considerar el borrado. Lo usa la API. */
    public boolean isActiveFlag() {
        return active;
    }
}
