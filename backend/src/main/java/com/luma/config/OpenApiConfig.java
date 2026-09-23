package com.luma.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La documentacion de la API se genera desde el codigo. No existe un catalogo
 * de endpoints escrito a mano: {@code docs/api.md} solo describe convenciones.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI lumaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LUMA API")
                        .version("v1")
                        .description("""
                                API de finanzas personales de LUMA.

                                Toda la logica financiera vive en el backend: el cliente
                                recibe cifras ya calculadas y nunca las deriva por su cuenta.

                                Convenciones transversales en docs/api.md.

                                En desarrollo, los correos que envia la aplicacion
                                quedan en la bandeja de Mailpit: http://localhost:8025
                                """)
                        .contact(new Contact().name("LUMA"))
                        .license(new License().name("Propietario")))
                // Habilita el boton "Authorize" de Swagger UI para pegar el token.
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Pega aqui el accessToken que devuelve /auth/login")));
    }
}
