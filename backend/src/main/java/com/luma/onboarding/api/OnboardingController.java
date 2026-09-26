package com.luma.onboarding.api;

import com.luma.budget.api.dto.CycleSummaryResponse;
import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.onboarding.api.dto.OnboardingStateResponse;
import com.luma.onboarding.application.OnboardingService;
import com.luma.users.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El alta guiada.
 *
 * <p>Tiene tres endpoints y ninguno recibe datos del presupuesto. Los ingresos,
 * los gastos y las metas se capturan por sus propios endpoints, los mismos que
 * usa el resto de la aplicacion: el asistente es un camino por la API, no una
 * API paralela. Asi no hay dos formas de crear un ingreso que puedan validar
 * distinto.
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@Tag(name = "Onboarding", description = "Configuracion inicial de una cuenta")
@SecurityRequirement(name = "bearerAuth")
public class OnboardingController {

    private final OnboardingService onboarding;
    private final BudgetCycleService cycles;
    private final CurrentUserService currentUser;

    public OnboardingController(
            OnboardingService onboarding,
            BudgetCycleService cycles,
            CurrentUserService currentUser) {
        this.onboarding = onboarding;
        this.cycles = cycles;
        this.currentUser = currentUser;
    }

    @GetMapping("/state")
    @Operation(
            summary = "Donde va el alta",
            description = """
                    Cuantos ingresos, gastos y metas lleva la cuenta, si ya
                    alcanza para terminar y a que paso conviene volver.

                    El paso se deduce de lo que hay, no de un avance guardado.
                    """)
    public OnboardingStateResponse state(@AuthenticationPrincipal Jwt jwt) {
        return OnboardingStateResponse.from(
                onboarding.stateOf(currentUser.requireId(jwt.getSubject())));
    }

    @PostMapping("/complete")
    @Operation(
            summary = "Terminar el alta y abrir el primer ciclo",
            description = """
                    Exige al menos un ingreso activo: sin el no hay presupuesto
                    que calcular y el resumen no diria nada. Responde 422 si
                    falta.

                    Marca la cuenta como configurada y abre el primer ciclo en la
                    misma transaccion, con los renglones ya materializados.
                    """)
    public CycleSummaryResponse complete(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUser.requireId(jwt.getSubject());

        BudgetCycle cycle = onboarding.complete(userId);

        return CycleSummaryResponse.from(
                cycle, cycles.balanceOf(cycle, cycles.currencyOf(userId)));
    }

    @PostMapping("/skip")
    @Operation(
            summary = "Hacerlo despues",
            description = """
                    Da el alta por terminada sin configurar nada y sin abrir
                    ciclo. Lo que se haya capturado se conserva.

                    Es idempotente: repetirlo sobre una cuenta ya configurada no
                    cambia nada ni falla.
                    """)
    public ResponseEntity<Void> skip(@AuthenticationPrincipal Jwt jwt) {
        onboarding.skip(currentUser.requireId(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }
}
