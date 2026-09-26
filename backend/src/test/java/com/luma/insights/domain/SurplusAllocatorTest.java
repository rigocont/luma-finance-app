package com.luma.insights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.common.model.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("El repartidor de remanente")
class SurplusAllocatorTest {

    private static SurplusAllocator.GoalCandidate meta(String nombre, int prioridad, String falta) {
        return new SurplusAllocator.GoalCandidate("goal-" + nombre, nombre, prioridad, Money.of(falta));
    }

    private static List<String> nombresDe(SurplusAllocator.Advice advice) {
        return advice.allocations().stream().map(SurplusAllocator.Allocation::name).toList();
    }

    @Nested
    @DisplayName("cuando no hay remanente")
    class SinRemanente {

        @Test
        @DisplayName("en deficit no reparte nada")
        void enDeficitNoReparteNada() {
            SurplusAllocator.Advice advice =
                    SurplusAllocator.allocate(Money.of("-100.00"), List.of(meta("Fondo", 1, "500.00")));

            assertThat(advice.allocations()).isEmpty();
            assertThat(advice.allocated()).isEqualTo(Money.of("0.00"));
        }

        @Test
        @DisplayName("en cero tampoco")
        void enCeroTampoco() {
            assertThat(SurplusAllocator.allocate(Money.of("0.00"), List.of()).allocations()).isEmpty();
        }
    }

    @Nested
    @DisplayName("el orden")
    class Orden {

        @Test
        @DisplayName("primero la meta mas prioritaria, hasta llenarla")
        void primeroLaMasPrioritaria() {
            SurplusAllocator.Advice advice = SurplusAllocator.allocate(
                    Money.of("1000.00"),
                    List.of(meta("Vacaciones", 2, "5000.00"), meta("Emergencias", 1, "300.00")));

            assertThat(nombresDe(advice)).containsExactly("Emergencias", "Vacaciones");
            assertThat(advice.allocations().get(0).amount()).isEqualTo(Money.of("300.00"));
            assertThat(advice.allocations().get(1).amount()).isEqualTo(Money.of("700.00"));
        }

        @Test
        @DisplayName("una meta ya cumplida no aparece")
        void unaMetaCumplidaNoAparece() {
            SurplusAllocator.Advice advice = SurplusAllocator.allocate(
                    Money.of("500.00"), List.of(meta("Ya lista", 1, "0.00"), meta("Falta", 2, "200.00")));

            assertThat(nombresDe(advice)).containsExactly("Falta");
        }

        @Test
        @DisplayName("dos metas con la misma prioridad desempatan por nombre, siempre igual")
        void desempateEstable() {
            // El remanente alcanza para llenar una meta completa y dejar algo
            // para la otra: asi ambas aparecen en el reparto sin importar el
            // desempate, y el orden entre ellas es lo unico que la prueba
            // necesita demostrar.
            List<SurplusAllocator.GoalCandidate> unOrden =
                    List.of(meta("Bebe", 1, "500.00"), meta("Alfa", 1, "500.00"));
            List<SurplusAllocator.GoalCandidate> elOtro =
                    List.of(meta("Alfa", 1, "500.00"), meta("Bebe", 1, "500.00"));

            assertThat(nombresDe(SurplusAllocator.allocate(Money.of("700.00"), unOrden)))
                    .isEqualTo(nombresDe(SurplusAllocator.allocate(Money.of("700.00"), elOtro)))
                    .containsExactly("Alfa", "Bebe");
        }
    }

    @Nested
    @DisplayName("cuanto reparte")
    class Cuanto {

        @Test
        @DisplayName("si sobra despues de llenar todas las metas, ese resto no se reparte")
        void elRestoNoSeInventa() {
            SurplusAllocator.Advice advice =
                    SurplusAllocator.allocate(Money.of("1000.00"), List.of(meta("Unica", 1, "300.00")));

            assertThat(advice.allocated()).isEqualTo(Money.of("300.00"));
            assertThat(advice.surplus()).isEqualTo(Money.of("1000.00"));
        }

        @Test
        @DisplayName("sin ninguna meta activa, no hay nada que repartir")
        void sinMetasActivas() {
            SurplusAllocator.Advice advice = SurplusAllocator.allocate(Money.of("1000.00"), List.of());

            assertThat(advice.allocations()).isEmpty();
            assertThat(advice.allocated()).isEqualTo(Money.of("0.00"));
        }
    }
}
