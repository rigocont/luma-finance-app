package com.luma.savings.api;

import com.luma.savings.api.dto.CreateSavingsGoalRequest;
import com.luma.savings.api.dto.RegisterMovementRequest;
import com.luma.savings.api.dto.ReorderGoalsRequest;
import com.luma.savings.api.dto.SavingsContributionResponse;
import com.luma.savings.api.dto.SavingsGoalResponse;
import com.luma.savings.api.dto.UpdateSavingsGoalRequest;
import com.luma.savings.application.SavingsGoalService;
import com.luma.savings.domain.ContributionType;
import com.luma.savings.domain.GoalStatus;
import com.luma.savings.domain.SavingsGoal;
import com.luma.users.application.CurrentUserService;
import com.luma.users.application.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Metas de ahorro.
 *
 * <p>No se pagina: una persona tiene metas, no cientos, y reordenarlas por
 * prioridad exige tenerlas todas a la vista.
 */
@RestController
@RequestMapping("/api/v1/savings-goals")
@Tag(name = "Savings goals", description = "Metas de ahorro y sus movimientos")
@SecurityRequirement(name = "bearerAuth")
public class SavingsGoalController {

    private final SavingsGoalService service;
    private final CurrentUserService currentUser;
    private final UserPreferencesService preferences;

    public SavingsGoalController(
            SavingsGoalService service,
            CurrentUserService currentUser,
            UserPreferencesService preferences) {
        this.service = service;
        this.currentUser = currentUser;
        this.preferences = preferences;
    }

    @PostMapping
    @Operation(
            summary = "Crear una meta",
            description = """
                    La meta nueva queda al final por prioridad. Si ya tienes un
                    ciclo abierto y la meta resta del presupuesto, su aporte
                    entra al ciclo en curso de inmediato.
                    """)
    public ResponseEntity<SavingsGoalResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateSavingsGoalRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        SavingsGoal goal = service.create(
                userId,
                request.name(),
                SavingsGoalService.parseAmount(request.target(), currency),
                request.targetDate(),
                request.mode(),
                SavingsGoalService.parseAmount(request.plannedPerCycle(), currency),
                request.icon(),
                request.color());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SavingsGoalResponse.from(goal, currency));
    }

    @GetMapping
    @Operation(summary = "Listar metas", description = "En orden de prioridad.")
    public List<SavingsGoalResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) GoalStatus status) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        return service.search(userId, status).stream()
                .map(goal -> SavingsGoalResponse.from(goal, currency))
                .toList();
    }

    @GetMapping("/{goalId}")
    @Operation(summary = "Una meta concreta")
    public SavingsGoalResponse byId(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String goalId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        return SavingsGoalResponse.from(
                service.require(userId, goalId), preferences.currencyOf(userId));
    }

    @GetMapping("/{goalId}/movements")
    @Operation(
            summary = "Los movimientos de una meta",
            description = """
                    Aportaciones y retiros, del mas reciente al mas antiguo. Los
                    retiros llevan monto negativo.
                    """)
    public List<SavingsContributionResponse> movements(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String goalId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        return service.movements(userId, goalId).stream()
                .map(movement -> SavingsContributionResponse.from(movement, currency))
                .toList();
    }

    @PatchMapping("/{goalId}")
    @Operation(summary = "Editar una meta", description = "Lo que no mandes no se toca.")
    public SavingsGoalResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String goalId,
            @Valid @RequestBody UpdateSavingsGoalRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        SavingsGoal goal = service.update(
                userId,
                goalId,
                request.name(),
                SavingsGoalService.parseAmount(request.target(), currency),
                request.touchesTargetDate(),
                request.clearTargetDate() ? null : request.targetDate(),
                request.mode(),
                SavingsGoalService.parseAmount(request.plannedPerCycle(), currency),
                request.icon(),
                request.color());

        return SavingsGoalResponse.from(goal, currency);
    }

    @PostMapping("/{goalId}/movements")
    @Operation(
            summary = "Registrar una aportacion o un retiro",
            description = """
                    El monto va siempre positivo: el signo lo decide el tipo. Un
                    retiro no puede dejar el progreso en negativo.

                    Los aportes que nacen del ciclo NO se registran aqui: salen
                    de confirmar el renglon de ahorro del ciclo.
                    """)
    public SavingsGoalResponse registerMovement(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String goalId,
            @Valid @RequestBody RegisterMovementRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        ContributionType type = request.type() == ContributionType.WITHDRAWAL
                ? ContributionType.WITHDRAWAL
                : ContributionType.EXTRA;

        SavingsGoal goal = service.registerMovement(
                userId,
                goalId,
                SavingsGoalService.parseAmount(request.amount(), currency),
                request.date(),
                type,
                null,
                request.notes());

        return SavingsGoalResponse.from(goal, currency);
    }

    @PutMapping("/order")
    @Operation(
            summary = "Reordenar las metas",
            description = """
                    Manda la lista COMPLETA de identificadores, de mayor a menor
                    prioridad. La prioridad decide a cual se le reparte primero
                    un remanente.
                    """)
    public List<SavingsGoalResponse> reorder(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReorderGoalsRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = preferences.currencyOf(userId);

        return service.reorder(userId, request.goalIds()).stream()
                .map(goal -> SavingsGoalResponse.from(goal, currency))
                .toList();
    }

    @PostMapping("/{goalId}/pause")
    @Operation(
            summary = "Pausar una meta",
            description = "Deja de restar del presupuesto, sin perder el progreso.")
    public SavingsGoalResponse pause(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String goalId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        return SavingsGoalResponse.from(
                service.setPaused(userId, goalId, true), preferences.currencyOf(userId));
    }

    @PostMapping("/{goalId}/resume")
    @Operation(summary = "Reanudar una meta pausada")
    public SavingsGoalResponse resume(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String goalId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        return SavingsGoalResponse.from(
                service.setPaused(userId, goalId, false), preferences.currencyOf(userId));
    }

    @DeleteMapping("/{goalId}")
    @Operation(
            summary = "Eliminar una meta",
            description = """
                    Borrado logico: se conserva para que los ciclos anteriores
                    sigan teniendo explicacion, pero desaparece de tus listas.
                    """)
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String goalId) {

        service.delete(currentUser.requireId(jwt.getSubject()), goalId);
        return ResponseEntity.noContent().build();
    }
}
