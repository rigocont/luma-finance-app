package com.luma.expenses.application;

import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.expenses.domain.Expense;
import com.luma.expenses.domain.ExpenseCategory;
import com.luma.expenses.domain.ExpenseCreatedEvent;
import com.luma.expenses.domain.ExpenseKind;
import com.luma.expenses.infrastructure.ExpenseCategoryRepository;
import com.luma.expenses.infrastructure.ExpenseRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de los gastos, fijos y variables.
 *
 * <p>Son una sola cosa con una clasificacion, no dos entidades: lo unico que
 * cambia entre un gasto fijo y uno variable es que el variable se materializa
 * pidiendo confirmacion del monto.
 *
 * <p>Ningun metodo recibe el gasto por su id interno: siempre por
 * {@code publicId} MAS el {@code userId}.
 */
@Service
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    private final ExpenseRepository expenses;
    private final ExpenseCategoryRepository categories;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ExpenseService(
            ExpenseRepository expenses,
            ExpenseCategoryRepository categories,
            ApplicationEventPublisher events,
            Clock clock) {
        this.expenses = expenses;
        this.categories = categories;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> catalog(Long userId) {
        return categories.catalogFor(userId);
    }

    @Transactional
    public Expense create(
            Long userId,
            String categoryPublicId,
            String name,
            ExpenseKind kind,
            Money amount,
            Frequency frequency,
            Integer dueDay,
            Flexibility flexibility,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {

        Expense expense = Expense.create(
                userId,
                resolveCategory(userId, categoryPublicId),
                name,
                kind,
                amount,
                frequency,
                dueDay,
                flexibility,
                startDate,
                endDate);

        if (notes != null) {
            expense.changeNotes(notes);
        }

        Expense saved = expenses.save(expense);
        log.info("Gasto {} creado para el usuario {}", saved.getPublicId(), userId);

        events.publishEvent(new ExpenseCreatedEvent(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Expense> search(
            Long userId,
            ExpenseKind kind,
            String categoryPublicId,
            Boolean active,
            Pageable pageable) {

        return expenses.search(
                userId, kind, resolveCategory(userId, categoryPublicId), active, pageable);
    }

    @Transactional(readOnly = true)
    public Expense require(Long userId, String publicId) {
        return find(userId, publicId);
    }

    /**
     * Aplica los cambios que vengan. Lo que llega nulo no se toca.
     *
     * <p>La categoria es la excepcion: se distingue "no la mandaste" de "quiero
     * quitarla" con una bandera aparte, porque en los dos casos el valor llega
     * nulo y significan cosas distintas.
     */
    @Transactional
    public Expense update(
            Long userId,
            String publicId,
            String name,
            ExpenseKind kind,
            Money amount,
            boolean categoryChanged,
            String categoryPublicId,
            Flexibility flexibility,
            boolean scheduleChanged,
            Frequency frequency,
            Integer dueDay,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {

        Expense expense = find(userId, publicId);

        if (name != null) {
            expense.rename(name);
        }
        if (kind != null) {
            expense.changeKind(kind);
        }
        if (amount != null) {
            expense.changeAmount(amount);
        }
        if (categoryChanged) {
            expense.changeCategory(resolveCategory(userId, categoryPublicId));
        }
        if (flexibility != null) {
            expense.changeFlexibility(flexibility);
        }
        if (scheduleChanged) {
            expense.changeSchedule(
                    frequency != null ? frequency : expense.getFrequency(),
                    dueDay,
                    startDate != null ? startDate : expense.getStartDate(),
                    endDate);
        }
        if (notes != null) {
            expense.changeNotes(notes);
        }

        return expenses.save(expense);
    }

    @Transactional
    public Expense setActive(Long userId, String publicId, boolean active) {
        Expense expense = find(userId, publicId);

        if (active) {
            expense.activate();
        } else {
            expense.deactivate();
        }

        return expenses.save(expense);
    }

    @Transactional
    public void delete(Long userId, String publicId) {
        Expense expense = find(userId, publicId);
        expense.softDelete(clock.instant());
        expenses.save(expense);

        log.info("Gasto {} eliminado (borrado logico) por el usuario {}", publicId, userId);
    }

    private Expense find(Long userId, String publicId) {
        return expenses.findByPublicIdAndUserIdAndDeletedAtIsNull(publicId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Gasto", publicId));
    }

    /**
     * Traduce el identificador publico de la categoria al interno.
     *
     * <p>Una categoria que no existe, o que es de otra persona, da 404 en lugar
     * de guardarse en silencio: un gasto con una categoria invalida ensucia el
     * analisis sin que nadie se entere.
     */
    private Long resolveCategory(Long userId, String categoryPublicId) {
        if (categoryPublicId == null || categoryPublicId.isBlank()) {
            return null;
        }
        return categories.findUsable(categoryPublicId, userId)
                .map(ExpenseCategory::getId)
                .orElseThrow(() -> ResourceNotFoundException.of("Categoria", categoryPublicId));
    }

    /** Convierte el importe que llega como cadena, o nulo si no venia. */
    public static Money parseAmount(String raw, String currency) {
        return raw != null ? Money.of(new BigDecimal(raw), currency) : null;
    }
}
