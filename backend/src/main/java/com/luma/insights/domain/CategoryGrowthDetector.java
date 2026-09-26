package com.luma.insights.domain;

import com.luma.common.model.Money;
import java.util.List;

/**
 * Si una serie de montos, un ciclo detras de otro, cuenta una historia de
 * crecimiento sostenido.
 *
 * <p>Codigo puro, como BudgetCalculator: recibe numeros ya calculados y no
 * toca ninguna base de datos. "Sostenido" tiene una definicion exacta y nada
 * generosa: cada ciclo de la serie tiene que costar mas que el anterior. Un
 * ciclo caro seguido de uno barato no cuenta, aunque el promedio suba -eso
 * seria una racha, no una tendencia.
 */
public final class CategoryGrowthDetector {

    private CategoryGrowthDetector() {}

    /**
     * @param amounts los montos de una sola categoria, del ciclo mas viejo al
     *     mas nuevo. El tamano de la lista ES la ventana que se exige: quien
     *     llama decide cuantos ciclos hacen falta para hablar de "sostenido".
     * @return true si cada monto es estrictamente mayor que el anterior, y el
     *     ultimo es positivo -una categoria en ceros no crecio, dejo de
     *     existir.
     */
    public static boolean isSustainedGrowth(List<Money> amounts) {
        if (amounts.size() < 2) {
            return false;
        }

        for (int i = 1; i < amounts.size(); i++) {
            Money anterior = amounts.get(i - 1);
            Money actual = amounts.get(i);

            if (!actual.subtract(anterior).isPositive()) {
                return false;
            }
        }

        return amounts.get(amounts.size() - 1).isPositive();
    }
}
