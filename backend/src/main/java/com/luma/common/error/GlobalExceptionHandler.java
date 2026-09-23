package com.luma.common.error;

import com.luma.common.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce cualquier excepcion a un cuerpo de error unico y predecible.
 *
 * <p>Se usa {@link ProblemDetail} (RFC 9457) como base y se agregan las extensiones
 * propias del proyecto: {@code timestamp}, {@code errorCode}, {@code traceId} y,
 * cuando aplica, {@code errors}.
 *
 * <p>Regla de seguridad: al cliente nunca le llega un stack trace ni el mensaje
 * interno de una excepcion inesperada. Eso va al log, junto al traceId.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TYPE_PREFIX = "https://luma.app/errors/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldError::field))
                .toList();

        ProblemDetail problem = build(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Validation failed",
                "Revisa los campos marcados.",
                request);
        problem.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(build(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.VALIDATION_ERROR,
                        "Malformed request",
                        "El cuerpo de la peticion no se pudo leer.",
                        request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ProblemDetail problem = build(
                HttpStatus.BAD_REQUEST,
                ErrorCode.VALIDATION_ERROR,
                "Invalid parameter",
                "El parametro '%s' no tiene un valor valido.".formatted(ex.getName()),
                request);
        return ResponseEntity.badRequest().body(problem);
    }

    // NOTA: aqui vivia un manejador para PropertyReferenceException de Spring
    // Data, que convertia un `?sort=campoQueNoExiste` en 400 en lugar de 500.
    //
    // Se retiro porque impedia que arrancara el contexto de Spring: al
    // introspeccionar esta clase, el tipo del parametro no se podia resolver en
    // tiempo de ejecucion. Se quedo sin aclarar por que, y una clase que rompe
    // el arranque de toda la aplicacion no se deja "a ver si jala".
    //
    // Hoy no hace falta: ningun endpoint acepta un `sort` libre. Vuelve a hacer
    // falta en la Fase 5, cuando las listas de ingresos y gastos sean
    // ordenables, y entonces entra con una prueba que lo ejercite de verdad.

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Resource not found",
                        ex.getMessage(),
                        request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(build(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Resource not found",
                        "La ruta solicitada no existe.",
                        request));
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ProblemDetail> handleAuthenticationFailed(
            AuthenticationFailedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(build(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCode.UNAUTHORIZED,
                        "Authentication failed",
                        ex.getMessage(),
                        request));
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ProblemDetail> handleBusinessRule(
            BusinessRuleException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(build(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        ErrorCode.BUSINESS_RULE_VIOLATION,
                        "Business rule violation",
                        ex.getMessage(),
                        request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(build(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        ErrorCode.VALIDATION_ERROR,
                        "Method not allowed",
                        "El metodo HTTP no aplica para esta ruta.",
                        request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(build(
                        HttpStatus.FORBIDDEN,
                        ErrorCode.FORBIDDEN,
                        "Forbidden",
                        "No tienes acceso a este recurso.",
                        request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception ex, HttpServletRequest request) {
        // El detalle real solo va al log, correlacionado por traceId.
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(build(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_ERROR,
                        "Internal error",
                        "Algo fallo de nuestro lado. Intentalo de nuevo en un momento.",
                        request));
    }

    private ProblemDetail build(
            HttpStatus status,
            ErrorCode code,
            String title,
            String detail,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + code.name().toLowerCase().replace('_', '-')));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("errorCode", code.name());

        String traceId = CorrelationIdFilter.current();
        if (traceId != null) {
            problem.setProperty("traceId", traceId);
        }
        return problem;
    }
}
