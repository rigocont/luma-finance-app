package com.luma.users.application;

import com.luma.budget.domain.CycleType;
import com.luma.common.error.BusinessRuleException;
import com.luma.users.domain.UserPreferences;
import com.luma.users.infrastructure.UserPreferencesRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Preferencias del usuario, con una separacion deliberada entre leer y crear.
 *
 * <p>Las preferencias se crean de forma diferida y no al registrarse, para que
 * las cuentas creadas antes de que existiera esta tabla sigan funcionando sin
 * una migracion de relleno. Pero esa creacion diferida es una ESCRITURA, y
 * esconderla detras de un metodo que parece de lectura fue un error: llamada
 * desde una transaccion {@code readOnly}, la propagacion REQUIRED la une a esa
 * transaccion y la conexion rechaza el insert.
 *
 * <p>De ahi los dos metodos: {@link #find(Long)} no escribe nunca y se puede
 * llamar desde cualquier lectura; {@link #getOrCreate(Long)} escribe y solo se
 * llama desde un caso de uso que ya esta en una transaccion de escritura.
 */
@Service
public class UserPreferencesService {

    private final UserPreferencesRepository preferences;

    public UserPreferencesService(UserPreferencesRepository preferences) {
        this.preferences = preferences;
    }

    /** Las preferencias, si ya existen. No crea nada: se puede usar en una lectura. */
    @Transactional(readOnly = true)
    public Optional<UserPreferences> find(Long userId) {
        return preferences.findByUserId(userId);
    }

    /**
     * La moneda del usuario. LECTURA: no crea las preferencias.
     *
     * <p>Vive aqui y no en el modulo de presupuesto porque es una preferencia
     * del usuario, no un concepto presupuestal. Todo modulo que necesite
     * formatear dinero la pide aqui, sin tener que conocer al presupuesto.
     *
     * <p>Mientras no existan las preferencias se usa el mismo valor por omision
     * que {@link UserPreferences#defaultsFor(Long)}: la fila aparece la primera
     * vez que la persona abre un ciclo, que si es una escritura.
     */
    @Transactional(readOnly = true)
    public String currencyOf(Long userId) {
        return find(userId)
                .map(UserPreferences::getCurrency)
                .orElse(UserPreferences.DEFAULT_CURRENCY);
    }

    /**
     * Las preferencias del usuario, creandolas con los valores por omision si no
     * existen.
     *
     * <p>ESCRIBE. Solo debe llamarse desde un caso de uso de escritura; desde una
     * transaccion de solo lectura falla con "Connection is read-only".
     */
    @Transactional
    public UserPreferences getOrCreate(Long userId) {
        return preferences
                .findByUserId(userId)
                .orElseGet(() -> preferences.save(UserPreferences.defaultsFor(userId)));
    }

    /**
     * Cambia el tipo de ciclo y su dia de anclaje.
     *
     * <p>ESCRIBE. No toca los ciclos que ya existen: un ciclo abierto conserva
     * su periodo, y el tipo nuevo empieza a regir en el siguiente. Reescribir un
     * ciclo en curso cambiaria las fechas de un presupuesto que la persona ya
     * esta usando.
     */
    @Transactional
    public UserPreferences changeCycle(Long userId, CycleType cycleType, int anchorDay) {
        UserPreferences current = getOrCreate(userId);

        try {
            current.changeCycle(cycleType, anchorDay);
        } catch (IllegalArgumentException invalido) {
            // El dominio lanza IllegalArgumentException, que saldria como 500.
            // Un dia fuera de rango es una regla de negocio, no una falla: se
            // traduce para que responda 422 con su mensaje.
            throw new BusinessRuleException(invalido.getMessage());
        }

        return preferences.save(current);
    }

    /** Marca la fila como reiniciada a los valores por omision. */
    @Transactional
    public void resetCycleToDefaults(Long userId) {
        find(userId).ifPresent(current -> {
            UserPreferences defaults = UserPreferences.defaultsFor(userId);
            current.changeCycle(defaults.getBudgetCycleType(), defaults.getCycleAnchorDay());
            preferences.save(current);
        });
    }

    /**
     * Cambia el idioma de la interfaz.
     *
     * <p>ESCRIBE, y a proposito crea la fila si no existia (a diferencia de
     * {@link #changeCycle}, que tambien la crea): elegir un idioma es una
     * accion explicita de la persona, nunca un efecto secundario de una
     * lectura.
     */
    @Transactional
    public UserPreferences changeUiLanguage(Long userId, String uiLanguage) {
        UserPreferences current = getOrCreate(userId);

        try {
            current.changeUiLanguage(uiLanguage);
        } catch (IllegalArgumentException invalido) {
            throw new BusinessRuleException(invalido.getMessage());
        }

        return preferences.save(current);
    }
}
