package com.luma.system.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico minimo para verificar que la pila completa responde.
 *
 * <p>Existe para que el frontend tenga algo real que consumir en la Fase 1 y para
 * que la validacion manual no dependa de Actuator. No expone nada sensible.
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "Informacion basica del servicio")
public class SystemController {

    private final Environment environment;
    private final String applicationName;
    private final String version;

    public SystemController(
            Environment environment,
            @Value("${spring.application.name:luma}") String applicationName,
            @Value("${info.app.version:0.1.0-SNAPSHOT}") String version) {
        this.environment = environment;
        this.applicationName = applicationName;
        this.version = version;
    }

    @GetMapping("/info")
    @Operation(summary = "Datos de identificacion del servicio en ejecucion")
    public SystemInfoResponse info() {
        return new SystemInfoResponse(
                applicationName,
                version,
                List.of(environment.getActiveProfiles()),
                Instant.now());
    }

    public record SystemInfoResponse(
            String name, String version, List<String> profiles, Instant serverTime) {}
}
