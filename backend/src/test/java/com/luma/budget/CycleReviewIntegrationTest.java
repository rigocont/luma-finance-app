package com.luma.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.application.CycleReviewService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.BudgetPeriod;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.budget.domain.ItemHistoryEntry;
import com.luma.budget.domain.ItemSource;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.expenses.application.ExpenseService;
import com.luma.expenses.domain.Expense;
import com.luma.expenses.domain.ExpenseKind;
import com.luma.support.IntegrationTest;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * La revision del ciclo contra la base real.
 *
 * <p>Lo que se prueba aqui cruza ciclos, y eso no se puede demostrar con
 * objetos en memoria: la sugerencia sale de una consulta que une el renglon de
 * hoy con el mismo gasto en el ciclo pasado.
 *
 * <p>Los ciclos anteriores se construyen a mano con las fabricas del dominio.
 * No es hacer trampa: {@code openNextCycle} usa el reloj del sistema y se niega
 * —con razon— a abrir dos ciclos el mismo dia. Lo que esta bajo prueba es
 * {@link CycleReviewService}, no como se abre un ciclo.
 */
@Transactional
@DisplayName("La revision del ciclo contra la base real")
class CycleReviewIntegrationTest extends IntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    ExpenseService expenses;

    @Autowired
    BudgetCycleService cycles;

    @Autowired
    CycleReviewService review;

    @Autowired
    BudgetCycleRepository cycleRepository;

    @Autowired
    CycleItemRepository items;

    /**
     * Un dia que cae en cualquier periodo quincenal. Con frecuencia quincenal el
     * gasto ocurre una vez por ciclo pase lo que pase, asi que la prueba no
     * depende del dia del mes en que se ejecute.
     */
    private static final int DIA_SEGURO = 10;

    private Long userId;

    private static LocalDate inicioLejano() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusYears(1);
    }

    @BeforeEach
    void crearUsuario() {
        User user = users.save(User.register(
                "qa+" + UUID.randomUUID() + "@luma.app", "Persona de prueba", "hash-irrelevante"));
        userId = user.getId();
    }

    private Expense gastoVariable(String nombre, String estimado) {
        return expenses.create(
                userId, null, nombre, ExpenseKind.VARIABLE, Money.of(estimado),
                Frequency.BIWEEKLY, DIA_SEGURO, Flexibility.IMPORTANT,
                inicioLejano(), null, null);
    }

    /** Un ciclo ya terminado, con un renglon de ese gasto confirmado en cierto monto. */
    private BudgetCycle cicloAnteriorCon(
            BudgetCycle referencia, Expense gasto, String planeado, String real, int atras) {

        LocalDate inicio = referencia.getStartDate().minusMonths(atras);
        BudgetPeriod periodo = BudgetPeriod.of(inicio, inicio.plusDays(27));

        BudgetCycle nuevo = BudgetCycle.open(
                userId, referencia.getCycleType(), periodo, referencia.getSequenceNumber() - atras);
        // Cerrado, como corresponde a un ciclo que ya paso: dejar dos ciclos
        // activos a la vez seria un estado que la aplicacion nunca produce.
        nuevo.close();
        BudgetCycle anterior = cycleRepository.save(nuevo);

        CycleItem renglon = CycleItem.materialize(
                anterior.getId(),
                CycleItemType.VARIABLE_EXPENSE,
                ItemSource.EXPENSE,
                gasto.getId(),
                gasto.getName(),
                null,
                Money.of(planeado),
                periodo.end(),
                ItemStatus.NEEDS_REVIEW,
                Flexibility.IMPORTANT,
                0);

        if (real != null) {
            renglon.settle(Money.of(real), periodo.end(), Instant.now());
        }
        items.save(renglon);

        return anterior;
    }

    private CycleItem renglonEnRevision(BudgetCycle cycle) {
        return items.findByBudgetCycleIdAndStatusOrderByDueDateAscIdAsc(
                        cycle.getId(), ItemStatus.NEEDS_REVIEW)
                .getFirst();
    }

    @Nested
    @DisplayName("que pide revision")
    class LoQuePideRevision {

        @Test
        @DisplayName("un gasto variable nace pidiendo revision y uno fijo no")
        void soloLosVariables() {
            gastoVariable("Despensa", "3000.00");
            expenses.create(
                    userId, null, "Renta", ExpenseKind.FIXED, Money.of("9000.00"),
                    Frequency.BIWEEKLY, DIA_SEGURO, Flexibility.CRITICAL,
                    inicioLejano(), null, null);

            BudgetCycle cycle = cycles.openNextCycle(userId);

            assertThat(review.pendingReview(cycle))
                    .extracting(item -> item.item().getName())
                    .containsExactly("Despensa");
        }

        @Test
        @DisplayName("un ciclo sin nada por revisar devuelve una lista vacia")
        void nadaQueRevisar() {
            BudgetCycle cycle = cycles.openNextCycle(userId);

            assertThat(review.pendingReview(cycle)).isEmpty();
        }
    }

    @Nested
    @DisplayName("la sugerencia de monto")
    class Sugerencia {

        @Test
        @DisplayName("propone lo que se confirmo en el ciclo anterior")
        void proponeElMontoDelCicloPasado() {
            Expense gasto = gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            cicloAnteriorCon(actual, gasto, "800.00", "1150.00", 1);

            CycleReviewService.ReviewItem revision = review.pendingReview(actual).getFirst();

            assertThat(revision.suggestion()).isEqualByComparingTo("1150.00");
            assertThat(revision.suggestedFrom()).isNotNull();
        }

        @Test
        @DisplayName("sin ciclo anterior no hay sugerencia")
        void sinCicloAnterior() {
            gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);

            assertThat(review.pendingReview(actual).getFirst().suggestion()).isNull();
        }

        @Test
        @DisplayName("un monto que nadie confirmo NO se sugiere")
        void noSugiereUnPlanSinConfirmar() {
            // Sugerir el planeado propagaria la misma estimacion de ciclo en
            // ciclo y la haria parecer un dato cuando nunca lo fue.
            Expense gasto = gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            cicloAnteriorCon(actual, gasto, "800.00", null, 1);

            assertThat(review.pendingReview(actual).getFirst().suggestion()).isNull();
        }

        @Test
        @DisplayName("con varios ciclos atras gana el mas reciente")
        void ganaElMasReciente() {
            Expense gasto = gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            cicloAnteriorCon(actual, gasto, "800.00", "600.00", 3);
            cicloAnteriorCon(actual, gasto, "800.00", "1150.00", 1);

            assertThat(review.pendingReview(actual).getFirst().suggestion())
                    .isEqualByComparingTo("1150.00");
        }
    }

    @Nested
    @DisplayName("el historial de un renglon")
    class Historial {

        @Test
        @DisplayName("trae los ciclos anteriores del mas reciente al mas antiguo")
        void delMasRecienteAlMasAntiguo() {
            Expense gasto = gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            cicloAnteriorCon(actual, gasto, "800.00", "600.00", 3);
            cicloAnteriorCon(actual, gasto, "800.00", "900.00", 2);
            cicloAnteriorCon(actual, gasto, "800.00", "1150.00", 1);

            List<ItemHistoryEntry> historial =
                    review.historyOf(actual, renglonEnRevision(actual).getPublicId());

            assertThat(historial)
                    .extracting(ItemHistoryEntry::actualAmount)
                    .containsExactly(
                            new BigDecimal("1150.00"),
                            new BigDecimal("900.00"),
                            new BigDecimal("600.00"));
        }

        @Test
        @DisplayName("no incluye el ciclo en curso")
        void sinElCicloEnCurso() {
            Expense gasto = gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            cicloAnteriorCon(actual, gasto, "800.00", "1150.00", 1);

            CycleItem renglon = renglonEnRevision(actual);
            // Se REGISTRA el pago, no solo se fija el monto: asi el renglon de
            // hoy tiene monto real y la prueba demuestra que queda fuera porque
            // es de este ciclo, no porque le falte el dato.
            cycles.settleItem(actual, renglon.getPublicId(), Money.of("999.00"), null, false);

            assertThat(review.historyOf(actual, renglon.getPublicId()))
                    .extracting(ItemHistoryEntry::actualAmount)
                    .containsExactly(new BigDecimal("1150.00"));
        }

        @Test
        @DisplayName("la primera vez que aparece un gasto, el historial viene vacio")
        void primeraVez() {
            gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);

            assertThat(review.historyOf(actual, renglonEnRevision(actual).getPublicId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("un renglon que no es de este ciclo no se encuentra")
        void renglonAjeno() {
            gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);

            assertThatThrownBy(() -> review.historyOf(actual, UUID.randomUUID().toString()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("el ciclo de otra persona no se puede pedir")
        void aislamiento() {
            gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);

            User otro = users.save(User.register(
                    "qa+" + UUID.randomUUID() + "@luma.app", "Otra persona", "hash"));

            assertThatThrownBy(() -> cycles.requireCycle(actual.getPublicId(), otro.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("confirmar montos en lote")
    class ConfirmarEnLote {

        @Test
        @DisplayName("fija el monto y saca el renglon de revision")
        void fijaElMonto() {
            gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            CycleItem renglon = renglonEnRevision(actual);

            List<CycleItem> confirmados = review.confirmAmounts(
                    actual, Map.of(renglon.getPublicId(), Money.of("1150.00")));

            assertThat(confirmados).hasSize(1);
            assertThat(confirmados.getFirst().getPlannedAmount()).isEqualByComparingTo("1150.00");
            assertThat(confirmados.getFirst().getStatus()).isEqualTo(ItemStatus.PENDING);
        }

        @Test
        @DisplayName("NO marca el renglon como pagado")
        void noLoMarcaPagado() {
            // Fijar cuanto es y haber pagado son dos hechos distintos. Juntarlos
            // haria que el balance diera por pagado lo que nadie pago.
            gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            CycleItem renglon = renglonEnRevision(actual);

            CycleItem confirmado = review.confirmAmounts(
                            actual, Map.of(renglon.getPublicId(), Money.of("1150.00")))
                    .getFirst();

            assertThat(confirmado.getActualAmount()).isNull();
            assertThat(confirmado.getSettledOn()).isNull();
        }

        @Test
        @DisplayName("un renglon inexistente cancela el lote entero")
        void todoONada() {
            gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            CycleItem renglon = renglonEnRevision(actual);

            assertThatThrownBy(() -> review.confirmAmounts(
                            actual,
                            Map.of(
                                    renglon.getPublicId(), Money.of("1150.00"),
                                    "no-existe", Money.of("50.00"))))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("un lote vacio se rechaza")
        void loteVacio() {
            BudgetCycle actual = cycles.openNextCycle(userId);

            assertThatThrownBy(() -> review.confirmAmounts(actual, Map.of()))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("no se puede confirmar en un ciclo cerrado")
        void cicloCerrado() {
            gastoVariable("Luz", "800.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            CycleItem renglon = renglonEnRevision(actual);

            cycles.closeCycle(actual);

            assertThatThrownBy(() -> review.confirmAmounts(
                            actual, Map.of(renglon.getPublicId(), Money.of("1150.00"))))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }
}
