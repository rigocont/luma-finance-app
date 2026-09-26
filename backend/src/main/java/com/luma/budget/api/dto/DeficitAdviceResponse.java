package com.luma.budget.api.dto;

import com.luma.budget.domain.DeficitAdvisor;
import com.luma.common.web.MoneyDto;
import java.util.List;

/**
 * De donde podria salir el dinero que falta.
 *
 * <p>Con el ciclo en equilibrio o con remanente, {@code missing} es cero y la
 * lista viene vacia. No es un error ni un 404: preguntar "que hago" cuando no
 * hay nada que hacer tiene una respuesta, y es "nada".
 *
 * @param coversTheGap si los recortes propuestos alcanzan para cerrar el hueco.
 *     Cuando es falso, la interfaz TIENE que decirlo. Una lista que se presenta
 *     como solucion sin serlo es peor que no dar ninguna.
 */
public record DeficitAdviceResponse(
        MoneyDto missing,
        MoneyDto covered,
        boolean coversTheGap,
        List<CutSuggestionResponse> cuts) {

    public static DeficitAdviceResponse from(DeficitAdvisor.Advice advice, String currency) {
        return new DeficitAdviceResponse(
                MoneyDto.from(advice.missing()),
                MoneyDto.from(advice.covered()),
                advice.coversTheGap(),
                advice.cuts().stream()
                        .map(cut -> CutSuggestionResponse.from(cut, currency))
                        .toList());
    }
}
