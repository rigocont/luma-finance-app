package com.luma.savings.infrastructure;

import com.luma.savings.domain.SavingsGoal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdAndDeletedAtIsNullOrderByPriorityAsc(Long userId);
}
