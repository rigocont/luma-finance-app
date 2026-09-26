package com.luma.expenses.api;

import com.luma.common.web.PageResponse;
import com.luma.expenses.api.dto.CreateExpenseRequest;
import com.luma.expenses.api.dto.ExpenseCategoryResponse;
import com.luma.expenses.api.dto.ExpenseResponse;
import com.luma.expenses.api.dto.ExpenseSort;
import com.luma.expenses.api.dto.UpdateExpenseRequest;
import com.luma.expenses.application.ExpenseService;
import com.luma.expenses.domain.Expense;
import com.luma.expenses.domain.ExpenseCategory;
import com.luma.expenses.domain.ExpenseKind;
import com.luma.users.application.CurrentUserService;
import com.luma.users.application.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
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
 * Gastos, fijos y variables.
 *
 * <p>Un solo recurso para ambos: lo unico que los distingue es {@code kind}, y
 * separarlos en dos rutas duplicaria el contrato completo por un campo.
 *
 * <p>Ningun endpoint recibe un identificador de usuario: siempre sale del token.
 */
@RestController
@RequestMapping("/api/v1/expenses")
@Tag(name = "Expenses", description = "Gastos fijos y variables")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    /** Tope de pagina. Sin tope, un cliente puede pedir toda la tabla de una vez. */
    private static final int MAX_PAGE_SIZE = 100;

    private final ExpenseService service;
    private final CurrentUserService currentUser;
    private final UserPreferencesService preferences;

    public ExpenseController(
            ExpenseService service,
            CurrentUserService currentUser,
            UserPreferencesService preferences) {
        this.service = service;
        this.currentUser = currentUser;
        this.preferences = preferences;
    }

    @PostMapping
    @Operation(
            summary = "Capturar un gasto",
            description = """
                    Si ya tienes un ciclo abierto y el gasto cae dentro del
                    periodo, entra al ciclo en curso de inmediato. Los gastos
                    variables entran pidiendo confirmacion del monto.
                    """)
    public ResponseEntity<ExpenseResponse> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateExpenseRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        Expense expense = service.create(
                userId,
                request.categoryId(),
                request.name(),
                request.kind(),
                ExpenseService.parseAmount(request.amount(), currency),
                request.frequency(),
                request.dueDay(),
                request.flexibility(),
                request.startDate(),
                request.endDate(),
                request.notes());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(responder(userId, expense, currency));
    }

    @GetMapping
    @Operation(
            summary = "Listar gastos",
            description = """
                    Se puede filtrar por tipo (fijo o variable), por categoria y
                    por si esta activo. Los eliminados nunca aparecen.
                    """)
    public PageResponse<ExpenseResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ExpenseKind kind,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "NEWEST") ExpenseSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);
        Map<Long, ExpenseCategoryResponse> categorias = indiceDeCategorias(userId);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort.toSort());

        return PageResponse.from(
                service.search(userId, kind, categoryId, active, pageable),
                expense -> ExpenseResponse.from(
                        expense, categorias.get(expense.getCategoryId()), currency));
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Un gasto concreto")
    public ExpenseResponse byId(@AuthenticationPrincipal Jwt jwt, @PathVariable String expenseId) {
        Long userId = currentUser.requireId(jwt.getSubject());

        return responder(
                userId, service.require(userId, expenseId), preferences.currencyOf(userId));
    }

    @PatchMapping("/{expenseId}")
    @Operation(
            summary = "Editar un gasto",
            description = """
                    Lo que no mandes no se toca. Editar NO altera los ciclos ya
                    abiertos: sus renglones son copias del momento en que se
                    generaron. El cambio aplica desde el siguiente ciclo.
                    """)
    public ExpenseResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String expenseId,
            @Valid @RequestBody UpdateExpenseRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        Expense expense = service.update(
                userId,
                expenseId,
                request.name(),
                request.kind(),
                ExpenseService.parseAmount(request.amount(), currency),
                request.touchesCategory(),
                request.clearCategory() ? null : request.categoryId(),
                request.flexibility(),
                request.touchesSchedule(),
                request.frequency(),
                request.dueDay(),
                request.startDate(),
                request.endDate(),
                request.notes());

        return responder(userId, expense, currency);
    }

    @PostMapping("/{expenseId}/activate")
    @Operation(summary = "Volver a contarlo en los ciclos siguientes")
    public ExpenseResponse activate(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String expenseId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        return responder(
                userId,
                service.setActive(userId, expenseId, true),
                preferences.currencyOf(userId));
    }

    @PostMapping("/{expenseId}/deactivate")
    @Operation(
            summary = "Dejar de contarlo",
            description = """
                    Distinto de eliminar: el gasto se conserva y se puede
                    reactivar. Deja de entrar en los ciclos que se abran, y no
                    toca los que ya existen.
                    """)
    public ExpenseResponse deactivate(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String expenseId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        return responder(
                userId,
                service.setActive(userId, expenseId, false),
                preferences.currencyOf(userId));
    }

    @DeleteMapping("/{expenseId}")
    @Operation(
            summary = "Eliminar un gasto",
            description = """
                    Borrado logico: la fila se conserva para que los ciclos
                    anteriores sigan teniendo explicacion, pero el gasto
                    desaparece de tus listas y no se puede reactivar.
                    """)
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String expenseId) {

        service.delete(currentUser.requireId(jwt.getSubject()), expenseId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Las categorias indexadas por su id interno.
     *
     * <p>Se carga el catalogo UNA vez por peticion en lugar de consultar la
     * categoria de cada gasto: son menos de veinte filas y evita el problema de
     * las N+1 consultas en una lista de cien gastos.
     */
    private Map<Long, ExpenseCategoryResponse> indiceDeCategorias(Long userId) {
        Map<Long, ExpenseCategoryResponse> indice = new HashMap<>();
        for (ExpenseCategory category : service.catalog(userId)) {
            indice.put(category.getId(), ExpenseCategoryResponse.from(category));
        }
        return indice;
    }

    private ExpenseResponse responder(Long userId, Expense expense, String currency) {
        ExpenseCategoryResponse category = expense.getCategoryId() == null
                ? null
                : indiceDeCategorias(userId).get(expense.getCategoryId());

        return ExpenseResponse.from(expense, category, currency);
    }
}
