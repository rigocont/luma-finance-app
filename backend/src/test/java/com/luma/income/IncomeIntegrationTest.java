package com.luma.income;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.Frequency;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.income.application.IncomeService;
import com.luma.income.domain.Income;
import com.luma.income.domain.IncomeType;
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
 * Los ingresos contra la base real, incluida su relacion con el ciclo abierto.
 *
 * <p>Verifica la decision de producto de la Fase 5, que es la parte que ninguna
 * prueba unitaria puede demostrar porque vive en el cruce de dos modulos:
 *
 * <ul>
 *   <li>Un ingreso NUEVO entra al ciclo en curso.
 *   <li>EDITAR un ingreso no toca los renglones ya generados.
 * </ul>
 *
 * <p>Cada prueba corre en su propia transaccion y se deshace al terminar, asi
 * que no se pisan entre si aunque compartan la misma base.
 */
@Transactional
@DisplayName("Ingresos contra la base real")
class IncomeIntegrationTest extends IntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    IncomeService incomes;

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

    /**
     * Un dia que SIEMPRE cae dentro del ciclo quincenal en curso.
     *
     * <p>Una recurrencia quincenal del dia 10 ocurre el 10 y el 25. El ciclo por
     * omision es quincenal: [1..15] o [16..fin de mes]. Sea cual sea la mitad en
     * la que corra la prueba, exactamente una de las dos fechas cae dentro. Asi
     * el resultado no depende del dia en que se ejecute la suite.
     */
    private static final int DIA_SEGURO = 10;

    private static LocalDate inicioLejano() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusYears(1);
    }

    private Income capturar(String nombre, IncomeType tipo, String monto) {
        return incomes.create(
                userId,
                nombre,
                tipo,
                Money.of(monto),
                Frequency.BIWEEKLY,
                DIA_SEGURO,
                inicioLejano(),
                null,
                null);
    }

    private List<CycleItem> renglonesDeIngreso(BudgetCycle cycle) {
        return items.findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(cycle.getId())
                .stream()
                .filter(item -> item.getItemType() == CycleItemType.INCOME)
                .toList();
    }

    @Nested
    @DisplayName("con un ciclo abierto")
    class ConCicloAbierto {

        @Test
        @DisplayName("un ingreso nuevo entra al ciclo en curso")
        void elIngresoNuevoEntraAlCicloEnCurso() {
            BudgetCycle cycle = cycles.openNextCycle(userId);
            assertThat(renglonesDeIngreso(cycle)).isEmpty();

            Income sueldo = capturar("Sueldo", IncomeType.RECURRENT, "12500.00");

            List<CycleItem> renglones = renglonesDeIngreso(cycle);
            assertThat(renglones).hasSize(1);
            assertThat(renglones.getFirst().getName()).isEqualTo("Sueldo");
            assertThat(renglones.getFirst().getPlannedAmount()).isEqualByComparingTo("12500.00");
            assertThat(renglones.getFirst().getSourceId()).isEqualTo(sueldo.getId());
        }

        @Test
        @DisplayName("un ingreso de monto variable entra pidiendo revision")
        void elIngresoVariablePideRevision() {
            BudgetCycle cycle = cycles.openNextCycle(userId);

            capturar("Comisiones", IncomeType.VARIABLE, "4000.00");

            assertThat(renglonesDeIngreso(cycle).getFirst().getStatus())
                    .isEqualTo(ItemStatus.NEEDS_REVIEW);
        }

        @Test
        @DisplayName("un ingreso de monto estable entra como pendiente")
        void elIngresoEstableEntraPendiente() {
            BudgetCycle cycle = cycles.openNextCycle(userId);

            capturar("Sueldo", IncomeType.RECURRENT, "12500.00");

            assertThat(renglonesDeIngreso(cycle).getFirst().getStatus())
                    .isEqualTo(ItemStatus.PENDING);
        }

        @Test
        @DisplayName("editar el monto NO toca el renglon ya generado")
        void editarNoTocaElCicloEnCurso() {
            // Es la mitad menos obvia de la regla, y la que sostiene que un ciclo
            // signifique algo: lo que revisaste ayer no cambia solo.
            BudgetCycle cycle = cycles.openNextCycle(userId);
            Income sueldo = capturar("Sueldo", IncomeType.RECURRENT, "12500.00");

            incomes.update(
                    userId, sueldo.getPublicId(), null, null, Money.of("20000.00"),
                    false, null, null, null, null, null);

            assertThat(renglonesDeIngreso(cycle).getFirst().getPlannedAmount())
                    .isEqualByComparingTo("12500.00");
        }

        @Test
        @DisplayName("eliminar un ingreso NO quita el renglon ya generado")
        void eliminarNoTocaElCicloEnCurso() {
            BudgetCycle cycle = cycles.openNextCycle(userId);
            Income sueldo = capturar("Sueldo", IncomeType.RECURRENT, "12500.00");

            incomes.delete(userId, sueldo.getPublicId());

            assertThat(renglonesDeIngreso(cycle)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("sin ciclo abierto")
    class SinCicloAbierto {

        @Test
        @DisplayName("capturar un ingreso no falla ni crea ciclos")
        void capturarSinCicloNoRompe() {
            Income sueldo = capturar("Sueldo", IncomeType.RECURRENT, "12500.00");

            assertThat(sueldo.getPublicId()).isNotBlank();
            assertThat(cycles.currentCycle(userId)).isEmpty();
        }

        @Test
        @DisplayName("el ingreso aparece al abrir el ciclo siguiente")
        void apareceAlAbrirElCiclo() {
            capturar("Sueldo", IncomeType.RECURRENT, "12500.00");

            BudgetCycle cycle = cycles.openNextCycle(userId);

            assertThat(renglonesDeIngreso(cycle)).hasSize(1);
        }
    }

    @Nested
    @DisplayName("listado y borrado")
    class ListadoYBorrado {

        @Test
        @DisplayName("un ingreso eliminado desaparece de la lista")
        void elEliminadoNoAparece() {
            Income sueldo = capturar("Sueldo", IncomeType.RECURRENT, "12500.00");
            capturar("Comisiones", IncomeType.VARIABLE, "4000.00");

            incomes.delete(userId, sueldo.getPublicId());

            assertThat(incomes.search(userId, null, null, PageRequest.of(0, 20)))
                    .hasSize(1)
                    .allSatisfy(i -> assertThat(i.getName()).isEqualTo("Comisiones"));
        }

        @Test
        @DisplayName("un ingreso eliminado ya no se puede consultar")
        void elEliminadoNoSeConsulta() {
            Income sueldo = capturar("Sueldo", IncomeType.RECURRENT, "12500.00");
            incomes.delete(userId, sueldo.getPublicId());

            assertThatThrownBy(() -> incomes.require(userId, sueldo.getPublicId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("el filtro por tipo solo devuelve ese tipo")
        void filtraPorTipo() {
            capturar("Sueldo", IncomeType.RECURRENT, "12500.00");
            capturar("Comisiones", IncomeType.VARIABLE, "4000.00");

            assertThat(incomes.search(userId, IncomeType.VARIABLE, null, PageRequest.of(0, 20)))
                    .hasSize(1)
                    .allSatisfy(i -> assertThat(i.getIncomeType()).isEqualTo(IncomeType.VARIABLE));
        }

        @Test
        @DisplayName("el filtro por activo distingue de los desactivados")
        void filtraPorActivo() {
            Income sueldo = capturar("Sueldo", IncomeType.RECURRENT, "12500.00");
            capturar("Comisiones", IncomeType.VARIABLE, "4000.00");

            incomes.setActive(userId, sueldo.getPublicId(), false);

            assertThat(incomes.search(userId, null, false, PageRequest.of(0, 20))).hasSize(1);
            assertThat(incomes.search(userId, null, true, PageRequest.of(0, 20))).hasSize(1);
            assertThat(incomes.search(userId, null, null, PageRequest.of(0, 20))).hasSize(2);
        }

        @Test
        @DisplayName("no se ven los ingresos de otra persona")
        void noSeVenLosDeOtro() {
            Income mio = capturar("Sueldo", IncomeType.RECURRENT, "12500.00");

            User otro = users.save(User.register(
                    "qa+" + UUID.randomUUID() + "@luma.app", "Otra persona", "hash"));

            assertThatThrownBy(() -> incomes.require(otro.getId(), mio.getPublicId()))
                    .isInstanceOf(ResourceNotFoundException.class);
            assertThat(incomes.search(otro.getId(), null, null, PageRequest.of(0, 20))).isEmpty();
        }
    }
}
