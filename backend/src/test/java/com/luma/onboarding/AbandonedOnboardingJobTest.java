package com.luma.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.common.model.Money;
import com.luma.expenses.application.ExpenseService;
import com.luma.expenses.domain.ExpenseKind;
import com.luma.expenses.infrastructure.ExpenseRepository;
import com.luma.income.application.IncomeService;
import com.luma.income.domain.IncomeType;
import com.luma.income.infrastructure.IncomeRepository;
import com.luma.onboarding.application.AbandonedOnboardingJob;
import com.luma.onboarding.application.OnboardingService;
import com.luma.savings.application.SavingsGoalService;
import com.luma.savings.domain.ContributionMode;
import com.luma.savings.infrastructure.SavingsGoalRepository;
import com.luma.support.IntegrationTest;
import com.luma.users.application.UserPreferencesService;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * La limpieza de altas abandonadas.
 *
 * <p>El trabajo se construye a mano con un reloj adelantado en lugar de esperar
 * al cron. No es un atajo: lo que hay que demostrar es COMO decide, y para eso
 * el reloj tiene que ser un dato de la prueba.
 *
 * <p>Antes de cada ejecucion se vacia el contexto de persistencia. Es
 * deliberado: {@code created_at} lo pone la base con su propio DEFAULT y la
 * columna esta mapeada como no insertable, asi que una entidad recien guardada
 * NO lo trae en memoria. Sin el vaciado, la prueba verificaria un caso que en
 * produccion no ocurre, porque ahi el trabajo siempre lee filas frescas.
 */
@Transactional
@DisplayName("La limpieza de altas abandonadas")
class AbandonedOnboardingJobTest extends IntegrationTest {

    private static final Duration PLAZO = Duration.ofDays(7);

    @Autowired
    UserRepository users;

    @Autowired
    IncomeService incomes;

    @Autowired
    ExpenseService expenses;

    @Autowired
    SavingsGoalService savings;

    @Autowired
    IncomeRepository incomeRepository;

    @Autowired
    ExpenseRepository expenseRepository;

    @Autowired
    SavingsGoalRepository goalRepository;

    @Autowired
    UserPreferencesService preferences;

    @Autowired
    OnboardingService onboarding;

    @PersistenceContext
    EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void crearUsuario() {
        User user = users.save(User.register(
                "qa+" + UUID.randomUUID() + "@luma.app", "Persona de prueba", "hash-irrelevante"));
        userId = user.getId();
    }

    private static LocalDate inicioLejano() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusYears(1);
    }

    private void capturarDeTodo() {
        incomes.create(
                userId, "Sueldo", IncomeType.RECURRENT, Money.of("18000.00"),
                Frequency.BIWEEKLY, 10, inicioLejano(), null, null);

        expenses.create(
                userId, null, "Renta", ExpenseKind.FIXED, Money.of("6000.00"),
                Frequency.BIWEEKLY, 10, Flexibility.CRITICAL, inicioLejano(), null, null);

        savings.create(
                userId, "Fondo", Money.of("30000.00"), null,
                ContributionMode.FIXED_PER_CYCLE, Money.of("1000.00"), null, null);
    }

    /** El trabajo, con el reloj corrido los dias que se le indiquen. */
    private AbandonedOnboardingJob trabajoEn(long diasAdelante, Duration plazo) {
        Clock reloj = Clock.fixed(
                Instant.now().plus(Duration.ofDays(diasAdelante)), ZoneOffset.UTC);

        return new AbandonedOnboardingJob(
                users, incomeRepository, expenseRepository, goalRepository,
                preferences, reloj, plazo);
    }

    /**
     * Vacia el contexto para que el trabajo lea de la base, como en produccion.
     * Sin esto, {@code created_at} llegaria nulo.
     */
    private void comoEnProduccion() {
        entityManager.flush();
        entityManager.clear();
    }

    private long ingresosVivos() {
        return incomeRepository.findByUserIdAndDeletedAtIsNull(userId).size();
    }

    @Test
    @DisplayName("invalida lo capturado en un alta que nadie termino")
    void invalidaLoAbandonado() {
        capturarDeTodo();
        comoEnProduccion();

        int limpiadas = trabajoEn(30, PLAZO).run();

        assertThat(limpiadas).isEqualTo(1);
        assertThat(incomeRepository.findByUserIdAndDeletedAtIsNull(userId)).isEmpty();
        assertThat(expenseRepository.findByUserIdAndDeletedAtIsNull(userId)).isEmpty();
        assertThat(goalRepository.findByUserIdAndDeletedAtIsNullOrderByPriorityAsc(userId))
                .isEmpty();
    }

    @Test
    @DisplayName("invalida, no borra: las filas siguen existiendo")
    void invalidaSinBorrar() {
        // Un DELETE de verdad dejaria sin forma de explicar que paso con lo
        // capturado si alguien reclama.
        capturarDeTodo();
        comoEnProduccion();

        trabajoEn(30, PLAZO).run();

        assertThat(incomeRepository.count()).isPositive();
        assertThat(users.findById(userId)).isPresent();
    }

    @Test
    @DisplayName("NO toca a quien capturo algo hace poco")
    void respetaLaActividadReciente() {
        capturarDeTodo();
        comoEnProduccion();

        // Un dia despues, con plazo de siete: sigue dentro.
        int limpiadas = trabajoEn(1, PLAZO).run();

        assertThat(limpiadas).isZero();
        assertThat(ingresosVivos()).isEqualTo(1);
    }

    @Test
    @DisplayName("NO toca una cuenta que ya termino el alta")
    void respetaLasCuentasConfiguradas() {
        capturarDeTodo();
        onboarding.complete(userId);
        comoEnProduccion();

        int limpiadas = trabajoEn(365, PLAZO).run();

        assertThat(limpiadas).isZero();
        assertThat(ingresosVivos()).isEqualTo(1);
    }

    @Test
    @DisplayName("NO toca una cuenta que pospuso el alta")
    void respetaLasCuentasPospuestas() {
        // "Hacerlo despues" marca la cuenta como terminada, asi que lo que haya
        // capturado es suyo y se queda.
        capturarDeTodo();
        onboarding.skip(userId);
        comoEnProduccion();

        assertThat(trabajoEn(365, PLAZO).run()).isZero();
        assertThat(ingresosVivos()).isEqualTo(1);
    }

    @Test
    @DisplayName("con el plazo en cero la limpieza esta apagada")
    void sePuedeApagar() {
        capturarDeTodo();
        comoEnProduccion();

        assertThat(trabajoEn(365, Duration.ZERO).run()).isZero();
        assertThat(ingresosVivos()).isEqualTo(1);
    }

    @Test
    @DisplayName("una cuenta sin nada capturado no cuenta como limpiada")
    void cuentaVacia() {
        comoEnProduccion();

        assertThat(trabajoEn(365, PLAZO).run()).isZero();
    }

    @Test
    @DisplayName("despues de limpiar, el asistente empieza de nuevo")
    void elAsistenteEmpiezaDeNuevo() {
        capturarDeTodo();
        comoEnProduccion();

        trabajoEn(30, PLAZO).run();

        OnboardingService.State estado = onboarding.stateOf(userId);
        assertThat(estado.incomeCount()).isZero();
        assertThat(estado.canFinish()).isFalse();
        assertThat(estado.resumeStep()).isEqualTo(OnboardingService.Step.INCOMES);
    }
}
