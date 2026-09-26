package com.luma.savings.infrastructure;

import com.luma.savings.domain.GoalStatus;
import com.luma.savings.domain.SavingsGoal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Los metodos siempre llevan el userId. Las metas eliminadas nunca salen.
 *
 * <p>No se pagina a proposito: una persona tiene metas de ahorro, no cientos, y
 * reordenarlas por prioridad exige tenerlas todas a la vista.
 */
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdAndDeletedAtIsNullOrderByPriorityAsc(Long userId);

    Optional<SavingsGoal> findByPublicIdAndUserIdAndDeletedAtIsNull(String publicId, Long userId);

    @Query("""
            SELECT g FROM SavingsGoal g
            WHERE g.userId = :userId
              AND g.deletedAt IS NULL
              AND (:status IS NULL OR g.status = :status)
            ORDER BY g.priority ASC, g.id ASC
            """)
    List<SavingsGoal> search(@Param("userId") Long userId, @Param("status") GoalStatus status);

    /** La prioridad mas alta en uso. Sirve para poner la meta nueva al final. */
    @Query("""
            SELECT COALESCE(MAX(g.priority), 0) FROM SavingsGoal g
            WHERE g.userId = :userId AND g.deletedAt IS NULL
            """)
    int maxPriority(@Param("userId") Long userId);
}
