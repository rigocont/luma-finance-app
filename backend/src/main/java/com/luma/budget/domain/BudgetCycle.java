package com.luma.budget.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Un ciclo presupuestal concreto de un usuario. */
@Entity
@Table(name = "budget_cycles")
public class BudgetCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_type", nullable = false, length = 16)
    private CycleType cycleType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CycleStatus status;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected BudgetCycle() {
        // Requerido por JPA.
    }

    public static BudgetCycle open(
            Long userId, CycleType cycleType, BudgetPeriod period, int sequenceNumber) {

        BudgetCycle cycle = new BudgetCycle();
        cycle.publicId = UUID.randomUUID().toString();
        cycle.userId = userId;
        cycle.cycleType = cycleType;
        cycle.startDate = period.start();
        cycle.endDate = period.end();
        cycle.status = CycleStatus.ACTIVE;
        cycle.sequenceNumber = sequenceNumber;
        return cycle;
    }

    public BudgetPeriod period() {
        return BudgetPeriod.of(startDate, endDate);
    }

    public boolean isMutable() {
        return status.isMutable();
    }

    /** El periodo ya termino, aunque el ciclo siga marcado como activo. */
    public boolean hasEnded(LocalDate today) {
        return endDate.isBefore(today);
    }

    public void close() {
        this.status = CycleStatus.CLOSED;
        this.closedAt = Instant.now();
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

    public CycleType getCycleType() {
        return cycleType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public CycleStatus getStatus() {
        return status;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Instant getClosedAt() {
        return closedAt;
    }
}
