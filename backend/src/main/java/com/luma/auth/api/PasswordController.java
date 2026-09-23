package com.luma.auth.api;

import com.luma.auth.api.dto.ChangePasswordRequest;
import com.luma.auth.api.dto.ForgotPasswordRequest;
import com.luma.auth.api.dto.ResetPasswordRequest;
import com.luma.auth.application.PasswordService;
import com.luma.auth.infrastructure.RefreshTokenCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
@Tag(name = "Password", description = "Recuperacion y cambio de contrasena")
public class PasswordController {

    private final PasswordService passwordService;
    private final RefreshTokenCookies cookies;

    public PasswordController(PasswordService passwordService, RefreshTokenCookies cookies) {
        this.passwordService = passwordService;
        this.cookies = cookies;
    }

    @PostMapping("/forgot")
    @Operation(
            summary = "Pedir un enlace de recuperacion",
            description = """
                    Responde 202 siempre, exista o no una cuenta con ese correo.

                    Si respondiera distinto en cada caso, cualquiera podria
                    averiguar quien tiene cuenta en LUMA probando correos.
                    """)
    public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset")
    @Operation(
            summary = "Elegir una contrasena nueva con el codigo del enlace",
            description = "Cierra todas las sesiones abiertas del usuario.")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request.token(), request.newPassword());

        // La sesion de este navegador tambien queda cerrada: la cookie sobra.
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    @PostMapping("/change")
    @Operation(
            summary = "Cambiar la contrasena estando dentro",
            description = """
                    Exige la contrasena actual y cierra todas las sesiones,
                    incluida la de este dispositivo.
                    """)
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> change(
            @Valid @RequestBody ChangePasswordRequest request, @AuthenticationPrincipal Jwt jwt) {

        passwordService.changePassword(
                jwt.getSubject(), request.currentPassword(), request.newPassword());

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }
}
