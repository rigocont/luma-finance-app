package com.luma.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("CyclePlanner")
class CyclePlannerTest {

    @Nested
    @DisplayName("quincenal")
    class Biweekly {

        private final CyclePlanner planner = CyclePlanner.of(CycleType.BIWEEKLY);

        @ParameterizedTest
        @CsvSource({
            "2026-09-01, 2026-09-01, 2026-09-15",
            "2026-09-13, 2026-09-01, 2026-09-15",
            "2026-09-15, 2026-09-01, 2026-09-15",
            "2026-09-16, 2026-09-16, 2026-09-30",
            "2026-09-30, 2026-09-16, 2026-09-30",
        })
        void partirElMesEnDosMitades(String date, String expectedStart, String expectedEnd) {
            BudgetPeriod period = planner.periodContaining(LocalDate.parse(date));

            assertThat(period.start()).isEqualTo(LocalDate.parse(expectedStart));
            assertThat(period.end()).isEqualTo(LocalDate.parse(expectedEnd));
        }

        @Test
        void laSegundaQuincenaDeFebreroTerminaEl28EnAnoComun() {
            BudgetPeriod period = planner.periodContaining(LocalDate.parse("2026-02-20"));

            assertThat(period.end()).isEqualTo(LocalDate.parse("2026-02-28"));
        }

        @Test
        void laSegundaQuincenaDeFebreroTerminaEl29EnAnoBisiesto() {
            BudgetPeriod period = planner.periodContaining(LocalDate.parse("2024-02-20"));

            assertThat(period.end()).isEqualTo(LocalDate.parse("2024-02-29"));
        }

        @Test
        void elSiguienteDeLaSegundaQuincenaEsLaPrimeraDelMesEntrante() {
            BudgetPeriod segunda = planner.periodContaining(LocalDate.parse("2026-09-20"));

            BudgetPeriod siguiente = planner.next(segunda);

            assertThat(siguiente.start()).isEqualTo(LocalDate.parse("2026-10-01"));
            assertThat(siguiente.end()).isEqualTo(LocalDate.parse("2026-10-15"));
        }

        @Test
        void cruzaElFinDeAno() {
            BudgetPeriod ultima = planner.periodContaining(LocalDate.parse("2026-12-20"));

            BudgetPeriod siguiente = planner.next(ultima);

            assertThat(siguiente.start()).isEqualTo(LocalDate.parse("2027-01-01"));
        }
    }

    @Nested
    @DisplayName("mensual")
    class Monthly {

        @Test
        void conAnclajeEnUnoEsElMesNatural() {
            BudgetPeriod period =
                    CyclePlanner.of(CycleType.MONTHLY).periodContaining(LocalDate.parse("2026-09-13"));

            assertThat(period.start()).isEqualTo(LocalDate.parse("2026-09-01"));
            assertThat(period.end()).isEqualTo(LocalDate.parse("2026-09-30"));
        }

        @Test
        void conAnclajeEn25EmpiezaElDiaDePago() {
            CyclePlanner planner = CyclePlanner.of(CycleType.MONTHLY, 25);

            BudgetPeriod antes = planner.periodContaining(LocalDate.parse("2026-09-13"));
            BudgetPeriod desde = planner.periodContaining(LocalDate.parse("2026-09-25"));

            assertThat(antes.start()).isEqualTo(LocalDate.parse("2026-08-25"));
            assertThat(antes.end()).isEqualTo(LocalDate.parse("2026-09-24"));
            assertThat(desde.start()).isEqualTo(LocalDate.parse("2026-09-25"));
            assertThat(desde.end()).isEqualTo(LocalDate.parse("2026-10-24"));
        }

        @Test
        void conAnclajeEn31SeRecortaEnFebrero() {
            CyclePlanner planner = CyclePlanner.of(CycleType.MONTHLY, 31);

            BudgetPeriod period = planner.periodContaining(LocalDate.parse("2026-02-10"));

            assertThat(period.start()).isEqualTo(LocalDate.parse("2026-01-31"));
            assertThat(period.end()).isEqualTo(LocalDate.parse("2026-02-27"));
        }

        @Test
        void conAnclajeEn31LosPeriodosSiguenSiendoContiguos() {
            CyclePlanner planner = CyclePlanner.of(CycleType.MONTHLY, 31);
            BudgetPeriod period = planner.periodContaining(LocalDate.parse("2026-01-15"));

            // Doce meses seguidos, incluido el recorte de febrero, sin huecos ni
            // traslapes. Es el caso que mas facil se rompe al tocar estas fechas.
            for (int i = 0; i < 12; i++) {
                BudgetPeriod siguiente = planner.next(period);

                assertThat(siguiente.start())
                        .as("el periodo %s debe empezar justo despues de %s", siguiente, period)
                        .isEqualTo(period.end().plusDays(1));

                period = siguiente;
            }
        }

        @Test
        void elAnteriorDevuelveElPeriodoPrevio() {
            CyclePlanner planner = CyclePlanner.of(CycleType.MONTHLY);
            BudgetPeriod septiembre = planner.periodContaining(LocalDate.parse("2026-09-13"));

            BudgetPeriod agosto = planner.previous(septiembre);

            assertThat(agosto.start()).isEqualTo(LocalDate.parse("2026-08-01"));
            assertThat(agosto.end()).isEqualTo(LocalDate.parse("2026-08-31"));
        }
    }

    @Nested
    @DisplayName("bimestral")
    class Bimonthly {

        private final CyclePlanner planner = CyclePlanner.of(CycleType.BIMONTHLY);

        @ParameterizedTest
        @CsvSource({
            "2026-01-05, 2026-01-01, 2026-02-28",
            "2026-02-20, 2026-01-01, 2026-02-28",
            "2026-09-13, 2026-09-01, 2026-10-31",
            "2026-10-31, 2026-09-01, 2026-10-31",
            "2026-11-01, 2026-11-01, 2026-12-31",
        })
        void seAlineaEnMesesImpares(String date, String expectedStart, String expectedEnd) {
            BudgetPeriod period = planner.periodContaining(LocalDate.parse(date));

            assertThat(period.start()).isEqualTo(LocalDate.parse(expectedStart));
            assertThat(period.end()).isEqualTo(LocalDate.parse(expectedEnd));
        }

        @Test
        void conAnclajeEn15PuedeEmpezarElAnoAnterior() {
            CyclePlanner anclado = CyclePlanner.of(CycleType.BIMONTHLY, 15);

            BudgetPeriod period = anclado.periodContaining(LocalDate.parse("2026-01-10"));

            assertThat(period.start()).isEqualTo(LocalDate.parse("2025-11-15"));
            assertThat(period.end()).isEqualTo(LocalDate.parse("2026-01-14"));
        }

        @Test
        void losPeriodosSonContiguos() {
            BudgetPeriod period = planner.periodContaining(LocalDate.parse("2026-01-05"));

            for (int i = 0; i < 6; i++) {
                BudgetPeriod siguiente = planner.next(period);
                assertThat(siguiente.start()).isEqualTo(period.end().plusDays(1));
                period = siguiente;
            }
        }
    }

    @Nested
    @DisplayName("conteo de periodos")
    class CountPeriods {

        @Test
        void cuentaElCicloActualCuandoLaFechaCaeDentro() {
            CyclePlanner planner = CyclePlanner.of(CycleType.MONTHLY);
            BudgetPeriod septiembre = planner.periodContaining(LocalDate.parse("2026-09-01"));

            int periodos = planner.countPeriodsUntil(septiembre, LocalDate.parse("2026-09-20"));

            assertThat(periodos).isEqualTo(1);
        }

        @Test
        void cuentaLosCiclosHastaLaFechaObjetivo() {
            CyclePlanner planner = CyclePlanner.of(CycleType.MONTHLY);
            BudgetPeriod septiembre = planner.periodContaining(LocalDate.parse("2026-09-01"));

            int periodos = planner.countPeriodsUntil(septiembre, LocalDate.parse("2026-12-15"));

            assertThat(periodos).isEqualTo(4);
        }

        @Test
        void unaFechaYaPasadaCuentaComoUnCiclo() {
            CyclePlanner planner = CyclePlanner.of(CycleType.MONTHLY);
            BudgetPeriod septiembre = planner.periodContaining(LocalDate.parse("2026-09-01"));

            int periodos = planner.countPeriodsUntil(septiembre, LocalDate.parse("2026-05-01"));

            assertThat(periodos).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("validacion")
    class Validation {

        @ParameterizedTest
        @CsvSource({"0", "32", "-1"})
        void rechazaDiasDeAnclajeImposibles(int anchorDay) {
            assertThatThrownBy(() -> CyclePlanner.of(CycleType.MONTHLY, anchorDay))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dia de anclaje");
        }

        @Test
        void unPeriodoNoPuedeTerminarAntesDeEmpezar() {
            assertThatThrownBy(() ->
                            BudgetPeriod.of(LocalDate.parse("2026-09-30"), LocalDate.parse("2026-09-01")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("BudgetPeriod")
    class Period {

        private final BudgetPeriod quincena =
                BudgetPeriod.of(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-15"));

        @Test
        void incluyeAmbosExtremos() {
            assertThat(quincena.contains(LocalDate.parse("2026-09-01"))).isTrue();
            assertThat(quincena.contains(LocalDate.parse("2026-09-15"))).isTrue();
            assertThat(quincena.contains(LocalDate.parse("2026-08-31"))).isFalse();
            assertThat(quincena.contains(LocalDate.parse("2026-09-16"))).isFalse();
        }

        @Test
        void cuentaLosDiasIncluyendoLosExtremos() {
            assertThat(quincena.lengthInDays()).isEqualTo(15);
        }

        @Test
        void detectaTraslapes() {
            BudgetPeriod segunda =
                    BudgetPeriod.of(LocalDate.parse("2026-09-16"), LocalDate.parse("2026-09-30"));
            BudgetPeriod solapada =
                    BudgetPeriod.of(LocalDate.parse("2026-09-10"), LocalDate.parse("2026-09-20"));

            assertThat(quincena.overlaps(segunda)).isFalse();
            assertThat(quincena.overlaps(solapada)).isTrue();
        }
    }
}
