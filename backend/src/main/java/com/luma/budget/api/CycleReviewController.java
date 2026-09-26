package com.luma.budget.api;

import com.luma.budget.api.dto.ConfirmAmountsRequest;
import com.luma.budget.api.dto.CycleItemResponse;
import com.luma.budget.api.dto.ItemHistorySummaryResponse;
import com.luma.budget.api.dto.ReviewItemResponse;
import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.application.CycleReviewService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.users.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * La revision del ciclo: los renglones cuyo monto todavia no se sabe.
 *
 * <p>Comparte la ruta base de los ciclos porque es parte del mismo recurso, y
 * vive en su propia clase porque responde otra pregunta: no como va el
 * presupuesto, sino que le falta a la persona por decidir.
 */
@RestController
@RequestMapping("/api/v1/budget-cycles")
@Tag(name = "Cycle review", description = "Revision de los montos de un ciclo")
@SecurityRequirement(name = "bearerAuth")
public class CycleReviewController {

    private final CycleReviewService review;
    private final BudgetCycleService cycles;
    private final CurrentUserService currentUser;

    public CycleReviewController(
            CycleReviewService review,
            BudgetCycleService cycles,
            CurrentUserService currentUser) {
        this.review = review;
        this.cycles = cycles;
        this.currentUser = currentUser;
    }

    @GetMapping("/current/review")
    @Operation(
            summary = "Lo que falta por revisar en el ciclo en curso",
            description = """
                    Los renglones en NEEDS_REVIEW, cada uno con lo que costo el
                    mismo gasto en el ciclo anterior.

                    Solo el ciclo en curso: uno cerrado es inmutable y no hay
                    nada que revisar en el. 404 si no hay ningun ciclo abierto
                    —que no es lo mismo que no tener nada pendiente, y por eso se
                    distingue de una lista vacia.
                    """)
    public List<ReviewItemResponse> currentReview(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = cycles.currencyOf(userId);

        BudgetCycle cycle = cycles.currentCycle(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "Ciclo en curso", "no hay ninguno abierto"));

        return review.pendingReview(cycle).stream()
                .map(item -> ReviewItemResponse.from(item, currency))
                .toList();
    }

    @GetMapping("/{cycleId}/items/{itemId}/history")
    @Operation(
            summary = "Lo que costo este gasto en ciclos anteriores",
            description = """
                    Del mas reciente al mas antiguo, hasta seis ciclos. Solo
                    aparecen los que se confirmaron: un monto planeado que nadie
                    confirmo es un plan, no lo que costo.

                    Incluye el promedio de lo confirmado, ya calculado: es una
                    cifra de dinero y el cliente no deriva ninguna.

                    Viene vacio si el renglon no salio de ninguna plantilla o si
                    es la primera vez que aparece.
                    """)
    public ItemHistorySummaryResponse history(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String cycleId,
            @PathVariable String itemId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = cycles.currencyOf(userId);
        BudgetCycle cycle = cycles.requireCycle(cycleId, userId);

        return ItemHistorySummaryResponse.from(review.historyOf(cycle, itemId), currency);
    }

    @PostMapping("/{cycleId}/items/confirm-amounts")
    @Operation(
            summary = "Fijar de una vez el monto de varios renglones",
            description = """
                    Confirmar el monto de un gasto variable lo saca de revision y
                    lo deja PENDIENTE: ya se sabe cuanto es, todavia no se ha
                    pagado. Para registrar el pago esta `/settle`.

                    Todo o nada: si un renglon del lote falla, no se confirma
                    ninguno. Un identificador repetido se rechaza en lugar de
                    quedarse con el ultimo en silencio.
                    """)
    public List<CycleItemResponse> confirmAmounts(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String cycleId,
            @Valid @RequestBody ConfirmAmountsRequest request) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = cycles.currencyOf(userId);
        BudgetCycle cycle = cycles.requireCycle(cycleId, userId);

        Map<String, Money> montos = new LinkedHashMap<>();

        for (ConfirmAmountsRequest.Entry entry : request.items()) {
            Money previo = montos.put(
                    entry.itemId(), Money.of(new BigDecimal(entry.amount()), currency));

            // Un mapa se quedaria con el ultimo sin decir nada. Dos montos para
            // el mismo renglon significa que el cliente armo mal el lote, y
            // elegir uno por el se veria como un monto perdido.
            if (previo != null) {
                throw new BusinessRuleException(
                        "El renglon " + entry.itemId() + " viene dos veces en el lote.");
            }
        }

        return review.confirmAmounts(cycle, montos).stream()
                .map(item -> CycleItemResponse.from(item, currency))
                .toList();
    }
}
