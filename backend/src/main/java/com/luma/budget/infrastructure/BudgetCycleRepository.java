package com.luma.budget.infrastructure;

import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Todas las consultas llevan userId. Es la barrera que impide leer los ciclos de
 * otra persona, y por eso no existe un {@code findByPublicId} suelto.
 */
public interface BudgetCycleRepository extends JpaRepository<BudgetCycle, Long> {

    Optional<BudgetCycle> findByPublicIdAndUserId(String publicId, Long userId);

    Optional<BudgetCycle> findFirstByUserIdAndStatusOrderByStartDateDesc(
            Long userId, CycleStatus status);

    List<BudgetCycle> findByUserIdAndStatus(Long userId, CycleStatus status);

    /** Para las tareas programadas, que recorren los ciclos de todos los usuarios. */
    List<BudgetCycle> findByStatus(CycleStatus status);

    Page<BudgetCycle> findByUserIdOrderByStartDateDesc(Long userId, Pageable pageable);

    Optional<BudgetCycle> findFirstByUserIdOrderBySequenceNumberDesc(Long userId);

    /**
     * El ciclo inmediatamente anterior a una fecha.
     *
     * <p>Se busca por fecha y no por numero de secuencia porque la sugerencia de
     * monto es una pregunta del calendario —"cuanto fue la vez pasada"— y no del
     * orden en que se crearon los ciclos.
     */
    Optional<BudgetCycle> findFirstByUserIdAndStartDateLessThanOrderByStartDateDesc(
            Long userId, LocalDate startDate);

    boolean existsByUserIdAndStartDate(Long userId, LocalDate startDate);
}
