package com.luma.budget.infrastructure;

import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleStatus;
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

    boolean existsByUserIdAndStartDate(Long userId, java.time.LocalDate startDate);
}
