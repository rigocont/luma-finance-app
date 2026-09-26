package com.luma.onboarding.api.dto;

import com.luma.onboarding.application.OnboardingService;

/**
 * Donde va el alta de esta cuenta.
 *
 * <p>Son HECHOS, no un numero de paso guardado: cuantas cosas hay y si ya
 * alcanza para terminar. El asistente decide que pintar con eso. Guardar el paso
 * mentiria en cuanto la persona borrara algo desde otra pantalla, y costaria una
 * columna para un dato que se deduce.
 *
 * @param resumeStep a donde conviene volver. Es una sugerencia para quien
 *     regresa, no una reja: desde el asistente se puede ir a cualquier paso.
 * @param canFinish si ya se puede terminar. Solo los ingresos son obligatorios.
 */
public record OnboardingStateResponse(
        boolean completed,
        String resumeStep,
        int incomeCount,
        int expenseCount,
        int goalCount,
        boolean canFinish) {

    public static OnboardingStateResponse from(OnboardingService.State state) {
        return new OnboardingStateResponse(
                state.completed(),
                state.resumeStep().name(),
                state.incomeCount(),
                state.expenseCount(),
                state.goalCount(),
                state.canFinish());
    }
}
