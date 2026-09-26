package com.luma.expenses.infrastructure;

import com.luma.expenses.domain.Expense;
import com.luma.expenses.domain.ExpenseKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Los metodos siempre llevan el userId: es la barrera que impide que un usuario
 * lea los datos de otro. Los eliminados nunca salen.
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** Los que cuentan en el presupuesto. Lo usa la materializacion de ciclos. */
    List<Expense> findByUserIdAndActiveTrueAndDeletedAtIsNull(Long userId);

    Optional<Expense> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);

    /**
     * Todos los del usuario, cuenten o no en el presupuesto.
     *
     * <p>Lo usa la limpieza de altas abandonadas, que tiene que barrer TODO lo
     * capturado y no solo lo que estaba activo.
     */
    List<Expense> findByUserIdAndDeletedAtIsNull(Long userId);

    /** Cuantos gastos usan una categoria. Evita dejarlos huerfanos sin avisar. */
    long countByCategoryIdAndUserIdAndDeletedAtIsNull(Long categoryId, Long userId);

    @Query("""
            SELECT e FROM Expense e
            WHERE e.userId = :userId
              AND e.deletedAt IS NULL
              AND (:kind IS NULL OR e.expenseKind = :kind)
              AND (:categoryId IS NULL OR e.categoryId = :categoryId)
              AND (:active IS NULL OR e.active = :active)
            """)
    Page<Expense> search(
            @Param("userId") Long userId,
            @Param("kind") ExpenseKind kind,
            @Param("categoryId") Long categoryId,
            @Param("active") Boolean active,
            Pageable pageable);
}
