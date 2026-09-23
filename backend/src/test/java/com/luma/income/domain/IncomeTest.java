package com.luma.income.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.budget.domain.Frequency;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.model.Money;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("Income")
class IncomeTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final Instant AHORA = Instant.parse("2026-09-23T10:00:00Z");

    private static Income sueldo() {
        return Income.create(
                1L,
                "Sueldo",
                IncomeType.RECURRENT,
                Money.of("12500.00"),
                Frequency.BIWEEKLY,
                15,
                INICIO);
    }

    @Nested
    @DisplayName("al crearse")
    class Creacion {

        @Test
        void naceActivoYConIdentificadorPublico() {
            Income income = sueldo();

            assertThat(income.isActive()).isTrue();
            assertThat(income.isDeleted()).isFalse();
            assertThat(income.getPublicId()).isNotBlank().hasSize(36);
        }

        @Test
        void recortaLosEspaciosDelNombre() {
            Income income = Income.create(
                    1L, "  Sueldo  ", IncomeType.RECURRENT, Money.of("100.00"),
                    Frequency.MONTHLY, 1, INICIO);

            assertThat(income.getName()).isEqualTo("Sueldo");
        }

        @Test
        void rechazaUnNombreVacio() {
            assertThatThrownBy(() -> Income.create(
                            1L, "   ", IncomeType.RECURRENT, Money.of("100.00"),
                            Frequency.MONTHLY, 1, INICIO))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("nombre");
        }

        @Test
        void rechazaUnDiaFueraDeRango() {
            assertThatThrownBy(() -> Income.create(
                            1L, "Sueldo", IncomeType.RECURRENT, Money.of("100.00"),
                            Frequency.MONTHLY, 32, INICIO))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rechazaUnaFechaDeFinAnteriorAlInicio() {
            // La validacion la hace RecurrenceSchedule: una sola regla, un solo
            // lugar, aunque la use mas de una entidad.
            assertThatThrownBy(() -> Income.create(
                            1L, "Bono", IncomeType.BONUS, Money.of("100.00"),
                            Frequency.ONE_TIME, null, INICIO, INICIO.minusDays(1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("la regla de revision")
    class Revision {

        @ParameterizedTest
        @EnumSource(value = IncomeType.class, names = {"VARIABLE", "SALE"})
        void losIngresosDeMontoIncierroPidenRevision(IncomeType tipo) {
            Income income = Income.create(
                    1L, "Comisiones", tipo, Money.of("4000.00"),
                    Frequency.MONTHLY, 15, INICIO);

            assertThat(income.requiresReview()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(
                value = IncomeType.class,
                names = {"RECURRENT", "BONUS", "AGUINALDO", "OTHER"})
        void losDemasNoLaPiden(IncomeType tipo) {
            Income income = Income.create(
                    1L, "Sueldo", tipo, Money.of("12500.00"),
                    Frequency.MONTHLY, 15, INICIO);

            assertThat(income.requiresReview()).isFalse();
        }
    }

    @Nested
    @DisplayName("al editarse")
    class Edicion {

        @Test
        void cambiarElMontoLoActualiza() {
            Income income = sueldo();

            income.changeAmount(Money.of("13000.00"));

            assertThat(income.getAmount()).isEqualByComparingTo("13000.00");
        }

        @Test
        void rechazaUnMontoNegativo() {
            assertThatThrownBy(() -> sueldo().changeAmount(Money.of("-1.00")))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void permiteMontoCero() {
            // Un ingreso en cero es raro pero valido: sirve para dejar capturada
            // una entrada que este ciclo no llego.
            assertThatCode(() -> sueldo().changeAmount(Money.of("0.00")))
                    .doesNotThrowAnyException();
        }

        @Test
        void cambiarElCalendarioValidaAntesDeAplicar() {
            Income income = sueldo();

            assertThatThrownBy(() -> income.changeSchedule(
                            Frequency.MONTHLY, 5, INICIO, INICIO.minusDays(1)))
                    .isInstanceOf(IllegalArgumentException.class);

            // Y no dejo la entidad a medias.
            assertThat(income.getFrequency()).isEqualTo(Frequency.BIWEEKLY);
            assertThat(income.getExpectedDay()).isEqualTo(15);
        }

        @Test
        void cambiarElCalendarioAplicaLosCuatroCampos() {
            Income income = sueldo();
            LocalDate fin = LocalDate.of(2026, 12, 31);

            income.changeSchedule(Frequency.MONTHLY, 5, INICIO, fin);

            assertThat(income.getFrequency()).isEqualTo(Frequency.MONTHLY);
            assertThat(income.getExpectedDay()).isEqualTo(5);
            assertThat(income.getEndDate()).isEqualTo(fin);
        }
    }

    @Nested
    @DisplayName("al activarse, desactivarse y eliminarse")
    class CicloDeVida {

        @Test
        void desactivarLoSacaDelPresupuestoSinBorrarlo() {
            Income income = sueldo();

            income.deactivate();

            assertThat(income.isActive()).isFalse();
            assertThat(income.isDeleted()).isFalse();
        }

        @Test
        void unIngresoDesactivadoSePuedeReactivar() {
            Income income = sueldo();
            income.deactivate();

            income.activate();

            assertThat(income.isActive()).isTrue();
        }

        @Test
        void eliminarEsLogicoYTambienLoDesactiva() {
            Income income = sueldo();

            income.softDelete(AHORA);

            assertThat(income.isDeleted()).isTrue();
            assertThat(income.getDeletedAt()).isEqualTo(AHORA);
            assertThat(income.isActive()).isFalse();
        }

        @Test
        void unIngresoEliminadoNoSePuedeReactivar() {
            // Si se pudiera, el borrado no significaria nada.
            Income income = sueldo();
            income.softDelete(AHORA);

            assertThatThrownBy(income::activate)
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("eliminado");
        }
    }
}
