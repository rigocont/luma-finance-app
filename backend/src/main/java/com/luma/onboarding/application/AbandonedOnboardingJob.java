package com.luma.onboarding.application;

import com.luma.expenses.domain.Expense;
import com.luma.expenses.infrastructure.ExpenseRepository;
import com.luma.income.domain.Income;
import com.luma.income.infrastructure.IncomeRepository;
import com.luma.savings.domain.SavingsGoal;
import com.luma.savings.infrastructure.SavingsGoalRepository;
import com.luma.users.application.UserPreferencesService;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invalida lo capturado en un alta que nadie termino.
 *
 * <p><b>Por que se puede hacer sin una columna nueva.</b> El asistente es
 * obligatorio: mientras {@code onboarding_completed_at} sea nulo, ninguna otra
 * pantalla de la aplicacion es alcanzable. De ahi se sigue que TODO lo que tiene
 * una cuenta en ese estado se capturo en el asistente. La marca que haria falta
 * para distinguir "esto vino del alta" de "esto lo hizo despues" no existe
 * porque no hay un "despues" posible. La ausencia de esa fecha es la marca.
 *
 * <p><b>Invalida, no borra.</b> Se usa el borrado logico que ya tienen ingresos,
 * gastos y metas. Un {@code DELETE} de verdad deja a quien vuelve sin forma de
 * entender que paso con lo que habia capturado, y a nosotros sin forma de
 * revisarlo si alguien reclama. Las filas dejan de contar en cualquier consulta
 * del producto, que es lo que importaba.
 *
 * <p><b>Que NO se toca.</b> La cuenta sigue existiendo y la persona puede volver
 * a entrar: lo que se limpia son sus datos financieros, no su acceso. Tampoco
 * hay ciclos que limpiar —el primero se abre justo al terminar el alta, asi que
 * una cuenta abandonada no tiene ninguno.
 *
 * <p>Se puede apagar poniendo el plazo en cero.
 */
@Component
public class AbandonedOnboardingJob {

    private static final Logger log = LoggerFactory.getLogger(AbandonedOnboardingJob.class);

    private final UserRepository users;
    private final IncomeRepository incomes;
    private final ExpenseRepository expenses;
    private final SavingsGoalRepository goals;
    private final UserPreferencesService preferences;
    private final Clock clock;
    private final Duration plazo;

    public AbandonedOnboardingJob(
            UserRepository users,
            IncomeRepository incomes,
            ExpenseRepository expenses,
            SavingsGoalRepository goals,
            UserPreferencesService preferences,
            Clock clock,
            @Value("${luma.onboarding.abandon-after}") Duration plazo) {
        this.users = users;
        this.incomes = incomes;
        this.expenses = expenses;
        this.goals = goals;
        this.preferences = preferences;
        this.clock = clock;
        this.plazo = plazo;
    }

    @Scheduled(cron = "0 45 3 * * *")
    public void limpiarAltasAbandonadas() {
        int limpiadas = run();

        if (limpiadas > 0) {
            log.info("Altas abandonadas limpiadas: {}", limpiadas);
        }
    }

    /**
     * Expuesto aparte del metodo programado para poder ejercitarlo sin esperar
     * al cron. Devuelve cuantas cuentas se limpiaron.
     */
    @Transactional
    public int run() {
        if (plazo.isZero() || plazo.isNegative()) {
            return 0;
        }

        Instant corte = clock.instant().minus(plazo);
        List<User> candidatos = users.findAbandonedOnboarding(corte);
        int limpiadas = 0;

        for (User candidato : candidatos) {
            if (limpiar(candidato, corte)) {
                limpiadas++;
            }
        }

        return limpiadas;
    }

    /**
     * Limpia una cuenta si de verdad esta abandonada.
     *
     * <p>La consulta ya filtro por la fecha de registro, pero eso no basta:
     * alguien registrado hace un mes pudo empezar a capturar hoy. Lo que decide
     * es la ULTIMA actividad, y esa se deduce de lo mas reciente que capturo.
     */
    private boolean limpiar(User user, Instant corte) {
        Long userId = user.getId();

        List<Income> susIngresos = incomes.findByUserIdAndDeletedAtIsNull(userId);
        List<Expense> susGastos = expenses.findByUserIdAndDeletedAtIsNull(userId);
        List<SavingsGoal> susMetas = goals.findByUserIdAndDeletedAtIsNullOrderByPriorityAsc(userId);

        if (susIngresos.isEmpty() && susGastos.isEmpty() && susMetas.isEmpty()) {
            // Nada que invalidar. Se deja en paz: la cuenta vacia no es un
            // riesgo y borrarla es una decision distinta, de retencion, que no
            // le toca a este trabajo.
            return false;
        }

        Instant ultimaActividad = ultimaDe(susIngresos, susGastos, susMetas);

        if (ultimaActividad == null || ultimaActividad.isAfter(corte)) {
            // Sigue en ello: puede haberse registrado hace semanas y estar
            // capturando ahora mismo.
            //
            // El caso nulo tambien se deja en paz, y no es un detalle. La base
            // pone created_at con su propio DEFAULT y la columna esta mapeada
            // como no insertable, asi que una fila recien guardada puede no
            // traer todavia ese valor en memoria. Ante la duda de CUANDO se
            // capturo algo, este trabajo no lo toca: equivocarse hacia el lado
            // de invalidar datos de alguien que sigue trabajando es mucho peor
            // que dejar una cuenta sin limpiar un dia mas.
            return false;
        }

        Instant ahora = clock.instant();
        susIngresos.forEach(income -> income.softDelete(ahora));
        susGastos.forEach(expense -> expense.softDelete(ahora));
        susMetas.forEach(goal -> goal.softDelete(ahora));

        incomes.saveAll(susIngresos);
        expenses.saveAll(susGastos);
        goals.saveAll(susMetas);

        // El ciclo elegido tambien se reinicia: si vuelve, el asistente empieza
        // igual que la primera vez en lugar de con una eleccion a medias de la
        // que ya no queda nada.
        preferences.resetCycleToDefaults(userId);

        log.info(
                "Alta abandonada del usuario {}: invalidados {} ingreso(s), {} gasto(s) "
                        + "y {} meta(s)",
                userId,
                susIngresos.size(),
                susGastos.size(),
                susMetas.size());

        return true;
    }

    private static Instant ultimaDe(
            List<Income> ingresos, List<Expense> gastos, List<SavingsGoal> metas) {

        Instant ultima = null;

        for (Income income : ingresos) {
            ultima = masReciente(ultima, income.getCreatedAt());
        }
        for (Expense expense : gastos) {
            ultima = masReciente(ultima, expense.getCreatedAt());
        }
        for (SavingsGoal goal : metas) {
            ultima = masReciente(ultima, goal.getCreatedAt());
        }

        return ultima;
    }

    private static Instant masReciente(Instant actual, Instant candidata) {
        if (candidata == null) {
            return actual;
        }
        return actual == null || candidata.isAfter(actual) ? candidata : actual;
    }
}
