package com.luma.users.api;

import com.luma.users.api.dto.UpdateCyclePreferenceRequest;
import com.luma.users.api.dto.UserPreferencesResponse;
import com.luma.users.application.CurrentUserService;
import com.luma.users.application.UserPreferencesService;
import com.luma.users.domain.UserPreferences;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las preferencias de la propia cuenta.
 *
 * <p>La ruta es {@code /users/me} y no {@code /users/{id}}: no existe forma de
 * nombrar a otra persona, asi que tampoco hay forma de pedir sus preferencias.
 */
@RestController
@RequestMapping("/api/v1/users/me/preferences")
@Tag(name = "User preferences", description = "Como se arman tus ciclos")
@SecurityRequirement(name = "bearerAuth")
public class UserPreferencesController {

    private final UserPreferencesService preferences;
    private final CurrentUserService currentUser;

    public UserPreferencesController(
            UserPreferencesService preferences, CurrentUserService currentUser) {
        this.preferences = preferences;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(
            summary = "Tus preferencias",
            description = """
                    Si la cuenta todavia no tiene fila de preferencias, responde
                    los valores por omision sin crearla. Leer no escribe.
                    """)
    public UserPreferencesResponse get(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUser.requireId(jwt.getSubject());

        return preferences.find(userId)
                .map(UserPreferencesResponse::from)
                // Se arman en memoria, no se guardan: una lectura no crea filas.
                .orElseGet(() -> UserPreferencesResponse.from(
                        UserPreferences.defaultsFor(userId)));
    }

    @PatchMapping("/cycle")
    @Operation(
            summary = "Cambiar el tipo de ciclo",
            description = """
                    Aplica al SIGUIENTE ciclo. El que este abierto conserva su
                    periodo: reescribirlo cambiaria las fechas de un presupuesto
                    que ya estas usando.
                    """)
    public UserPreferencesResponse changeCycle(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCyclePreferenceRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());

        return UserPreferencesResponse.from(
                preferences.changeCycle(userId, request.cycleType(), request.anchorDay()));
    }
}
