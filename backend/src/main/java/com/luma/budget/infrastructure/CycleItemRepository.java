package com.luma.budget.infrastructure;

import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.ItemHistoryEntry;
import com.luma.budget.domain.ItemSource;
import com.luma.budget.domain.ItemStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CycleItemRepository extends JpaRepository<CycleItem, Long> {

    List<CycleItem> findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(Long budgetCycleId);

    List<CycleItem> findByBudgetCycleIdAndItemTypeOrderByDueDateAscIdAsc(
            Long budgetCycleId, CycleItemType itemType);

    List<CycleItem> findByBudgetCycleIdAndStatusOrderByDueDateAscIdAsc(
            Long budgetCycleId, ItemStatus status);

    Optional<CycleItem> findByPublicIdAndBudgetCycleId(String publicId, Long budgetCycleId);

    List<CycleItem> findByBudgetCycleIdIn(List<Long> budgetCycleIds);

    /**
     * Los renglones de un ciclo que salieron de ciertas plantillas.
     *
     * <p>Sirve para traer de un golpe lo que costo el ciclo pasado cada gasto
     * que hoy pide revision, en vez de una consulta por renglon.
     */
    List<CycleItem> findByBudgetCycleIdAndSourceTypeAndSourceIdIn(
            Long budgetCycleId, ItemSource sourceType, Collection<Long> sourceIds);

    /**
     * Lo que costo un gasto en los ciclos anteriores, del mas reciente al mas
     * antiguo.
     *
     * <p>El cruce se escribe a mano porque {@code CycleItem} guarda el id del
     * ciclo como un numero, no como una asociacion: los renglones son copias y
     * no deben poder navegar a su ciclo ni arrastrarlo en cada carga.
     *
     * <p>El filtro por usuario NO es decorativo aunque el id del gasto ya sea
     * unico: es la misma barrera que llevan todas las consultas del modulo, y
     * quitarla dejaria el historial a merced de un id adivinado.
     */
    @Query("""
            SELECT new com.luma.budget.domain.ItemHistoryEntry(
                i.sourceId, c.publicId, c.startDate, c.endDate,
                i.plannedAmount, i.actualAmount, i.settledOn)
            FROM CycleItem i, BudgetCycle c
            WHERE i.budgetCycleId = c.id
              AND c.userId = :userId
              AND c.startDate < :before
              AND i.sourceType = :sourceType
              AND i.sourceId = :sourceId
              AND i.actualAmount IS NOT NULL
            ORDER BY c.startDate DESC
            """)
    List<ItemHistoryEntry> historyOfSource(
            @Param("userId") Long userId,
            @Param("sourceType") ItemSource sourceType,
            @Param("sourceId") Long sourceId,
            @Param("before") LocalDate before,
            Pageable pageable);
}
