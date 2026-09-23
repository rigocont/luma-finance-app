package com.luma.config;

import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El reloj como dependencia inyectable.
 *
 * <p>Ningun servicio llama a {@code LocalDate.now()} directamente. Con el reloj
 * inyectado, un test puede fijar la fecha y verificar el comportamiento del 31
 * de enero o del 29 de febrero sin esperar a que llegue ese dia.
 *
 * <p>En UTC: las fechas de calendario del usuario se resuelven con su zona
 * horaria en la capa que las presenta, no aqui.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
