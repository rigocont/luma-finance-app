package com.luma.expenses.api;

import com.luma.expenses.api.dto.ExpenseCategoryResponse;
import com.luma.expenses.application.ExpenseService;
import com.luma.users.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catalogo de categorias de gasto.
 *
 * <p>De solo lectura por ahora: la aplicacion todavia no deja crear categorias
 * propias. El esquema ya lo contempla, asi que cuando llegue no habra que
 * migrar nada.
 */
@RestController
@RequestMapping("/api/v1/expense-categories")
@Tag(name = "Expense categories", description = "Catalogo de categorias de gasto")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseCategoryController {

    private final ExpenseService service;
    private final CurrentUserService currentUser;

    public ExpenseCategoryController(ExpenseService service, CurrentUserService currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(
            summary = "Las categorias que puedes usar",
            description = """
                    Las del sistema mas las tuyas, ordenadas para mostrarse tal
                    cual. No se pagina: son pocas y la interfaz las necesita
                    todas para pintar un desplegable.
                    """)
    public List<ExpenseCategoryResponse> catalog(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUser.requireId(jwt.getSubject());

        return service.catalog(userId).stream().map(ExpenseCategoryResponse::from).toList();
    }
}
