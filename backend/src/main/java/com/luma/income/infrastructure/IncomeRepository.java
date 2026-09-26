package com.luma.income.infrastructure;

import com.luma.income.domain.Income;
import com.luma.income.domain.IncomeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Los metodos siempre llevan el userId: es la barrera que impide que un usuario
 * lea los datos de otro. Nunca se busca solo por id.
 *
 * <p>Los ingresos eliminados nunca salen de aqui. El borrado es logico, y su
 * unico proposito es que el historial de ciclos siga teniendo explicacion, no
 * que la persona vuelva a verlos en su lista.
 */
public interface IncomeRepository extends JpaRepository<Income, Long> {

    /** Los que cuentan en el presupuesto. Lo usa la materializacion de ciclos. */
    List<Income> findByUserIdAndActiveTrueAndDeletedAtIsNull(Long userId);

    Optional<Income> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);

    /**
     * Todos los del usuario, cuenten o no en el presupuesto.
     *
     * <p>Lo usa la limpieza de altas abandonadas, que tiene que barrer TODO lo
     * capturado y no solo lo que estaba activo.
     */
    List<Income> findByUserIdAndDeletedAtIsNull(Long userId);

    /**
     * Listado con filtros opcionales.
     *
     * <p>Los dos filtros llegan nulos cuando no se piden. Se resuelve con
     * {@code :param IS NULL OR} en lugar de Specifications: son dos criterios, y
     * montar un criteria builder para esto seria mas codigo del que ahorra.
     */
    @Query("""
            SELECT i FROM Income i
            WHERE i.userId = :userId
              AND i.deletedAt IS NULL
              AND (:type IS NULL OR i.incomeType = :type)
              AND (:active IS NULL OR i.active = :active)
            """)
    Page<Income> search(
            @Param("userId") Long userId,
            @Param("type") IncomeType type,
            @Param("active") Boolean active,
            Pageable pageable);
}
