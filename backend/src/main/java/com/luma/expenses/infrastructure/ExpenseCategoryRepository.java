package com.luma.expenses.infrastructure;

import com.luma.expenses.domain.ExpenseCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    /**
     * El catalogo que ve una persona: las del sistema mas las suyas.
     *
     * <p>El {@code userId} nulo de las del sistema es lo que las hace comunes a
     * todos. Sin la condicion de usuario, alguien veria las categorias que otro
     * invento.
     */
    @Query("""
            SELECT c FROM ExpenseCategory c
            WHERE c.deletedAt IS NULL
              AND (c.userId IS NULL OR c.userId = :userId)
            ORDER BY c.displayOrder ASC, c.name ASC
            """)
    List<ExpenseCategory> catalogFor(@Param("userId") Long userId);

    /** Una categoria concreta, verificando que la persona tenga derecho a usarla. */
    @Query("""
            SELECT c FROM ExpenseCategory c
            WHERE c.publicId = :publicId
              AND c.deletedAt IS NULL
              AND (c.userId IS NULL OR c.userId = :userId)
            """)
    Optional<ExpenseCategory> findUsable(
            @Param("publicId") String publicId, @Param("userId") Long userId);
}
