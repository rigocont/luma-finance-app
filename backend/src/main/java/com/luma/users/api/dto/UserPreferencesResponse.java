package com.luma.users.api.dto;

import com.luma.users.domain.UserPreferences;

/**
 * Las preferencias que gobiernan como se arman los ciclos.
 *
 * <p>Se devuelven tambien valores que no todos se pueden cambiar desde aqui
 * -moneda y zona, por ejemplo- porque la interfaz los necesita para presentar
 * cifras y fechas, y pedirlos aparte seria una peticion mas para un dato que
 * ya viaja aqui. {@code uiLanguage} si se puede cambiar (ver
 * {@code PATCH .../preferences/language}).
 */
public record UserPreferencesResponse(
        String currency,
        String locale,
        String uiLanguage,
        String timezone,
        String budgetCycleType,
        int cycleAnchorDay,
        String expenseAllocationPolicy) {

    public static UserPreferencesResponse from(UserPreferences preferences) {
        return new UserPreferencesResponse(
                preferences.getCurrency(),
                preferences.getLocale(),
                preferences.getUiLanguage(),
                preferences.getTimezone(),
                preferences.getBudgetCycleType().name(),
                preferences.getCycleAnchorDay(),
                preferences.getExpenseAllocationPolicy().name());
    }
}
