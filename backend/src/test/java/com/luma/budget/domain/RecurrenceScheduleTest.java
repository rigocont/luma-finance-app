package com.luma.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RecurrenceSchedule")
class RecurrenceScheduleTest {

    private static final LocalDate DESDE_SIEMPRE = LocalDate.parse("2020-01-01");

    private static BudgetPeriod periodo(String start, String end) {
        return BudgetPeriod.of(LocalDate.parse(start), LocalDate.parse(end));
    }

    @Nested
    @DisplayName("mensual")
    class Monthly {

        @Test
        void noOcurreEnLaQuincenaQueNoContieneSuVencimiento() {
            RecurrenceSchedule renta = RecurrenceSchedule.monthly(20, DESDE_SIEMPRE);

            assertThat(renta.occurrencesIn(periodo("2026-09-01", "2026-09-15"))).isEmpty();
        }

        @Test
        void ocurreUnaVezEnLaQuincenaQueContieneSuVencimiento() {
            RecurrenceSchedule renta = RecurrenceSchedule.monthly(20, DESDE_SIEMPRE);

            assertThat(renta.occurrencesIn(periodo("2026-09-16", "2026-09-30")))
                    .containsExactly(LocalDate.parse("2026-09-20"));
        }

        @Test
        void unGastoMensualOcurreDosVecesEnUnCicloBimestral() {
            RecurrenceSchedule renta = RecurrenceSchedule.monthly(20, DESDE_SIEMPRE);

            // Es el caso que obliga a que occurrencesIn devuelva una lista y no
            // una sola fecha: en un bimestre, la renta se paga dos veces.
            assertThat(renta.occurrencesIn(periodo("2026-09-01", "2026-10-31")))
                    .containsExactly(LocalDate.parse("2026-09-20"), LocalDate.parse("2026-10-20"));
        }

        @Test
        void elDia31SeRecortaAlUltimoDiaDeFebrero() {
            RecurrenceSchedule predial = RecurrenceSchedule.monthly(31, DESDE_SIEMPRE);

            assertThat(predial.occurrencesIn(periodo("2026-02-16", "2026-02-28")))
                    .containsExactly(LocalDate.parse("2026-02-28"));
        }

        @Test
        void elDia31CaeEl29EnFebreroBisiesto() {
            RecurrenceSchedule predial = RecurrenceSchedule.monthly(31, DESDE_SIEMPRE);

            assertThat(predial.occurrencesIn(periodo("2024-02-16", "2024-02-29")))
                    .containsExactly(LocalDate.parse("2024-02-29"));
        }

        @Test
        void elDia31CaeEl30EnMesesDeTreintaDias() {
            RecurrenceSchedule renta = RecurrenceSchedule.monthly(31, DESDE_SIEMPRE);

            assertThat(renta.occurrencesIn(periodo("2026-04-16", "2026-04-30")))
                    .containsExactly(LocalDate.parse("2026-04-30"));
        }

        @Test
        void noOcurreAntesDeSuFechaDeInicio() {
            RecurrenceSchedule nuevo =
                    RecurrenceSchedule.monthly(10, LocalDate.parse("2026-09-15"));

            assertThat(nuevo.occurrencesIn(periodo("2026-09-01", "2026-09-30"))).isEmpty();
        }

        @Test
        void noOcurreDespuesDeSuFechaDeFin() {
            RecurrenceSchedule terminado = new RecurrenceSchedule(
                    Frequency.MONTHLY, 10, DESDE_SIEMPRE, LocalDate.parse("2026-08-31"));

            assertThat(terminado.occurrencesIn(periodo("2026-09-01", "2026-09-30"))).isEmpty();
        }

        @Test
        void sinDiaIndicadoUsaElDiaDeLaFechaDeInicio() {
            RecurrenceSchedule sinDia = new RecurrenceSchedule(
                    Frequency.MONTHLY, null, LocalDate.parse("2026-01-07"), null);

            assertThat(sinDia.occurrencesIn(periodo("2026-09-01", "2026-09-30")))
                    .containsExactly(LocalDate.parse("2026-09-07"));
        }
    }

    @Nested
    @DisplayName("quincenal")
    class Biweekly {

        @Test
        void ocurreDosVecesEnUnMes() {
            RecurrenceSchedule sueldo =
                    new RecurrenceSchedule(Frequency.BIWEEKLY, 5, DESDE_SIEMPRE, null);

            assertThat(sueldo.occurrencesIn(periodo("2026-09-01", "2026-09-30")))
                    .containsExactly(LocalDate.parse("2026-09-05"), LocalDate.parse("2026-09-20"));
        }

        @Test
        void laSegundaOcurrenciaSeRecortaCuandoSePasaDelMes() {
            RecurrenceSchedule sueldo =
                    new RecurrenceSchedule(Frequency.BIWEEKLY, 20, DESDE_SIEMPRE, null);

            // 20 + 15 = 35, que no existe: cae el ultimo dia del mes.
            assertThat(sueldo.occurrencesIn(periodo("2026-09-01", "2026-09-30")))
                    .containsExactly(LocalDate.parse("2026-09-20"), LocalDate.parse("2026-09-30"));
        }

        @Test
        void ocurreUnaSolaVezDentroDeUnaQuincena() {
            RecurrenceSchedule sueldo =
                    new RecurrenceSchedule(Frequency.BIWEEKLY, 5, DESDE_SIEMPRE, null);

            assertThat(sueldo.occurrencesIn(periodo("2026-09-01", "2026-09-15")))
                    .containsExactly(LocalDate.parse("2026-09-05"));
        }
    }

    @Nested
    @DisplayName("bimestral")
    class Bimonthly {

        @Test
        void seAlineaConLaParidadDelMesDeInicio() {
            // Empieza en enero (impar): ocurre en meses impares.
            RecurrenceSchedule luz = new RecurrenceSchedule(
                    Frequency.BIMONTHLY, 10, LocalDate.parse("2026-01-10"), null);

            assertThat(luz.occurrencesIn(periodo("2026-09-01", "2026-10-31")))
                    .containsExactly(LocalDate.parse("2026-09-10"));
        }

        @Test
        void empezandoEnMesParOcurreEnMesesPares() {
            RecurrenceSchedule agua = new RecurrenceSchedule(
                    Frequency.BIMONTHLY, 10, LocalDate.parse("2026-02-10"), null);

            assertThat(agua.occurrencesIn(periodo("2026-09-01", "2026-10-31")))
                    .containsExactly(LocalDate.parse("2026-10-10"));
        }
    }

    @Nested
    @DisplayName("anual y unica vez")
    class AnnualAndOneTime {

        @Test
        void elAnualOcurreEnElMismoDiaYMesCadaAno() {
            RecurrenceSchedule seguro = new RecurrenceSchedule(
                    Frequency.ANNUAL, null, LocalDate.parse("2024-03-15"), null);

            assertThat(seguro.occurrencesIn(periodo("2026-03-01", "2026-03-31")))
                    .containsExactly(LocalDate.parse("2026-03-15"));
        }

        @Test
        void elAnualDel29DeFebreroSeRecortaEnAnosComunes() {
            RecurrenceSchedule raro = new RecurrenceSchedule(
                    Frequency.ANNUAL, null, LocalDate.parse("2024-02-29"), null);

            assertThat(raro.occurrencesIn(periodo("2026-02-16", "2026-02-28")))
                    .containsExactly(LocalDate.parse("2026-02-28"));
        }

        @Test
        void elAnualNoOcurreEnOtrosMeses() {
            RecurrenceSchedule seguro = new RecurrenceSchedule(
                    Frequency.ANNUAL, null, LocalDate.parse("2024-03-15"), null);

            assertThat(seguro.occurrencesIn(periodo("2026-09-01", "2026-09-30"))).isEmpty();
        }

        @Test
        void elDeUnaVezSoloOcurreEnSuFecha() {
            RecurrenceSchedule reparacion =
                    RecurrenceSchedule.oneTime(LocalDate.parse("2026-09-20"));

            assertThat(reparacion.occurrencesIn(periodo("2026-09-16", "2026-09-30")))
                    .containsExactly(LocalDate.parse("2026-09-20"));
            assertThat(reparacion.occurrencesIn(periodo("2026-09-01", "2026-09-15"))).isEmpty();
            assertThat(reparacion.occurrencesIn(periodo("2026-10-01", "2026-10-31"))).isEmpty();
        }
    }

    @Test
    void elConteoCoincideConLaListaDeOcurrencias() {
        RecurrenceSchedule renta = RecurrenceSchedule.monthly(20, DESDE_SIEMPRE);
        BudgetPeriod bimestre = periodo("2026-09-01", "2026-10-31");

        assertThat(renta.occurrenceCountIn(bimestre)).isEqualTo(2);
    }
}
