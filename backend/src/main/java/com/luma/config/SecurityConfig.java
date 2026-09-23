package com.luma.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Seguridad base de la Fase 1.
 *
 * <p>Todavia no hay autenticacion real: eso llega en la Fase 2 (JWT + refresh token).
 * Lo que si queda fijado desde ahora es la postura: sin sesion, sin formulario de
 * login, sin basic auth, y todo endpoint cerrado salvo los que se abren explicitamente.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Rutas publicas mientras no exista autenticacion. */
    private static final String[] PUBLIC_PATHS = {
        "/actuator/health",
        "/actuator/health/**",
        "/actuator/info",
        "/v3/api-docs",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/api/v1/system/**",
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        // Se autentican con la cookie de renovacion, no con el access token:
        // justamente se usan cuando el access token ya no sirve.
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        // Recuperacion: por definicion se usan sin poder iniciar sesion.
        "/api/v1/auth/password/forgot",
        "/api/v1/auth/password/reset"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // No se inyecta CorsConfigurationSource por tipo: Spring MVC registra su
        // propio mvcHandlerMappingIntrospector, que tambien implementa esa
        // interfaz, y la inyeccion quedaria ambigua. Con withDefaults(), Spring
        // Security busca el bean llamado exactamente `corsConfigurationSource`.
        http.cors(Customizer.withDefaults())
                // La API es stateless y se autentica por token: no hay cookie de sesion
                // que proteger con CSRF. Si en la Fase 2 el refresh token viaja en
                // cookie HttpOnly, hay que revisar esta decision para esa ruta.
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                // El token se valida con el soporte nativo de resource server:
                // sin filtro propio que mantener. El JwtDecoder lo aporta JwtConfig.
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * BCrypt con coste 12. Es el algoritmo recomendado por Spring Security y el
     * coste da un margen razonable frente a hardware actual sin castigar el login.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(LumaProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Correlation-Id"));
        // Necesario para la estrategia de refresh token en cookie HttpOnly (Fase 2).
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
