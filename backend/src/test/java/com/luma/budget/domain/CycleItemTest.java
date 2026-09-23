package com.luma.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.common.model.Money;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * La maquina de estados de un renglon.
 *
 * <p>CycleItem es una entidad de JPA, pero su comportamiento no depende de la
 * base de datos: se construye con {@code materialize} y se prueba sin levantar
 * nada. Eso es justamente el punto de tener la logica en el dominio.
 */
@DisplayName("CycleItem")
class CycleItemTest {

    private static final String MXN = "MXN";
    private static final LocalDate VENCE = LocalDate.of(2026, 3, 15);
    private static final Instant REGISTRO = Instant.parse("2026-03-15T18:30:00Z");

    private static CycleItem gastoFijo(String planeado) {
        return CycleItem.materialize(
                1L,
                CycleItemType.FIXED_EXPENSE,
                ItemSource.EXPENSE,
                7L,
                "Renta",
                null,
                Money.of(planeado),
                VENCE,
                ItemStatus.PENDING,
                Flexibility.CRITICAL,
                0);
    }

    private static CycleItem gastoVariable(String planeado) {
        return CycleItem.materialize(
                1L,
                CycleItemType.VARIABLE_EXPENSE,
                ItemSource.EXPENSE,
                8L,
                "Despensa",
                null,
                Money.of(planeado),
                VENCE,
                ItemStatus.NEEDS_REVIEW,
                Flexibility.IMPORTANT,
                1);
    }

    @Nested
    @DisplayName("al materializarse")
    class Materializacion {

        @Test
        void copiaElNombreYLaFlexibilidadDeLaPlantilla() {
            CycleItem item = gastoFijo("6000.00");

            assertThat(item.getName()).isEqualTo("Renta");
            assertThat(item.getFlexibility()).isEqualTo(Flexibility.CRITICAL);
            assertThat(item.getSourceId()).isEqualTo(7L);
        }

        @Test
        void naceConIdentificadorPublicoYSinMontoReal() {
            CycleItem item = gastoFijo("6000.00");

            assertThat(item.getPublicId()).isNotBlank().hasSize(36);
            assertThat(item.getActualAmount()).isNull();
            assertThat(item.getSettledOn()).isNull();
            assertThat(item.getSettledAt()).isNull();
        }
    }

    @Nested
    @DisplayName("al confirmarse")
    class Confirmacion {

        @Test
        void pagarElMontoExactoLoDejaPagado() {
            CycleItem item = gastoFijo("6000.00");

            item.settle(Money.of("6000.00"), VENCE, REGISTRO);

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PAID);
            assertThat(item.getActualAmount()).isEqualByComparingTo("6000.00");
        }

        @Test
        void pagarDeMasTambienLoDejaPagado() {
            // Pagar mas de lo planeado no es un pago incompleto. El excedente se
            // ve comparando los totales reales con los planeados.
            CycleItem item = gastoFijo("6000.00");

            item.settle(Money.of("6300.00"), VENCE, REGISTRO);

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PAID);
        }

        @Test
        void pagarDeMenosLoDejaParcial() {
            CycleItem item = gastoFijo("6000.00");

            item.settle(Money.of("4000.00"), VENCE, REGISTRO);

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PARTIAL);
            assertThat(item.getActualAmount()).isEqualByComparingTo("4000.00");
        }

        @Test
        void guardaLaFechaEnQueOcurrioYLaDelRegistroPorSeparado() {
            // Se paga el dia 20 pero se captura el 15: son dos datos distintos y
            // ninguno debe sobrescribir al otro.
            CycleItem item = gastoFijo("6000.00");
            LocalDate ocurrio = LocalDate.of(2026, 3, 20);

            item.settle(Money.of("6000.00"), ocurrio, REGISTRO);

            assertThat(item.getSettledOn()).isEqualTo(ocurrio);
            assertThat(item.getSettledAt()).isEqualTo(REGISTRO);
        }

        @Test
        void confirmarNoMueveLaFechaDeVencimiento() {
            CycleItem item = gastoFijo("6000.00");

            item.settle(Money.of("6000.00"), LocalDate.of(2026, 3, 20), REGISTRO);

            assertThat(item.getDueDate()).isEqualTo(VENCE);
        }

        @Test
        void unRenglonVencidoSeConfirmaIgual() {
            CycleItem item = gastoFijo("6000.00");
            item.markOverdue();

            item.settle(Money.of("6000.00"), VENCE, REGISTRO);

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PAID);
        }
    }

    @Nested
    @DisplayName("al ajustar el monto planeado")
    class Ajuste {

        @Test
        void confirmarElMontoDeUnVariableLoSacaDeRevision() {
            CycleItem item = gastoVariable("2500.00");

            item.changePlannedAmount(Money.of("2780.50"));

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PENDING);
            assertThat(item.getPlannedAmount()).isEqualByComparingTo("2780.50");
        }

        @Test
        void ajustarUnPendienteNoCambiaSuEstado() {
            CycleItem item = gastoFijo("6000.00");

            item.changePlannedAmount(Money.of("6500.00"));

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PENDING);
        }

        @Test
        void ajustarUnPagadoNoLoDevuelveAPendiente() {
            // Solo NEEDS_REVIEW se mueve. Un pagado que se corrige sigue pagado;
            // para deshacer el pago existe reopen().
            CycleItem item = gastoFijo("6000.00");
            item.settle(Money.of("6000.00"), VENCE, REGISTRO);

            item.changePlannedAmount(Money.of("6100.00"));

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PAID);
        }
    }

    @Nested
    @DisplayName("al omitirse y reabrirse")
    class OmisionYReapertura {

        @Test
        void omitirLimpiaElMontoReal() {
            CycleItem item = gastoFijo("6000.00");
            item.settle(Money.of("6000.00"), VENCE, REGISTRO);

            item.skip();

            assertThat(item.getStatus()).isEqualTo(ItemStatus.SKIPPED);
            assertThat(item.getActualAmount()).isNull();
            assertThat(item.getSettledOn()).isNull();
            assertThat(item.getSettledAt()).isNull();
        }

        @Test
        void omitirConservaElMontoPlaneado() {
            // El renglon sigue existiendo: se sabe cuanto se iba a gastar y que
            // este ciclo no aplico. Eso es lo que lo distingue de borrarlo.
            CycleItem item = gastoFijo("6000.00");

            item.skip();

            assertThat(item.getPlannedAmount()).isEqualByComparingTo("6000.00");
        }

        @Test
        void reabrirUnPagadoLoDejaPendienteYSinMontoReal() {
            CycleItem item = gastoFijo("6000.00");
            item.settle(Money.of("6000.00"), VENCE, REGISTRO);

            item.reopen();

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PENDING);
            assertThat(item.getActualAmount()).isNull();
            assertThat(item.getSettledAt()).isNull();
        }

        @Test
        void reabrirUnOmitidoLoDejaPendiente() {
            CycleItem item = gastoFijo("6000.00");
            item.skip();

            item.reopen();

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PENDING);
        }

        @Test
        void reabrirUnVariableLoDejaPendienteNoEnRevision() {
            // Deliberado: el monto ya se confirmo una vez, no hay que volver a
            // preguntarlo.
            CycleItem item = gastoVariable("2500.00");
            item.changePlannedAmount(Money.of("2780.50"));
            item.settle(Money.of("2780.50"), VENCE, REGISTRO);

            item.reopen();

            assertThat(item.getStatus()).isEqualTo(ItemStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("al revisar vencimientos")
    class Vencimientos {

        private static final LocalDate DIA_SIGUIENTE = VENCE.plusDays(1);

        @Test
        void unPendienteConFechaPasadaVence() {
            assertThat(gastoFijo("6000.00").becameOverdue(DIA_SIGUIENTE)).isTrue();
        }

        @Test
        void unPendienteEnSuPropioDiaNoVence() {
            // Todavia puede pagarse hoy.
            assertThat(gastoFijo("6000.00").becameOverdue(VENCE)).isFalse();
        }

        @Test
        void unPagadoNoVence() {
            CycleItem item = gastoFijo("6000.00");
            item.settle(Money.of("6000.00"), VENCE, REGISTRO);

            assertThat(item.becameOverdue(DIA_SIGUIENTE)).isFalse();
        }

        @Test
        void unParcialNoVence() {
            CycleItem item = gastoFijo("6000.00");
            item.settle(Money.of("4000.00"), VENCE, REGISTRO);

            assertThat(item.becameOverdue(DIA_SIGUIENTE)).isFalse();
        }

        @Test
        void unOmitidoNoVence() {
            CycleItem item = gastoFijo("6000.00");
            item.skip();

            assertThat(item.becameOverdue(DIA_SIGUIENTE)).isFalse();
        }

        @Test
        void unRenglonYaVencidoNoSeVuelveAMarcar() {
            CycleItem item = gastoFijo("6000.00");
            item.markOverdue();

            assertThat(item.becameOverdue(DIA_SIGUIENTE)).isFalse();
        }

        @Test
        void unRenglonEnRevisionConFechaPasadaVence() {
            // No haber confirmado el monto no exime de que la fecha ya paso.
            assertThat(gastoVariable("2500.00").becameOverdue(DIA_SIGUIENTE)).isTrue();
        }
    }

    @Nested
    @DisplayName("la vista para la calculadora")
    class VistaPlana {

        @Test
        void llevaElMontoPlaneadoYElRealCuandoExiste() {
            CycleItem item = gastoFijo("6000.00");
            item.settle(Money.of("5800.00"), VENCE, REGISTRO);

            PlannedItem plano = item.toPlannedItem(MXN);

            assertThat(plano.plannedAmount()).isEqualTo(Money.of("6000.00"));
            assertThat(plano.actualAmount()).isEqualTo(Money.of("5800.00"));
            assertThat(plano.status()).isEqualTo(ItemStatus.PARTIAL);
        }

        @Test
        void dejaElMontoRealEnNuloMientrasNadieConfirma() {
            PlannedItem plano = gastoFijo("6000.00").toPlannedItem(MXN);

            assertThat(plano.actualAmount()).isNull();
        }
    }
}
