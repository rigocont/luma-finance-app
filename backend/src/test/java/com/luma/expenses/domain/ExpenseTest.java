package com.luma.expenses.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.model.Money;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Expense")
class ExpenseTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final Instant AHORA = Instant.parse("2026-09-24T10:00:00Z");

    private static Expense renta() {
        return Expense.create(
                1L, null, "Renta", ExpenseKind.FIXED, Money.of("6000.00"),
                Frequency.MONTHLY, 1, Flexibility.CRITICAL, INICIO);
    }

    private static Expense despensa() {
        return Expense.create(
                1L, null, "Despensa", ExpenseKind.VARIABLE, Money.of("2500.00"),
                Frequency.MONTHLY, 5, Flexibility.IMPORTANT, INICIO);
    }

    @Nested
    @DisplayName("al crearse")
    class Creacion {

        @Test
        void naceActivoYSinBorrar() {
            Expense expense = renta();

            assertThat(expense.isActive()).isTrue();
            assertThat(expense.isDeleted()).isFalse();
            assertThat(expense.getPublicId()).isNotBlank().hasSize(36);
        }

        @Test
        void recortaLosEspaciosDelNombre() {
            Expense expense = Expense.create(
                    1L, null, "  Renta  ", ExpenseKind.FIXED, Money.of("100.00"),
                    Frequency.MONTHLY, 1, Flexibility.CRITICAL, INICIO);

            assertThat(expense.getName()).isEqualTo("Renta");
        }

        @Test
        void rechazaUnNombreVacio() {
            assertThatThrownBy(() -> Expense.create(
                            1L, null, "  ", ExpenseKind.FIXED, Money.of("100.00"),
                            Frequency.MONTHLY, 1, Flexibility.CRITICAL, INICIO))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void unGastoSinCategoriaEsValido() {
            // Exigir categoria siempre solo lograria que la gente eligiera
            // "Otros" para salir del paso, que es peor que no tenerla.
            assertThat(renta().getCategoryId()).isNull();
        }

        @Test
        void rechazaUnaFechaDeFinAnteriorAlInicio() {
            assertThatThrownBy(() -> Expense.create(
                            1L, null, "Renta", ExpenseKind.FIXED, Money.of("100.00"),
                            Frequency.MONTHLY, 1, Flexibility.CRITICAL, INICIO,
                            INICIO.minusDays(1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("la clasificacion")
    class Clasificacion {

        @Test
        void unGastoVariablePideRevision() {
            assertThat(despensa().isVariable()).isTrue();
        }

        @Test
        void unGastoFijoNoLaPide() {
            assertThat(renta().isVariable()).isFalse();
        }

        @Test
        void cambiarDeFijoAVariableCambiaLaRegla() {
            Expense expense = renta();

            expense.changeKind(ExpenseKind.VARIABLE);

            assertThat(expense.isVariable()).isTrue();
        }
    }

    @Nested
    @DisplayName("la flexibilidad")
    class Flexibilidad {

        @Test
        void unGastoCriticoNoSePuedeRetrasar() {
            // Es la propiedad que impide que el modulo de analisis sugiera
            // retrasar la renta. No es un adorno del formulario.
            assertThat(renta().getFlexibility().canBeDeferred()).isFalse();
        }

        @Test
        void losDemasSiSePuedenRetrasar() {
            assertThat(Flexibility.IMPORTANT.canBeDeferred()).isTrue();
            assertThat(Flexibility.FLEXIBLE.canBeDeferred()).isTrue();
        }

        @Test
        void sePuedeCambiar() {
            Expense expense = renta();

            expense.changeFlexibility(Flexibility.FLEXIBLE);

            assertThat(expense.getFlexibility()).isEqualTo(Flexibility.FLEXIBLE);
        }
    }

    @Nested
    @DisplayName("al editarse")
    class Edicion {

        @Test
        void rechazaUnMontoNegativo() {
            assertThatThrownBy(() -> renta().changeAmount(Money.of("-1.00")))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void quitarLaCategoriaEsValido() {
            Expense expense = renta();

            expense.changeCategory(7L);
            expense.changeCategory(null);

            assertThat(expense.getCategoryId()).isNull();
        }

        @Test
        void unCalendarioInvalidoNoDejaElGastoAMedias() {
            Expense expense = renta();

            assertThatThrownBy(() -> expense.changeSchedule(
                            Frequency.BIWEEKLY, 5, INICIO, INICIO.minusDays(1)))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(expense.getFrequency()).isEqualTo(Frequency.MONTHLY);
            assertThat(expense.getDueDay()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("al activarse, desactivarse y eliminarse")
    class CicloDeVida {

        @Test
        void desactivarLoSacaDelPresupuestoSinBorrarlo() {
            Expense expense = renta();

            expense.deactivate();

            assertThat(expense.isActive()).isFalse();
            assertThat(expense.isDeleted()).isFalse();
        }

        @Test
        void eliminarEsLogicoYTambienLoDesactiva() {
            Expense expense = renta();

            expense.softDelete(AHORA);

            assertThat(expense.isDeleted()).isTrue();
            assertThat(expense.getDeletedAt()).isEqualTo(AHORA);
            assertThat(expense.isActive()).isFalse();
        }

        @Test
        void unGastoEliminadoNoSePuedeReactivar() {
            Expense expense = renta();
            expense.softDelete(AHORA);

            assertThatThrownBy(expense::activate)
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("eliminado");
        }
    }
}
