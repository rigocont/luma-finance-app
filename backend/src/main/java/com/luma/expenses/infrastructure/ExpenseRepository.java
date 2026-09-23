package com.luma.expenses.infrastructure;

import com.luma.expenses.domain.Expense;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserIdAndActiveTrueAndDeletedAtIsNull(Long userId);
}
