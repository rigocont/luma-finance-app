package com.luma.auth.api;

import com.luma.auth.api.dto.AuthResponse;
import com.luma.auth.api.dto.LoginRequest;
import com.luma.auth.api.dto.RegisterRequest;
import com.luma.auth.api.dto.UserResponse;
import com.luma.auth.application.AuthService;
import com.luma.auth.infrastructure.RefreshTokenCookies;
import com.luma.common.error.AuthenticationFailedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registro, sesion y renovacion")
public class AuthController {

    /**
     * Permite a un cliente movil pedir el token de renovacion en el cuerpo.
     * Cualquier otro valor, o su ausencia, se trata como cliente web y recibe cookie.
     */
    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    private static final String MOBILE_CLIENT = "mobile";

    private final AuthService authService;
    private final RefreshTokenCookies cookies;

    public AuthController(AuthService authService, RefreshTokenCookies cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @Operation(summary = "Crear una cuenta")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {

        AuthService.Session session = authService.register(
                request.email(), request.name(), request.password(), clientInfo(httpRequest));

        return sessionResponse(session, HttpStatus.CREATED, httpRequest);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        AuthService.Session session =
                authService.login(request.email(), request.password(), clientInfo(httpRequest));

        return sessionResponse(session, HttpStatus.OK, httpRequest);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar la sesion",
            description = """
                    Canjea el token de renovacion por un access token nuevo.

                    El token llega en la cookie HttpOnly y no hace falta enviarlo
                    a mano. Cada token sirve una sola vez: esta llamada emite uno
                    nuevo y revoca el anterior.
                    """)
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest httpRequest) {
        String rawToken = cookies.read(httpRequest)
                .or(() -> headerRefreshToken(httpRequest))
                .orElseThrow(AuthenticationFailedException::new);

        AuthService.Session session = authService.refresh(rawToken, clientInfo(httpRequest));
        return sessionResponse(session, HttpStatus.OK, httpRequest);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar la sesion de este dispositivo",
            description = "Idempotente: cerrar una sesion inexistente tambien responde 204.")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        cookies.read(httpRequest)
                .or(() -> headerRefreshToken(httpRequest))
                .ifPresent(authService::logout);

        // La cookie se borra siempre, incluso si el token ya no era valido.
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario de la sesion actual")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        // El sujeto del token es el publicId. Se relee de la base en lugar de
        // confiar en los claims: el token pudo emitirse antes de un cambio.
        return UserResponse.from(authService.requireByPublicId(jwt.getSubject()));
    }

    private ResponseEntity<AuthResponse> sessionResponse(
            AuthService.Session session, HttpStatus status, HttpServletRequest httpRequest) {

        ResponseCookie cookie = cookies.create(session.refreshToken());

        AuthResponse body = isMobileClient(httpRequest)
                ? AuthResponse.mobileSession(session)
                : AuthResponse.webSession(session);

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    private boolean isMobileClient(HttpServletRequest request) {
        return MOBILE_CLIENT.equalsIgnoreCase(request.getHeader(CLIENT_TYPE_HEADER));
    }

    /** Un cliente movil manda el token de renovacion por cabecera, no por cookie. */
    private java.util.Optional<String> headerRefreshToken(HttpServletRequest request) {
        String value = request.getHeader("X-Refresh-Token");
        return StringUtils.hasText(value) ? java.util.Optional.of(value) : java.util.Optional.empty();
    }

    private AuthService.ClientInfo clientInfo(HttpServletRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent != null && userAgent.length() > 255) {
            userAgent = userAgent.substring(0, 255);
        }
        return new AuthService.ClientInfo(userAgent, request.getRemoteAddr());
    }
}
