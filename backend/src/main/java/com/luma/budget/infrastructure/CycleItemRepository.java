package com.luma.budget.infrastructure;

import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.ItemStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CycleItemRepository extends JpaRepository<CycleItem, Long> {

    List<CycleItem> findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(Long budgetCycleId);

    List<CycleItem> findByBudgetCycleIdAndItemTypeOrderByDueDateAscIdAsc(
            Long budgetCycleId, CycleItemType itemType);

    List<CycleItem> findByBudgetCycleIdAndStatusOrderByDueDateAscIdAsc(
            Long budgetCycleId, ItemStatus status);

    Optional<CycleItem> findByPublicIdAndBudgetCycleId(String publicId, Long budgetCycleId);

    List<CycleItem> findByBudgetCycleIdIn(List<Long> budgetCycleIds);
}
