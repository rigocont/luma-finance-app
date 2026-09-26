package com.luma.insights.domain;

import com.luma.common.model.Money;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A donde podria ir el dinero que sobra.
 *
 * <p>Codigo puro, simetrico a DeficitAdvisor pero en la otra direccion: en vez
 * de recortar de lo que menos duele a lo que mas, reparte de la meta mas
 * prioritaria a la menos -"todo a la primera hasta llenarla, luego la
 * siguiente"- porque asi es como ya se trata la prioridad en el resto del
 * producto (DeficitAdvisor la usa para decidir que ahorro tocar primero en un
 * deficit).
 *
 * <p>NO modifica nada. Es una sugerencia; la persona aporta desde Ahorros si
 * quiere seguirla.
 */
public final class SurplusAllocator {

    private SurplusAllocator() {}

    /**
     * Una meta activa a la que le falta algo.
     *
     * @param priority un numero mas bajo es MAS prioritario -1 es la primera
     *     de la lista que la persona ordeno.
     * @param remaining cuanto le falta para llegar a su objetivo. Nunca
     *     negativo: una meta ya cumplida no se pasa aqui.
     */
    public record GoalCandidate(String goalId, String name, int priority, Money remaining) {}

    public record Allocation(String goalId, String name, Money amount) {}

    /**
     * @param surplus lo que sobra, en positivo. Cero cuando no hay remanente.
     * @param allocated cuanto del remanente se alcanzo a repartir. Puede ser
     *     menor que {@code surplus} si las metas juntas necesitan menos de lo
     *     que sobra -el resto simplemente no se reparte, no se inventa un
     *     destino.
     * @param allocations el reparto, de la meta mas prioritaria a la menos.
     */
    public record Advice(Money surplus, Money allocated, List<Allocation> allocations) {}

    public static Advice allocate(Money surplus, List<GoalCandidate> candidates) {
        String currency = surplus.currency();

        if (!surplus.isPositive()) {
            return new Advice(Money.zero(currency), Money.zero(currency), List.of());
        }

        List<GoalCandidate> enOrden = candidates.stream()
                .filter(candidato -> candidato.remaining().isPositive())
                .sorted(Comparator.comparing(GoalCandidate::priority).thenComparing(GoalCandidate::name))
                .toList();

        List<Allocation> propuestos = new ArrayList<>();
        Money restante = surplus;

        for (GoalCandidate candidato : enOrden) {
            if (!restante.isPositive()) {
                break;
            }

            boolean restanteEsMenor = restante.subtract(candidato.remaining()).isNegative();
            Money asignado = restanteEsMenor ? restante : candidato.remaining();

            propuestos.add(new Allocation(candidato.goalId(), candidato.name(), asignado));
            restante = restante.subtract(asignado);
        }

        Money allocated = surplus.subtract(restante);

        return new Advice(surplus, allocated, List.copyOf(propuestos));
    }
}
