package com.luma.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.expenses.application.ExpenseService;
import com.luma.expenses.domain.Expense;
import com.luma.expenses.domain.ExpenseCategory;
import com.luma.expenses.domain.ExpenseKind;
import com.luma.support.IntegrationTest;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Los gastos contra la base real, incluido el catalogo sembrado y la relacion
 * con el ciclo abierto.
 */
@Transactional
@DisplayName("Gastos contra la base real")
class ExpenseIntegrationTest extends IntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    ExpenseService expenses;

    @Autowired
    BudgetCycleService cycles;

    @Autowired
    CycleItemRepository items;

    private Long userId;

    @BeforeEach
    void crearUsuario() {
        User user = users.save(User.register(
                "qa+" + UUID.randomUUID() + "@luma.app", "Persona de prueba", "hash-irrelevante"));
        userId = user.getId();
    }

    /** Ver IncomeIntegrationTest: el dia 10 siempre cae en el ciclo quincenal. */
    private static final int DIA_SEGURO = 10;

    private static LocalDate inicioLejano() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusYears(1);
    }

    private Expense capturar(String nombre, ExpenseKind tipo, String monto, String categoriaId) {
        return expenses.create(
                userId, categoriaId, nombre, tipo, Money.of(monto),
                Frequency.BIWEEKLY, DIA_SEGURO, Flexibility.IMPORTANT,
                inicioLejano(), null, null);
    }

    private List<CycleItem> renglonesDeGasto(BudgetCycle cycle) {
        return items.findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(cycle.getId())
                .stream()
                .filter(item -> item.getItemType() == CycleItemType.FIXED_EXPENSE
                        || item.getItemType() == CycleItemType.VARIABLE_EXPENSE)
                .toList();
    }

    @Nested
    @DisplayName("el catalogo de categorias")
    class Catalogo {

        @Test
        @DisplayName("trae las 17 del sistema")
        void traeLasDelSistema() {
            List<ExpenseCategory> catalogo = expenses.catalog(userId);

            assertThat(catalogo).hasSize(17).allSatisfy(c -> {
                assertThat(c.isSystem()).isTrue();
                assertThat(c.getUserId()).isNull();
            });
        }

        @Test
        @DisplayName("viene en el orden en que debe mostrarse")
        void vieneOrdenado() {
            List<ExpenseCategory> catalogo = expenses.catalog(userId);

            assertThat(catalogo).isSortedAccordingTo(
                    (a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()));
            assertThat(catalogo.getFirst().getCode()).isEqualTo("HOUSING");
            assertThat(catalogo.getLast().getCode()).isEqualTo("OTHER");
        }

        @Test
        @DisplayName("una categoria inventada no se puede usar")
        void rechazaUnaCategoriaInventada() {
            assertThatThrownBy(() -> capturar("Renta", ExpenseKind.FIXED, "6000.00",
                            UUID.randomUUID().toString()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("una categoria del sistema si se puede usar")
        void aceptaUnaDelSistema() {
            String vivienda = expenses.catalog(userId).getFirst().getPublicId();

            Expense renta = capturar("Renta", ExpenseKind.FIXED, "6000.00", vivienda);

            assertThat(renta.getCategoryId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("con un ciclo abierto")
    class ConCicloAbierto {

        @Test
        @DisplayName("un gasto nuevo entra al ciclo en curso")
        void elGastoNuevoEntra() {
            BudgetCycle cycle = cycles.openNextCycle(userId);
            assertThat(renglonesDeGasto(cycle)).isEmpty();

            capturar("Renta", ExpenseKind.FIXED, "6000.00", null);

            assertThat(renglonesDeGasto(cycle)).hasSize(1);
            assertThat(renglonesDeGasto(cycle).getFirst().getName()).isEqualTo("Renta");
        }

        @Test
        @DisplayName("un gasto fijo entra como pendiente")
        void elFijoEntraPendiente() {
            BudgetCycle cycle = cycles.openNextCycle(userId);

            capturar("Renta", ExpenseKind.FIXED, "6000.00", null);

            CycleItem renglon = renglonesDeGasto(cycle).getFirst();
            assertThat(renglon.getItemType()).isEqualTo(CycleItemType.FIXED_EXPENSE);
            assertThat(renglon.getStatus()).isEqualTo(ItemStatus.PENDING);
        }

        @Test
        @DisplayName("un gasto variable entra pidiendo revision")
        void elVariablePideRevision() {
            BudgetCycle cycle = cycles.openNextCycle(userId);

            capturar("Despensa", ExpenseKind.VARIABLE, "2500.00", null);

            CycleItem renglon = renglonesDeGasto(cycle).getFirst();
            assertThat(renglon.getItemType()).isEqualTo(CycleItemType.VARIABLE_EXPENSE);
            assertThat(renglon.getStatus()).isEqualTo(ItemStatus.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("la flexibilidad se copia al renglon")
        void copiaLaFlexibilidad() {
            // El renglon tiene que saberlo por si mismo: el analisis mira el
            // ciclo, no la plantilla, y la plantilla pudo cambiar despues.
            BudgetCycle cycle = cycles.openNextCycle(userId);

            expenses.create(
                    userId, null, "Renta", ExpenseKind.FIXED, Money.of("6000.00"),
                    Frequency.BIWEEKLY, DIA_SEGURO, Flexibility.CRITICAL,
                    inicioLejano(), null, null);

            assertThat(renglonesDeGasto(cycle).getFirst().getFlexibility())
                    .isEqualTo(Flexibility.CRITICAL);
        }

        @Test
        @DisplayName("editar el monto NO toca el renglon ya generado")
        void editarNoTocaElCiclo() {
            BudgetCycle cycle = cycles.openNextCycle(userId);
            Expense renta = capturar("Renta", ExpenseKind.FIXED, "6000.00", null);

            expenses.update(
                    userId, renta.getPublicId(), null, null, Money.of("9000.00"),
                    false, null, null, false, null, null, null, null, null);

            assertThat(renglonesDeGasto(cycle).getFirst().getPlannedAmount())
                    .isEqualByComparingTo("6000.00");
        }
    }

    @Nested
    @DisplayName("listado, filtros y borrado")
    class ListadoYBorrado {

        @Test
        @DisplayName("el filtro por tipo separa fijos de variables")
        void filtraPorTipo() {
            capturar("Renta", ExpenseKind.FIXED, "6000.00", null);
            capturar("Despensa", ExpenseKind.VARIABLE, "2500.00", null);

            assertThat(expenses.search(userId, ExpenseKind.FIXED, null, null, PageRequest.of(0, 20)))
                    .hasSize(1)
                    .allSatisfy(e -> assertThat(e.getName()).isEqualTo("Renta"));
        }

        @Test
        @DisplayName("el filtro por categoria solo devuelve la de esa categoria")
        void filtraPorCategoria() {
            String vivienda = expenses.catalog(userId).getFirst().getPublicId();
            capturar("Renta", ExpenseKind.FIXED, "6000.00", vivienda);
            capturar("Despensa", ExpenseKind.VARIABLE, "2500.00", null);

            assertThat(expenses.search(userId, null, vivienda, null, PageRequest.of(0, 20)))
                    .hasSize(1);
        }

        @Test
        @DisplayName("un gasto eliminado desaparece de la lista")
        void elEliminadoNoAparece() {
            Expense renta = capturar("Renta", ExpenseKind.FIXED, "6000.00", null);
            capturar("Despensa", ExpenseKind.VARIABLE, "2500.00", null);

            expenses.delete(userId, renta.getPublicId());

            assertThat(expenses.search(userId, null, null, null, PageRequest.of(0, 20)))
                    .hasSize(1);
        }

        @Test
        @DisplayName("no se ven los gastos de otra persona")
        void noSeVenLosDeOtro() {
            Expense mio = capturar("Renta", ExpenseKind.FIXED, "6000.00", null);

            User otro = users.save(User.register(
                    "qa+" + UUID.randomUUID() + "@luma.app", "Otra persona", "hash"));

            assertThatThrownBy(() -> expenses.require(otro.getId(), mio.getPublicId()))
                    .isInstanceOf(ResourceNotFoundException.class);
            assertThat(expenses.search(otro.getId(), null, null, null, PageRequest.of(0, 20)))
                    .isEmpty();
        }
    }
}
