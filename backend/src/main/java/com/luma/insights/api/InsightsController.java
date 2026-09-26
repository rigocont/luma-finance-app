package com.luma.insights.api;

import com.luma.insights.api.dto.FinancialInsightsResponse;
import com.luma.insights.application.InsightsService;
import com.luma.users.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El analisis financiero sin IA.
 *
 * <p>Un solo endpoint porque las tres senales comparten la misma lectura del
 * ciclo actual y de las categorias: separarlas en tres peticiones solo
 * multiplicaria las consultas sin que la interfaz necesite pedirlas por
 * separado.
 *
 * <p>La capa de redaccion y priorizacion con LLM (ver
 * docs/00-arquitectura-fase-0.md, S8.4) queda pendiente para una fase futura;
 * este controlador ya expone las cifras que esa capa, cuando exista, solo
 * tendria que interpretar.
 */
@RestController
@RequestMapping("/api/v1/insights")
@Tag(name = "Insights", description = "Analisis financiero determinista (sin IA)")
@SecurityRequirement(name = "bearerAuth")
public class InsightsController {

    private final InsightsService insights;
    private final CurrentUserService currentUser;

    public InsightsController(InsightsService insights, CurrentUserService currentUser) {
        this.insights = insights;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(
            summary = "El analisis financiero del usuario",
            description = """
                    Por que hay deficit (si lo hay), como repartir el remanente
                    entre las metas activas (si lo hay), y que categorias llevan
                    una racha de 3 ciclos seguidos al alza. Ninguna senal se
                    inventa: la que no tiene con que respaldarse, no aparece.""")
    public FinancialInsightsResponse current(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUser.requireId(jwt.getSubject());
        return FinancialInsightsResponse.from(insights.currentInsights(userId));
    }
}
