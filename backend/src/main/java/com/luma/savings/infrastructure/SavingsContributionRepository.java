package com.luma.savings.infrastructure;

import com.luma.savings.domain.SavingsContribution;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsContributionRepository extends JpaRepository<SavingsContribution, Long> {

    List<SavingsContribution> findBySavingsGoalIdOrderByContributionDateDescIdDesc(
            Long savingsGoalId);

    /**
     * El movimiento que nacio de un renglon de ciclo, si ya existe.
     *
     * <p>Es la defensa contra el doble conteo: confirmar dos veces el mismo
     * renglon no puede sumar dos veces a la meta.
     */
    Optional<SavingsContribution> findByCycleItemId(Long cycleItemId);
}
