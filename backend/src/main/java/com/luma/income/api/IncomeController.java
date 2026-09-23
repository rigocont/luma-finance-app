package com.luma.income.api;

import com.luma.common.web.PageResponse;
import com.luma.income.api.dto.CreateIncomeRequest;
import com.luma.income.api.dto.IncomeResponse;
import com.luma.income.api.dto.IncomeSort;
import com.luma.income.api.dto.UpdateIncomeRequest;
import com.luma.income.application.IncomeService;
import com.luma.income.domain.Income;
import com.luma.income.domain.IncomeType;
import com.luma.users.application.CurrentUserService;
import com.luma.users.application.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingresos del usuario.
 *
 * <p>Ningun endpoint recibe un identificador de usuario: siempre sale del token.
 * Los ingresos se identifican por UUID, nunca por el id secuencial interno.
 */
@RestController
@RequestMapping("/api/v1/incomes")
@Tag(name = "Incomes", description = "Ingresos recurrentes y extraordinarios")
@SecurityRequirement(name = "bearerAuth")
public class IncomeController {

    /** Tope de pagina. Sin tope, un cliente puede pedir toda la tabla de una vez. */
    private static final int MAX_PAGE_SIZE = 100;

    private final IncomeService service;
    private final CurrentUserService currentUser;
    private final UserPreferencesService preferences;

    public IncomeController(
            IncomeService service,
            CurrentUserService currentUser,
            UserPreferencesService preferences) {
        this.service = service;
        this.currentUser = currentUser;
        this.preferences = preferences;
    }

    @PostMapping
    @Operation(
            summary = "Capturar un ingreso",
            description = """
                    Si ya tienes un ciclo abierto y el ingreso cae dentro del
                    periodo, entra al ciclo en curso de inmediato. Los ingresos de
                    monto variable entran pidiendo confirmacion.
                    """)
    public ResponseEntity<IncomeResponse> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateIncomeRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        Income income = service.create(
                userId,
                request.name(),
                request.type(),
                IncomeService.parseAmount(request.amount(), currency),
                request.frequency(),
                request.expectedDay(),
                request.startDate(),
                request.endDate(),
                request.notes());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(IncomeResponse.from(income, currency));
    }

    @GetMapping
    @Operation(
            summary = "Listar ingresos",
            description = """
                    Se puede filtrar por tipo y por si esta activo. Los eliminados
                    nunca aparecen. El orden se elige de una lista cerrada de
                    opciones, no con un nombre de campo libre.
                    """)
    public PageResponse<IncomeResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) IncomeType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "NEWEST") IncomeSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort.toSort());

        return PageResponse.from(
                service.search(userId, type, active, pageable),
                income -> IncomeResponse.from(income, currency));
    }

    @GetMapping("/{incomeId}")
    @Operation(summary = "Un ingreso concreto")
    public IncomeResponse byId(@AuthenticationPrincipal Jwt jwt, @PathVariable String incomeId) {
        Long userId = currentUser.requireId(jwt.getSubject());
        return IncomeResponse.from(service.require(userId, incomeId), preferences.currencyOf(userId));
    }

    @PatchMapping("/{incomeId}")
    @Operation(
            summary = "Editar un ingreso",
            description = """
                    Lo que no mandes no se toca. Editar NO altera los ciclos ya
                    abiertos: sus renglones son copias del momento en que se
                    generaron. El cambio aplica desde el siguiente ciclo.
                    """)
    public IncomeResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String incomeId,
            @Valid @RequestBody UpdateIncomeRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        Income income = service.update(
                userId,
                incomeId,
                request.name(),
                request.type(),
                IncomeService.parseAmount(request.amount(), currency),
                request.touchesSchedule(),
                request.frequency(),
                request.expectedDay(),
                request.startDate(),
                request.endDate(),
                request.notes());

        return IncomeResponse.from(income, currency);
    }

    @PostMapping("/{incomeId}/activate")
    @Operation(summary = "Volver a contarlo en los ciclos siguientes")
    public IncomeResponse activate(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String incomeId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        return IncomeResponse.from(
                service.setActive(userId, incomeId, true), preferences.currencyOf(userId));
    }

    @PostMapping("/{incomeId}/deactivate")
    @Operation(
            summary = "Dejar de contarlo",
            description = """
                    Distinto de eliminar: el ingreso se conserva y se puede
                    reactivar. Deja de entrar en los ciclos que se abran, y no
                    toca los que ya existen.
                    """)
    public IncomeResponse deactivate(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String incomeId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        return IncomeResponse.from(
                service.setActive(userId, incomeId, false), preferences.currencyOf(userId));
    }

    @DeleteMapping("/{incomeId}")
    @Operation(
            summary = "Eliminar un ingreso",
            description = """
                    Borrado logico: la fila se conserva para que los ciclos
                    anteriores sigan teniendo explicacion, pero el ingreso
                    desaparece de tus listas y no se puede reactivar.
                    """)
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String incomeId) {

        service.delete(currentUser.requireId(jwt.getSubject()), incomeId);
        return ResponseEntity.noContent().build();
    }
}
