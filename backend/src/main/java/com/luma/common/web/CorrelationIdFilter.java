package com.luma.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Asigna un identificador de correlacion a cada peticion.
 *
 * <p>El mismo valor aparece en los logs ({@code %X{traceId}}), en la respuesta
 * ({@code X-Correlation-Id}) y dentro del cuerpo de error. Asi un usuario puede
 * reportar un problema con un identificador y ese identificador lleva directo a
 * la linea de log correspondiente.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /** Identificador de la peticion en curso, o {@code null} fuera de una peticion. */
    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
