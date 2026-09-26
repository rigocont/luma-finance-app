package com.luma.onboarding.application;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.expenses.infrastructure.ExpenseRepository;
import com.luma.income.infrastructure.IncomeRepository;
import com.luma.savings.infrastructure.SavingsGoalRepository;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El alta guiada de una cuenta nueva.
 *
 * <p>No guarda un borrador: cada paso del asistente llama a los endpoints que ya
 * existen, asi que capturar un ingreso en el asistente CREA el ingreso. Eso hace
 * que volver despues no necesite recordar nada —el servidor ya sabe que hay— y
 * evita un modelo paralelo al de ingresos, gastos y metas que tarde o temprano
 * se separaria del verdadero.
 *
 * <p>Lo unico que este modulo guarda por su cuenta es el instante en que el alta
 * se dio por terminada, y esa columna ya existia desde la Fase 1.
 *
 * <p>Este servicio orquesta a los demas modulos; ninguno de ellos lo conoce. La
 * dependencia va en un solo sentido y por eso el asistente puede desaparecer sin
 * tocar nada mas.
 */
@Service
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final UserRepository users;
    private final IncomeRepository incomes;
    private final ExpenseRepository expenses;
    private final SavingsGoalRepository goals;
    private final BudgetCycleService cycles;

    public OnboardingService(
            UserRepository users,
            IncomeRepository incomes,
            ExpenseRepository expenses,
            SavingsGoalRepository goals,
            BudgetCycleService cycles) {
        this.users = users;
        this.incomes = incomes;
        this.expenses = expenses;
        this.goals = goals;
        this.cycles = cycles;
    }

    /**
     * El paso al que conviene volver.
     *
     * <p>Se deduce de lo que hay, no de un numero guardado. Un paso guardado
     * miente en cuanto la persona borra algo desde otra pantalla, y obligaria a
     * una columna nueva para un dato que ya se puede calcular.
     */
    public enum Step {
        /** Cada cuanto presupuestas. Siempre visitable: hay valores por omision. */
        CYCLE,
        /** Lo unico obligatorio: sin ingreso no hay presupuesto que calcular. */
        INCOMES,
        EXPENSES,
        SAVINGS,
        SUMMARY
    }

    /** Lo que el asistente necesita saber para retomar donde se quedo. */
    public record State(
            boolean completed,
            Step resumeStep,
            int incomeCount,
            int expenseCount,
            int goalCount,
            boolean canFinish) {}

    @Transactional(readOnly = true)
    public State stateOf(Long userId) {
        User user = requireUser(userId);

        int ingresos = incomes.findByUserIdAndDeletedAtIsNull(userId).size();
        int gastos = expenses.findByUserIdAndDeletedAtIsNull(userId).size();
        int metas = goals.findByUserIdAndDeletedAtIsNullOrderByPriorityAsc(userId).size();

        // Solo los ingresos son obligatorios, asi que solo ellos pueden retener
        // a alguien en un paso. Quien ya tiene uno vuelve al resumen: mandarlo
        // a "gastos" daria a entender que le falta algo que decidio saltarse.
        Step paso = ingresos == 0 ? Step.INCOMES : Step.SUMMARY;

        return new State(
                user.hasCompletedOnboarding(), paso, ingresos, gastos, metas, ingresos > 0);
    }

    /**
     * Termina el alta y abre el primer ciclo.
     *
     * <p>Las dos cosas en una transaccion: una cuenta marcada como configurada
     * pero sin ciclo llegaria al resumen sin nada que mostrar y sin forma de
     * volver al asistente.
     */
    @Transactional
    public BudgetCycle complete(Long userId) {
        User user = requireUser(userId);

        if (user.hasCompletedOnboarding()) {
            throw new BusinessRuleException("Esta cuenta ya termino la configuracion inicial.");
        }
        if (incomes.findByUserIdAndActiveTrueAndDeletedAtIsNull(userId).isEmpty()) {
            throw new BusinessRuleException(
                    "Necesitas al menos un ingreso para calcular tu presupuesto.");
        }

        user.completeOnboarding();
        users.save(user);

        BudgetCycle cycle = cycles.openNextCycle(userId);

        log.info("Alta terminada para el usuario {}: primer ciclo {}", userId, cycle.getPublicId());
        return cycle;
    }

    /**
     * Deja el alta por terminada sin configurar nada.
     *
     * <p>No abre ciclo: no habria con que llenarlo. La persona entra a una
     * aplicacion vacia, que es exactamente lo que pidio, y cada seccion ya sabe
     * explicar que hacer cuando no tiene datos.
     *
     * <p>Lo capturado hasta aqui se conserva: es suyo. Al quedar marcada como
     * terminada, la cuenta tampoco entra en la limpieza de altas abandonadas.
     */
    @Transactional
    public void skip(Long userId) {
        User user = requireUser(userId);

        if (user.hasCompletedOnboarding()) {
            return;
        }

        user.completeOnboarding();
        users.save(user);

        log.info("Alta pospuesta por el usuario {}", userId);
    }

    /** El ciclo en curso, si la cuenta ya termino y lo abrio. */
    @Transactional(readOnly = true)
    public Optional<BudgetCycle> currentCycle(Long userId) {
        return cycles.currentCycle(userId);
    }

    private User requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", String.valueOf(userId)));
    }
}
