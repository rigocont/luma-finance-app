package com.luma.users.api.dto;

import com.luma.users.domain.UserPreferences;

/**
 * Las preferencias que gobiernan como se arman los ciclos.
 *
 * <p>Se devuelven tambien los valores que no se pueden cambiar todavia —moneda,
 * idioma, zona— porque la interfaz los necesita para presentar cifras y fechas,
 * y pedirlos aparte seria una peticion mas para un dato que ya viaja aqui.
 */
public record UserPreferencesResponse(
        String currency,
        String locale,
        String timezone,
        String budgetCycleType,
        int cycleAnchorDay,
        String expenseAllocationPolicy) {

    public static UserPreferencesResponse from(UserPreferences preferences) {
        return new UserPreferencesResponse(
                preferences.getCurrency(),
                preferences.getLocale(),
                preferences.getTimezone(),
                preferences.getBudgetCycleType().name(),
                preferences.getCycleAnchorDay(),
                preferences.getExpenseAllocationPolicy().name());
    }
}
