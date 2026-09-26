package com.luma.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.common.error.BusinessRuleException;
import com.luma.support.IntegrationTest;
import com.luma.users.application.UserPreferencesService;
import com.luma.users.domain.User;
import com.luma.users.domain.UserPreferences;
import com.luma.users.infrastructure.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * El idioma de la interfaz (Fase 15) contra la base real.
 *
 * <p>Se guarda en la cuenta -no solo en el navegador- para que el idioma
 * elegido se recuerde tambien en otro dispositivo. Por eso vive aqui y no
 * solo en una prueba de dominio: lo que importa demostrar es que sobrevive a
 * guardarse y volver a leerse.
 */
@Transactional
@DisplayName("Idioma de la interfaz contra la base real")
class UserPreferencesIntegrationTest extends IntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    UserPreferencesService preferences;

    private Long userId;

    @BeforeEach
    void crearUsuario() {
        User user = users.save(User.register(
                "qa+" + UUID.randomUUID() + "@luma.app", "Persona de prueba", "hash-irrelevante"));
        userId = user.getId();
    }

    @Test
    @DisplayName("Sin preferencias todavia, el idioma por omision es espanol")
    void idiomaPorOmision() {
        assertThat(preferences.find(userId)).isEmpty();
        assertThat(UserPreferences.defaultsFor(userId).getUiLanguage())
                .isEqualTo(UserPreferences.DEFAULT_UI_LANGUAGE);
    }

    @Test
    @DisplayName("Cambiar el idioma lo guarda y sobrevive a volver a leerlo")
    void cambiarIdioma() {
        preferences.changeUiLanguage(userId, "en");

        UserPreferences guardado = preferences.find(userId).orElseThrow();
        assertThat(guardado.getUiLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("Cambiar el idioma crea la fila si todavia no existia")
    void cambiarIdiomaCreaLaFila() {
        assertThat(preferences.find(userId)).isEmpty();

        preferences.changeUiLanguage(userId, "en");

        assertThat(preferences.find(userId)).isPresent();
    }

    @Test
    @DisplayName("Un idioma sin catalogo de traducciones se rechaza con 422, no con un error generico")
    void idiomaNoSoportado() {
        assertThatThrownBy(() -> preferences.changeUiLanguage(userId, "fr"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
