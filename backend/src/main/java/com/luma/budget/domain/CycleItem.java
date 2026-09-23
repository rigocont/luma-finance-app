package com.luma.budget.domain;

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
 * Un renglon materializado dentro de un ciclo. La entidad central del producto.
 *
 * <p>{@code name}, {@code categoryId} y {@code flexibility} son COPIAS de la
 * plantilla, no referencias. Si manana sube la renta, este renglon conserva lo
 * que decia cuando se genero el ciclo. Sin eso, los ciclos cerrados cambiarian
 * solos y la historia financiera dejaria de ser confiable.
 *
 * <p>{@code sourceId} tampoco es llave foranea: la plantilla puede borrarse y el
 * historial debe sobrevivir.
 */
@Entity
@Table(name = "cycle_items")
public class CycleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "budget_cycle_id", nullable = false)
    private Long budgetCycleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 24)
    private CycleItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 24)
    private ItemSource sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "planned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal plannedAmount;

    @Column(name = "actual_amount", precision = 15, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ItemStatus status;

    /**
     * El dia en que la persona dice que ocurrio. Es una fecha de calendario: "lo
     * pague el 3" no depende de la zona horaria de nadie.
     */
    @Column(name = "settled_on")
    private LocalDate settledOn;

    /** Cuando se registro en el sistema. Dato de auditoria, distinto del anterior. */
    @Column(name = "settled_at")
    private Instant settledAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Flexibility flexibility;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected CycleItem() {
        // Requerido por JPA.
    }

    public static CycleItem materialize(
            Long budgetCycleId,
            CycleItemType itemType,
            ItemSource sourceType,
            Long sourceId,
            String name,
            Long categoryId,
            Money plannedAmount,
            LocalDate dueDate,
            ItemStatus status,
            Flexibility flexibility,
            int displayOrder) {

        CycleItem item = new CycleItem();
        item.publicId = UUID.randomUUID().toString();
        item.budgetCycleId = budgetCycleId;
        item.itemType = itemType;
        item.sourceType = sourceType;
        item.sourceId = sourceId;
        item.name = name;
        item.categoryId = categoryId;
        item.plannedAmount = plannedAmount.amount();
        item.dueDate = dueDate;
        item.status = status;
        item.flexibility = flexibility;
        item.displayOrder = displayOrder;
        return item;
    }

    /** Vista inmutable para la calculadora, que no conoce JPA. */
    public PlannedItem toPlannedItem(String currency) {
        return new PlannedItem(
                itemType,
                status,
                Money.of(plannedAmount, currency),
                actualAmount != null ? Money.of(actualAmount, currency) : null);
    }

    /**
     * Confirma lo que de verdad ocurrio.
     *
     * <p>El estado resultante depende de si el monto real cubrio lo planeado. Un
     * pago menor deja el renglon en {@link ItemStatus#PARTIAL}, no en PAID: la
     * diferencia sigue siendo dinero que hay que sacar de algun lado, y decir que
     * ya se pago lo esconderia.
     *
     * <p>El instante de registro llega de fuera para que el dominio no dependa
     * del reloj del sistema y los tests puedan fijarlo.
     */
    public void settle(Money actual, LocalDate settledOn, Instant recordedAt) {
        this.actualAmount = actual.amount();
        this.settledOn = settledOn;
        this.settledAt = recordedAt;
        this.status = actual.amount().compareTo(plannedAmount) >= 0
                ? ItemStatus.PAID
                : ItemStatus.PARTIAL;
    }

    /** Lo saca del ciclo sin desactivar la plantilla. */
    public void skip() {
        this.status = ItemStatus.SKIPPED;
        this.actualAmount = null;
        this.settledOn = null;
        this.settledAt = null;
    }

    /** Vuelve a dejarlo pendiente. Deshace una confirmacion o una omision. */
    public void reopen() {
        this.status = ItemStatus.PENDING;
        this.actualAmount = null;
        this.settledOn = null;
        this.settledAt = null;
    }

    public void changePlannedAmount(Money amount) {
        this.plannedAmount = amount.amount();
        // Confirmar el monto de un gasto variable es justo lo que lo saca de
        // revision: ya se sabe cuanto es esta vez.
        if (this.status == ItemStatus.NEEDS_REVIEW) {
            this.status = ItemStatus.PENDING;
        }
    }

    public void changeDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void changeNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Paso su fecha sin ocurrir.
     *
     * <p>Un renglon PARTIAL no se marca vencido aunque su fecha ya paso: el
     * estado es un solo campo, y PARTIAL dice mas que OVERDUE. Que falte una
     * parte se ve comparando el monto real con el planeado.
     */
    public boolean becameOverdue(LocalDate today) {
        if (dueDate == null || status.isSettled() || status == ItemStatus.SKIPPED) {
            return false;
        }
        return dueDate.isBefore(today) && status != ItemStatus.OVERDUE;
    }

    public void markOverdue() {
        this.status = ItemStatus.OVERDUE;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getBudgetCycleId() {
        return budgetCycleId;
    }

    public CycleItemType getItemType() {
        return itemType;
    }

    public ItemSource getSourceType() {
        return sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getName() {
        return name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public BigDecimal getPlannedAmount() {
        return plannedAmount;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public LocalDate getSettledOn() {
        return settledOn;
    }

    public Instant getSettledAt() {
        return settledAt;
    }

    public Flexibility getFlexibility() {
        return flexibility;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getNotes() {
        return notes;
    }
}
