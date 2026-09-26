package com.luma.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.common.model.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("El asesor de deficit")
class DeficitAdvisorTest {

    private static CutCandidate gasto(String nombre, String monto, Flexibility flexibility) {
        return new CutCandidate(
                "item-" + nombre,
                nombre,
                CycleItemType.FIXED_EXPENSE,
                ItemStatus.PENDING,
                flexibility,
                null,
                Money.of(monto));
    }

    private static CutCandidate variable(String nombre, String monto, Flexibility flexibility) {
        return new CutCandidate(
                "item-" + nombre,
                nombre,
                CycleItemType.VARIABLE_EXPENSE,
                ItemStatus.NEEDS_REVIEW,
                flexibility,
                null,
                Money.of(monto));
    }

    private static CutCandidate ahorro(String nombre, String monto, int prioridad) {
        return new CutCandidate(
                "item-" + nombre,
                nombre,
                CycleItemType.SAVING,
                ItemStatus.PENDING,
                null,
                prioridad,
                Money.of(monto));
    }

    private static CutCandidate yaPagado(String nombre, String monto) {
        return new CutCandidate(
                "item-" + nombre,
                nombre,
                CycleItemType.FIXED_EXPENSE,
                ItemStatus.PAID,
                Flexibility.FLEXIBLE,
                null,
                Money.of(monto));
    }

    private static List<String> nombresDe(DeficitAdvisor.Advice advice) {
        return advice.cuts().stream().map(CutCandidate::name).toList();
    }

    @Nested
    @DisplayName("cuando no hay deficit")
    class SinDeficit {

        @Test
        @DisplayName("con remanente no aconseja nada")
        void conRemanente() {
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("500.00"), List.of(gasto("Cena", "300.00", Flexibility.FLEXIBLE)));

            assertThat(advice.cuts()).isEmpty();
            assertThat(advice.missing()).isEqualTo(Money.of("0.00"));
            assertThat(advice.coversTheGap()).isTrue();
        }

        @Test
        @DisplayName("en equilibrio exacto tampoco")
        void enEquilibrio() {
            assertThat(DeficitAdvisor.adviseFor(Money.of("0.00"), List.of()).cuts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("lo que nunca propone")
    class NuncaPropone {

        @Test
        @DisplayName("un gasto critico, aunque sea el unico que alcanzaria")
        void nuncaUnGastoCritico() {
            // Es una promesa del producto: la pantalla de gastos dice que un
            // gasto critico no se va a sugerir retrasar aunque falte dinero.
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-5000.00"), List.of(gasto("Renta", "9000.00", Flexibility.CRITICAL)));

            assertThat(advice.cuts()).isEmpty();
            assertThat(advice.coversTheGap()).isFalse();
            assertThat(advice.missing()).isEqualTo(Money.of("5000.00"));
        }

        @Test
        @DisplayName("un gasto que ya se pago")
        void nuncaLoYaPagado() {
            // El dinero ya salio: recortarlo no devuelve nada.
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-500.00"), List.of(yaPagado("Cena", "800.00")));

            assertThat(advice.cuts()).isEmpty();
        }

        @Test
        @DisplayName("un renglon en cero")
        void nuncaUnRenglonEnCero() {
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-500.00"), List.of(gasto("Nada", "0.00", Flexibility.FLEXIBLE)));

            assertThat(advice.cuts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("el orden")
    class Orden {

        @Test
        @DisplayName("primero lo flexible, luego el ahorro, al final lo importante")
        void deLoQueDuelaMenosALoQueDuelaMas() {
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-3000.00"),
                    List.of(
                            gasto("Luz", "700.00", Flexibility.IMPORTANT),
                            ahorro("Fondo", "1000.00", 1),
                            gasto("Streaming", "300.00", Flexibility.FLEXIBLE),
                            gasto("Renta", "9000.00", Flexibility.CRITICAL)));

            assertThat(nombresDe(advice)).containsExactly("Streaming", "Fondo", "Luz");
        }

        @Test
        @DisplayName("los ahorros van del menos prioritario al mas")
        void elAhorroMenosPrioritarioPrimero() {
            // Para eso sirve la prioridad que la persona le dio a sus metas.
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-5000.00"),
                    List.of(
                            ahorro("Emergencias", "1000.00", 1),
                            ahorro("Vacaciones", "800.00", 3),
                            ahorro("Enganche", "900.00", 2)));

            assertThat(nombresDe(advice))
                    .containsExactly("Vacaciones", "Enganche", "Emergencias");
        }

        @Test
        @DisplayName("dentro de un grupo, primero el monto mas grande")
        void elMontoMasGrandePrimero() {
            // Llegar a la cifra con la menor cantidad de renuncias posible.
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-5000.00"),
                    List.of(
                            gasto("Chico", "200.00", Flexibility.FLEXIBLE),
                            gasto("Grande", "1500.00", Flexibility.FLEXIBLE),
                            gasto("Mediano", "800.00", Flexibility.FLEXIBLE)));

            assertThat(nombresDe(advice)).containsExactly("Grande", "Mediano", "Chico");
        }

        @Test
        @DisplayName("dos montos iguales salen siempre en el mismo orden")
        void desempateEstable() {
            // Sin desempate, el orden dependeria de como llegaron y la pantalla
            // cambiaria de un refresco a otro sin que nada hubiera cambiado.
            List<CutCandidate> unOrden = List.of(
                    gasto("Bebe", "500.00", Flexibility.FLEXIBLE),
                    gasto("Alfa", "500.00", Flexibility.FLEXIBLE));

            List<CutCandidate> elOtro = List.of(
                    gasto("Alfa", "500.00", Flexibility.FLEXIBLE),
                    gasto("Bebe", "500.00", Flexibility.FLEXIBLE));

            assertThat(nombresDe(DeficitAdvisor.adviseFor(Money.of("-2000.00"), unOrden)))
                    .isEqualTo(nombresDe(DeficitAdvisor.adviseFor(Money.of("-2000.00"), elOtro)))
                    .containsExactly("Alfa", "Bebe");
        }
    }

    @Nested
    @DisplayName("cuanto propone")
    class Cuanto {

        @Test
        @DisplayName("se detiene en cuanto alcanza")
        void seDetieneAlCubrir() {
            // Seguir proponiendo pediria renuncias que no hacen falta.
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-1000.00"),
                    List.of(
                            gasto("Grande", "1200.00", Flexibility.FLEXIBLE),
                            gasto("Otro", "800.00", Flexibility.FLEXIBLE)));

            assertThat(nombresDe(advice)).containsExactly("Grande");
            assertThat(advice.covered()).isEqualTo(Money.of("1200.00"));
            assertThat(advice.coversTheGap()).isTrue();
        }

        @Test
        @DisplayName("cubrir exactamente cuenta como cubierto")
        void exactamenteCubierto() {
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-1000.00"),
                    List.of(gasto("Justo", "1000.00", Flexibility.FLEXIBLE)));

            assertThat(advice.cuts()).hasSize(1);
            assertThat(advice.coversTheGap()).isTrue();
        }

        @Test
        @DisplayName("si no alcanza lo dice y propone todo lo que hay")
        void cuandoNoAlcanza() {
            // Presentar una lista como solucion sin serlo es peor que no dar
            // ninguna.
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-5000.00"),
                    List.of(
                            gasto("Uno", "500.00", Flexibility.FLEXIBLE),
                            ahorro("Meta", "1000.00", 1)));

            assertThat(advice.cuts()).hasSize(2);
            assertThat(advice.covered()).isEqualTo(Money.of("1500.00"));
            assertThat(advice.coversTheGap()).isFalse();
        }

        @Test
        @DisplayName("un gasto variable sin confirmar se propone igual")
        void tambienLosEstimados() {
            // Suele ser la palanca mas grande —la despensa— y esconderla seria
            // dejar fuera justo lo que mas se puede mover. Que es estimado se
            // dice en la respuesta, no se resuelve ocultandolo.
            DeficitAdvisor.Advice advice = DeficitAdvisor.adviseFor(
                    Money.of("-1000.00"),
                    List.of(variable("Despensa", "3000.00", Flexibility.FLEXIBLE)));

            assertThat(nombresDe(advice)).containsExactly("Despensa");
        }
    }
}
