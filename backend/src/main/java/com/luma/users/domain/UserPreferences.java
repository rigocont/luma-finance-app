package com.luma.users.domain;

import com.luma.budget.domain.CycleType;
import com.luma.budget.domain.ExpenseAllocationPolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;

/**
 * Preferencias del usuario.
 *
 * <p>Aqui vive la configuracion que gobierna como se arman sus ciclos: tipo,
 * dia de anclaje y politica de asignacion de gastos.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreferences {

    public static final String DEFAULT_CURRENCY = "MXN";
    public static final String DEFAULT_LOCALE = "es-MX";
    public static final String DEFAULT_TIMEZONE = "America/Mexico_City";
    public static final String DEFAULT_UI_LANGUAGE = "es";
    private static final Set<String> SUPPORTED_UI_LANGUAGES = Set.of("es", "en");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 10)
    private String locale;

    /** "es" o "en". Separado de {@code locale}: gobierna el idioma de la
     *  interfaz, no el formato de numeros y fechas -aunque hoy el frontend
     *  deriva uno del otro-. */
    @Column(name = "ui_language", nullable = false, length = 2)
    private String uiLanguage;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_cycle_type", nullable = false, length = 16)
    private CycleType budgetCycleType;

    @Column(name = "cycle_anchor_day", nullable = false)
    private int cycleAnchorDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_allocation_policy", nullable = false, length = 16)
    private ExpenseAllocationPolicy expenseAllocationPolicy;

    @Column(nullable = false, length = 10)
    private String theme;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    protected UserPreferences() {
        // Requerido por JPA.
    }

    /** Preferencias por omision para una cuenta nueva. */
    public static UserPreferences defaultsFor(Long userId) {
        UserPreferences preferences = new UserPreferences();
        preferences.userId = userId;
        preferences.currency = DEFAULT_CURRENCY;
        preferences.locale = DEFAULT_LOCALE;
        preferences.uiLanguage = DEFAULT_UI_LANGUAGE;
        preferences.timezone = DEFAULT_TIMEZONE;
        preferences.budgetCycleType = CycleType.BIWEEKLY;
        preferences.cycleAnchorDay = 1;
        preferences.expenseAllocationPolicy = ExpenseAllocationPolicy.BY_DUE_DATE;
        preferences.theme = "SYSTEM";
        return preferences;
    }

    public void changeCycle(CycleType cycleType, int anchorDay) {
        if (anchorDay < 1 || anchorDay > 31) {
            throw new IllegalArgumentException("El dia de anclaje debe estar entre 1 y 31");
        }
        this.budgetCycleType = cycleType;
        this.cycleAnchorDay = anchorDay;
    }

    /** Solo "es" o "en": cualquier otro valor no tiene catalogo de traducciones. */
    public void changeUiLanguage(String uiLanguage) {
        if (uiLanguage == null || !SUPPORTED_UI_LANGUAGES.contains(uiLanguage)) {
            throw new IllegalArgumentException("El idioma debe ser 'es' o 'en'");
        }
        this.uiLanguage = uiLanguage;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCurrency() {
        return currency;
    }

    public String getLocale() {
        return locale;
    }

    public String getUiLanguage() {
        return uiLanguage;
    }

    public String getTimezone() {
        return timezone;
    }

    public CycleType getBudgetCycleType() {
        return budgetCycleType;
    }

    public int getCycleAnchorDay() {
        return cycleAnchorDay;
    }

    public ExpenseAllocationPolicy getExpenseAllocationPolicy() {
        return expenseAllocationPolicy;
    }

    public String getTheme() {
        return theme;
    }
}
