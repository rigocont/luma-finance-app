package com.luma.income.application;

import com.luma.budget.domain.Frequency;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import com.luma.income.domain.Income;
import com.luma.income.domain.IncomeCreatedEvent;
import com.luma.income.domain.IncomeType;
import com.luma.income.infrastructure.IncomeRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de los ingresos.
 *
 * <p>Ningun metodo recibe el ingreso por su id interno: siempre por
 * {@code publicId} MAS el {@code userId}. Es la misma barrera que en el resto
 * del producto — no hay forma de tocar el ingreso de otra persona cambiando un
 * identificador.
 */
@Service
public class IncomeService {

    private static final Logger log = LoggerFactory.getLogger(IncomeService.class);

    private final IncomeRepository incomes;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public IncomeService(
            IncomeRepository incomes, ApplicationEventPublisher events, Clock clock) {
        this.incomes = incomes;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Captura un ingreso.
     *
     * <p>Publica {@link IncomeCreatedEvent} para que el modulo de presupuesto lo
     * meta al ciclo en curso si aplica. Esa decision es del producto: capturar
     * el sueldo a media quincena debe verse de inmediato, no hasta el proximo
     * periodo.
     */
    @Transactional
    public Income create(
            Long userId,
            String name,
            IncomeType type,
            Money amount,
            Frequency frequency,
            Integer expectedDay,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {

        Income income = Income.create(
                userId, name, type, amount, frequency, expectedDay, startDate, endDate);

        if (notes != null) {
            income.changeNotes(notes);
        }

        Income saved = incomes.save(income);
        log.info("Ingreso {} creado para el usuario {}", saved.getPublicId(), userId);

        events.publishEvent(new IncomeCreatedEvent(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Income> search(Long userId, IncomeType type, Boolean active, Pageable pageable) {
        return incomes.search(userId, type, active, pageable);
    }

    @Transactional(readOnly = true)
    public Income require(Long userId, String publicId) {
        return incomes.findByPublicIdAndUserIdAndDeletedAtIsNull(publicId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Ingreso", publicId));
    }

    /**
     * Aplica los cambios que vengan. Lo que llega nulo no se toca.
     *
     * <p>El calendario se cambia completo y no campo por campo: frecuencia, dia
     * y fechas son una sola decision, y mezclar una frecuencia nueva con un dia
     * viejo produce combinaciones que nadie pidio.
     */
    @Transactional
    public Income update(
            Long userId,
            String publicId,
            String name,
            IncomeType type,
            Money amount,
            boolean scheduleChanged,
            Frequency frequency,
            Integer expectedDay,
            LocalDate startDate,
            LocalDate endDate,
            String notes) {

        Income income = requireForWrite(userId, publicId);

        if (name != null) {
            income.rename(name);
        }
        if (type != null) {
            income.changeType(type);
        }
        if (amount != null) {
            income.changeAmount(amount);
        }
        if (scheduleChanged) {
            income.changeSchedule(
                    frequency != null ? frequency : income.getFrequency(),
                    expectedDay,
                    startDate != null ? startDate : income.getStartDate(),
                    endDate);
        }
        if (notes != null) {
            income.changeNotes(notes);
        }

        return incomes.save(income);
    }

    @Transactional
    public Income setActive(Long userId, String publicId, boolean active) {
        Income income = requireForWrite(userId, publicId);

        if (active) {
            income.activate();
        } else {
            income.deactivate();
        }

        return incomes.save(income);
    }

    /**
     * Borrado logico.
     *
     * <p>La fila se queda. Los renglones de ciclos anteriores guardan el origen,
     * y borrar de verdad dejaria un historial sin explicacion — ademas de ser
     * irreversible para alguien que se equivoco con un clic.
     */
    @Transactional
    public void delete(Long userId, String publicId) {
        Income income = requireForWrite(userId, publicId);
        income.softDelete(clock.instant());
        incomes.save(income);

        log.info("Ingreso {} eliminado (borrado logico) por el usuario {}", publicId, userId);
    }

    /** Igual que {@link #require} pero sin la transaccion de solo lectura. */
    private Income requireForWrite(Long userId, String publicId) {
        return incomes.findByPublicIdAndUserIdAndDeletedAtIsNull(publicId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Ingreso", publicId));
    }

    /** Convierte el importe que llega como cadena, o nulo si no venia. */
    public static Money parseAmount(String raw, String currency) {
        return raw != null ? Money.of(new BigDecimal(raw), currency) : null;
    }
}
