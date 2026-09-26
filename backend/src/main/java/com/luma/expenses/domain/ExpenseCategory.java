package com.luma.expenses.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Categoria de gasto.
 *
 * <p>Hay dos clases y se distinguen por {@code userId}: las del sistema lo
 * tienen NULO y las ve todo el mundo; las que crea una persona apuntan a su
 * usuario y solo ella las ve. El catalogo base se siembra en la migracion V2.
 *
 * <p>Por ahora es de solo lectura: la aplicacion no deja crear categorias
 * propias todavia. La entidad ya contempla el caso para no tener que migrar el
 * esquema cuando llegue.
 */
@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    /** Nulo en las del sistema. */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 40)
    private String icon;

    @Column(length = 9)
    private String color;

    /** Sugerencia de clasificacion al crear un gasto con esta categoria. */
    @Enumerated(EnumType.STRING)
    @Column(name = "default_kind", nullable = false, length = 16)
    private ExpenseKind defaultKind;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected ExpenseCategory() {
        // Requerido por JPA.
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

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getColor() {
        return color;
    }

    public ExpenseKind getDefaultKind() {
        return defaultKind;
    }

    public boolean isSystem() {
        return system;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }
}
