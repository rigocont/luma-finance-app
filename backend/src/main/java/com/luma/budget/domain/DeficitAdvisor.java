package com.luma.budget.domain;

import com.luma.common.model.Money;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * De donde podria salir el dinero que falta.
 *
 * <p>Codigo puro, como la calculadora. Recibe un balance y una lista de
 * candidatos, y devuelve en que orden conviene mirarlos. NO modifica nada: la
 * persona decide. Una aplicacion de dinero que mueve gastos por su cuenta deja
 * de ser confiable el primer dia que se equivoca.
 *
 * <p><b>El orden no es arbitrario.</b> Va de lo que duele menos a lo que duele
 * mas:
 *
 * <ol>
 *   <li><b>Gastos flexibles.</b> "Flexible" significa justamente que se puede
 *       mover. Dejar de salir a cenar este ciclo no tiene consecuencia mas alla
 *       de no haber cenado fuera.
 *   <li><b>Ahorros, del menos prioritario al mas.</b> No apartar este ciclo no
 *       le cuesta nada a nadie hoy, y es reversible: el ciclo que entra vuelve a
 *       intentarlo. Se empieza por la meta que la persona puso mas abajo, que es
 *       exactamente para lo que sirve esa prioridad.
 *   <li><b>Gastos importantes.</b> Al final, porque retrasarlos SI tiene
 *       consecuencia: un recargo, un servicio cortado, una relacion tensa.
 * </ol>
 *
 * <p>Los gastos CRITICOS no aparecen nunca. Es una promesa del producto, no una
 * preferencia de implementacion.
 *
 * <p>Dentro de cada grupo se propone primero el monto mas grande. Asi se llega a
 * la cifra que falta con la menor cantidad de renuncias posible: tres recortes
 * chicos se sienten peor que uno grande, aunque sumen lo mismo.
 */
public final class DeficitAdvisor {

    private DeficitAdvisor() {}

    /**
     * La lista de recortes propuestos.
     *
     * @param missing lo que falta, en positivo. Cero cuando no hay deficit.
     * @param covered lo que suman los recortes propuestos.
     * @param coversTheGap si con estos recortes alcanza. Cuando es falso, la
     *     interfaz tiene que decirlo: proponer una lista que no cierra el hueco
     *     sin avisarlo seria dar por resuelto algo que no lo esta.
     * @param cuts los renglones, en el orden en que conviene mirarlos.
     */
    public record Advice(
            Money missing, Money covered, boolean coversTheGap, List<CutCandidate> cuts) {}

    public static Advice adviseFor(Money balance, List<CutCandidate> candidates) {
        String currency = balance.currency();

        if (!balance.isNegative()) {
            // Sin deficit no hay nada que aconsejar. Se devuelve una respuesta
            // vacia y no un nulo: quien llama no tiene por que preguntar dos
            // veces si hay deficit.
            return new Advice(Money.zero(currency), Money.zero(currency), true, List.of());
        }

        Money missing = balance.negate();

        List<CutCandidate> elegibles = candidates.stream()
                .filter(CutCandidate::isStillAvoidable)
                .filter(candidato -> !candidato.isUntouchable())
                .filter(candidato -> candidato.amount().isPositive())
                .toList();

        List<CutCandidate> enOrden = new ArrayList<>();
        enOrden.addAll(porMontoDescendente(gastosCon(elegibles, Flexibility.FLEXIBLE)));
        enOrden.addAll(ahorrosPorPrioridad(elegibles));
        enOrden.addAll(porMontoDescendente(gastosCon(elegibles, Flexibility.IMPORTANT)));

        List<CutCandidate> propuestos = new ArrayList<>();
        Money acumulado = Money.zero(currency);

        for (CutCandidate candidato : enOrden) {
            if (!acumulado.subtract(missing).isNegative()) {
                // Ya alcanza. Seguir agregando pediria renuncias que no hacen
                // falta, y una lista larga se lee como un regano.
                break;
            }
            propuestos.add(candidato);
            acumulado = acumulado.add(candidato.amount());
        }

        boolean alcanza = !acumulado.subtract(missing).isNegative();

        return new Advice(missing, acumulado, alcanza, List.copyOf(propuestos));
    }

    private static List<CutCandidate> gastosCon(
            List<CutCandidate> candidatos, Flexibility flexibility) {

        return candidatos.stream()
                .filter(candidato -> !candidato.isSaving())
                .filter(candidato -> candidato.flexibility() == flexibility)
                .toList();
    }

    private static List<CutCandidate> porMontoDescendente(List<CutCandidate> candidatos) {
        return candidatos.stream()
                .sorted(Comparator.comparing(
                                (CutCandidate candidato) -> candidato.amount().amount())
                        .reversed()
                        // El nombre desempata para que el orden no dependa de en
                        // que orden llegaron: dos gastos del mismo monto deben
                        // salir siempre igual.
                        .thenComparing(CutCandidate::name))
                .toList();
    }

    private static List<CutCandidate> ahorrosPorPrioridad(List<CutCandidate> candidatos) {
        return candidatos.stream()
                .filter(CutCandidate::isSaving)
                .sorted(Comparator.comparing(
                                CutCandidate::goalPriority,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CutCandidate::name))
                .toList();
    }
}
