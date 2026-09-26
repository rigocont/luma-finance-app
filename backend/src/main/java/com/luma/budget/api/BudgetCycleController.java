package com.luma.budget.api;

import com.luma.budget.api.dto.BudgetBalanceResponse;
import com.luma.budget.api.dto.BudgetCycleResponse;
import com.luma.budget.api.dto.CycleItemResponse;
import com.luma.budget.api.dto.CycleSummaryResponse;
import com.luma.budget.api.dto.CycleTrendResponse;
import com.luma.budget.api.dto.DeficitAdviceResponse;
import com.luma.budget.api.dto.SettleItemRequest;
import com.luma.budget.api.dto.UpdateItemRequest;
import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.application.DeficitAdviceService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.ItemStatus;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.common.web.PageResponse;
import com.luma.users.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
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
 * Ciclos presupuestales.
 *
 * <p>Ningun endpoint recibe un identificador de usuario: siempre sale del token.
 * Asi no hay forma de leer ni tocar el presupuesto de otra persona.
 */
@RestController
@RequestMapping("/api/v1/budget-cycles")
@Tag(name = "Budget cycles", description = "Ciclos presupuestales y motor de balance")
@SecurityRequirement(name = "bearerAuth")
public class BudgetCycleController {

    /** Tope de pagina. Sin tope, un cliente puede pedir toda la tabla de una vez. */
    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Cuantos ciclos se pueden comparar de una vez.
     *
     * <p>Doce: un ano de ciclos mensuales o medio ano de quincenales. Mas que eso
     * no cabe legible en una grafica, y el tope evita que un cliente pida el
     * historial entero por accidente.
     */
    private static final int MAX_TREND_CYCLES = 12;

    private final BudgetCycleService service;
    private final DeficitAdviceService deficitAdvice;
    private final CurrentUserService currentUser;

    public BudgetCycleController(
            BudgetCycleService service,
            DeficitAdviceService deficitAdvice,
            CurrentUserService currentUser) {
        this.service = service;
        this.deficitAdvice = deficitAdvice;
        this.currentUser = currentUser;
    }

    @GetMapping("/current")
    @Operation(
            summary = "El ciclo en curso con su balance",
            description = "404 si todavia no hay ningun ciclo abierto.")
    public CycleSummaryResponse current(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = service.currencyOf(userId);

        BudgetCycle cycle = service.currentCycle(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "Ciclo en curso", "no hay ninguno abierto"));

        return CycleSummaryResponse.from(cycle, service.balanceOf(cycle, currency));
    }

    @PostMapping
    @Operation(
            summary = "Abrir el siguiente ciclo",
            description = """
                    Genera el ciclo y materializa sus renglones a partir de los
                    ingresos, gastos y metas activas.

                    Si el ciclo en curso ya termino su periodo, se cierra solo. Si
                    no ha terminado, la peticion se rechaza: abrir el siguiente
                    seria saltarse el actual.
                    """)
    public ResponseEntity<CycleSummaryResponse> open(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = service.currencyOf(userId);

        BudgetCycle cycle = service.openNextCycle(userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CycleSummaryResponse.from(cycle, service.balanceOf(cycle, currency)));
    }

    @GetMapping
    @Operation(
            summary = "Historial de ciclos",
            description = """
                    Del mas reciente al mas antiguo. El orden no es configurable:
                    es una decision de producto, no una preferencia del cliente.
                    """)
    public PageResponse<BudgetCycleResponse> history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = currentUser.requireId(jwt.getSubject());

        // Se reciben dos enteros y no un Pageable a proposito. Un Pageable expone
        // ademas un parametro `sort` libre, y un nombre de campo que no existe
        // revienta la consulta con un 500 en lugar de un 400: el cliente puede
        // tumbar el endpoint con un dato invalido. Aqui el orden ya lo fija la
        // consulta, asi que ese parametro no tenia nada que aportar.
        Pageable pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        return PageResponse.from(service.history(userId, pageable), BudgetCycleResponse::from);
    }

    @GetMapping("/{cycleId}")
    @Operation(summary = "Un ciclo concreto")
    public BudgetCycleResponse byId(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String cycleId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        return BudgetCycleResponse.from(service.requireCycle(cycleId, userId));
    }

    @GetMapping("/{cycleId}/balance")
    @Operation(summary = "El balance de un ciclo")
    public BudgetBalanceResponse balance(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String cycleId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        BudgetCycle cycle = service.requireCycle(cycleId, userId);

        return BudgetBalanceResponse.from(
                service.balanceOf(cycle, service.currencyOf(userId)));
    }

    @GetMapping("/{cycleId}/advice")
    @Operation(
            summary = "De donde podria salir lo que falta",
            description = """
                    Con el ciclo en deficit, los renglones de los que se puede
                    recortar, en el orden en que conviene mirarlos: primero los
                    gastos flexibles, luego los ahorros del menos prioritario al
                    mas, y al final los importantes.

                    Los gastos CRITICOS no aparecen nunca. No es una heuristica:
                    es lo que la aplicacion promete al capturarlos.

                    Sin deficit responde `missing: 0` y la lista vacia. No
                    modifica nada: quien decide es la persona.
                    """)
    public DeficitAdviceResponse advice(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String cycleId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = service.currencyOf(userId);
        BudgetCycle cycle = service.requireCycle(cycleId, userId);

        return DeficitAdviceResponse.from(
                deficitAdvice.adviceFor(cycle, currency), currency);
    }

    @GetMapping("/trends")
    @Operation(
            summary = "Comparar los ultimos ciclos",
            description = """
                    Del MAS ANTIGUO al mas reciente, al contrario del historial:
                    una comparacion se lee como linea de tiempo, no como lista.

                    Cada ciclo trae sus totales presupuestados y reales ya
                    calculados. El cliente presenta; no deriva ninguna cifra.
                    """)
    public List<CycleTrendResponse> trends(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "6") int cycles) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = service.currencyOf(userId);

        int cuantos = Math.min(Math.max(cycles, 1), MAX_TREND_CYCLES);

        return service.trends(userId, cuantos, currency).stream()
                .map(CycleTrendResponse::from)
                .toList();
    }

    @GetMapping("/{cycleId}/items")
    @Operation(
            summary = "Los renglones de un ciclo",
            description = "Se puede filtrar por tipo o por estado, no por ambos a la vez.")
    public List<CycleItemResponse> items(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String cycleId,
            @RequestParam(required = false) CycleItemType type,
            @RequestParam(required = false) ItemStatus status) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = service.currencyOf(userId);
        BudgetCycle cycle = service.requireCycle(cycleId, userId);

        return service.itemsOf(cycle, type, status).stream()
                .map(item -> CycleItemResponse.from(item, currency))
                .toList();
    }

    @PatchMapping("/{cycleId}/items/{itemId}")
    @Operation(
            summary = "Ajustar un renglon",
            description = "Confirmar el monto de un gasto variable lo saca de revision.")
    public CycleItemResponse updateItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String cycleId,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateItemRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = service.currencyOf(userId);
        BudgetCycle cycle = service.requireCycle(cycleId, userId);

        Money plannedAmount = request.plannedAmount() != null
                ? Money.of(new BigDecimal(request.plannedAmount()), currency)
                : null;

        return CycleItemResponse.from(
                service.updateItem(
                        cycle, itemId, plannedAmount, request.displayOrder(), request.notes()),
                currency);
    }

    @PostMapping("/{cycleId}/items/{itemId}/settle")
    @Operation(
            summary = "Confirmar que un renglon ocurrio",
            description = """
                    En un renglon de ahorro, el monto se registra ademas como
                    aporte a la meta y sube su progreso. Manda
                    `registerInGoal: false` si apartaste el dinero pero no
                    quieres que cuente en la meta.
                    """)
    public CycleItemResponse settleItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String cycleId,
            @PathVariable String itemId,
            @Valid @RequestBody SettleItemRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = service.currencyOf(userId);
        BudgetCycle cycle = service.requireCycle(cycleId, userId);

        Money actual = Money.of(new BigDecimal(request.actualAmount()), currency);

        return CycleItemResponse.from(
                service.settleItem(
                        cycle,
                        itemId,
                        actual,
                        request.settledOn(),
                        request.shouldRegisterInGoal()),
                currency);
    }

    @DeleteMapping("/{cycleId}/items/{itemId}")
    @Operation(
            summary = "Quitar un renglon de este ciclo",
            description = """
                    No borra nada ni desactiva la plantilla: marca el renglon como
                    omitido y deja de contar en el presupuesto. Omitir es distinto
                    de no haber pagado.
                    """)
    public CycleItemResponse skipItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String cycleId,
            @PathVariable String itemId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = service.currencyOf(userId);
        BudgetCycle cycle = service.requireCycle(cycleId, userId);

        return CycleItemResponse.from(service.skipItem(cycle, itemId), currency);
    }

    @PostMapping("/{cycleId}/items/{itemId}/reopen")
    @Operation(summary = "Dejar un renglon pendiente de nuevo")
    public CycleItemResponse reopenItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String cycleId,
            @PathVariable String itemId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = service.currencyOf(userId);
        BudgetCycle cycle = service.requireCycle(cycleId, userId);

        return CycleItemResponse.from(service.reopenItem(cycle, itemId), currency);
    }

    @PostMapping("/{cycleId}/close")
    @Operation(
            summary = "Cerrar un ciclo",
            description = "Un ciclo cerrado es inmutable: ningun renglon se puede tocar despues.")
    public BudgetCycleResponse close(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String cycleId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        BudgetCycle cycle = service.requireCycle(cycleId, userId);

        return BudgetCycleResponse.from(service.closeCycle(cycle));
    }
}
