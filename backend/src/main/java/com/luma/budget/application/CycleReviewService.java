package com.luma.budget.application;

import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.ItemHistoryEntry;
import com.luma.budget.domain.ItemSource;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.common.model.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La revision de un ciclo: los renglones cuyo monto todavia no se sabe.
 *
 * <p>Vive aparte de {@link BudgetCycleService} porque responde una pregunta
 * distinta. Aquel opera el ciclo —abrirlo, cerrarlo, confirmar un renglon—;
 * este mira HACIA ATRAS, a lo que costo lo mismo en ciclos anteriores, y eso
 * trae consigo consultas que cruzan ciclos y que no tienen por que cargarse
 * cada vez que alguien pide un balance.
 *
 * <p>Confirmar aqui significa FIJAR EL MONTO, no marcar un pago. Un gasto
 * variable revisado sale de {@link ItemStatus#NEEDS_REVIEW} y queda pendiente:
 * ya se sabe cuanto es, todavia no se ha pagado. Son dos hechos distintos y
 * juntarlos haria que el balance diera por pagado lo que nadie pago.
 */
@Service
public class CycleReviewService {

    private static final Logger log = LoggerFactory.getLogger(CycleReviewService.class);

    /**
     * Cuantos ciclos anteriores se muestran en el historial de un renglon.
     *
     * <p>Seis: con ciclos quincenales son tres meses, suficiente para ver si un
     * recibo viene subiendo sin convertir el detalle en una tabla que nadie lee.
     */
    private static final int CICLOS_DE_HISTORIAL = 6;

    private final BudgetCycleRepository cycles;
    private final CycleItemRepository items;

    public CycleReviewService(BudgetCycleRepository cycles, CycleItemRepository items) {
        this.cycles = cycles;
        this.items = items;
    }

    /**
     * Un renglon por revisar, con lo que costo la vez pasada si se sabe.
     *
     * <p>La sugerencia viaja como {@code BigDecimal} y no como {@code Money}: la
     * moneda la pone la capa web al armar la respuesta, igual que en el resto
     * del modulo. Un servicio que tuviera que pedirla solo para envolver un
     * numero dependeria de las preferencias del usuario sin necesitarlas.
     */
    public record ReviewItem(
            CycleItem item, BigDecimal suggestion, BudgetCycle suggestedFrom) {}

    /**
     * Los renglones del ciclo que piden revision, con su sugerencia.
     *
     * <p>La sugerencia es lo que se CONFIRMO del mismo gasto en el ciclo
     * inmediatamente anterior. No un promedio: un promedio de seis ciclos
     * esconde justo lo que importa cuando un recibo acaba de subir. El promedio
     * lo puede sacar la persona mirando el historial, que esta a un clic.
     */
    @Transactional(readOnly = true)
    public List<ReviewItem> pendingReview(BudgetCycle cycle) {
        List<CycleItem> porRevisar = items.findByBudgetCycleIdAndStatusOrderByDueDateAscIdAsc(
                cycle.getId(), ItemStatus.NEEDS_REVIEW);

        if (porRevisar.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> sugerencias = new HashMap<>();
        Optional<BudgetCycle> anterior = cycles
                .findFirstByUserIdAndStartDateLessThanOrderByStartDateDesc(
                        cycle.getUserId(), cycle.getStartDate());

        if (anterior.isPresent()) {
            Set<Long> origenes = new HashSet<>();
            for (CycleItem item : porRevisar) {
                if (item.getSourceId() != null) {
                    origenes.add(item.getSourceId());
                }
            }

            if (!origenes.isEmpty()) {
                List<CycleItem> delAnterior =
                        items.findByBudgetCycleIdAndSourceTypeAndSourceIdIn(
                                anterior.get().getId(), ItemSource.EXPENSE, origenes);

                for (CycleItem viejo : delAnterior) {
                    // Solo lo confirmado. Un monto planeado que nadie confirmo
                    // es un plan, no lo que costo, y sugerirlo propagaria la
                    // estimacion vieja de ciclo en ciclo.
                    if (viejo.getActualAmount() != null) {
                        sugerencias.put(viejo.getSourceId(), viejo.getActualAmount());
                    }
                }
            }
        }

        List<ReviewItem> resultado = new ArrayList<>(porRevisar.size());

        for (CycleItem item : porRevisar) {
            BigDecimal sugerido =
                    item.getSourceId() != null ? sugerencias.get(item.getSourceId()) : null;

            resultado.add(new ReviewItem(
                    item, sugerido, sugerido != null ? anterior.orElse(null) : null));
        }

        return resultado;
    }

    /**
     * Lo que costo un gasto en los ciclos anteriores.
     *
     * <p>Se pide al abrir el detalle de un renglon, no al cargar la lista: es
     * una consulta por renglon y traerla para todos de golpe costaria mucho para
     * un dato que casi siempre se mira de a uno.
     */
    @Transactional(readOnly = true)
    public List<ItemHistoryEntry> historyOf(BudgetCycle cycle, String itemPublicId) {
        CycleItem item = items.findByPublicIdAndBudgetCycleId(itemPublicId, cycle.getId())
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "Renglon del ciclo", itemPublicId));

        if (item.getSourceId() == null) {
            // Un renglon suelto no salio de ninguna plantilla: no tiene con que
            // compararse. Lista vacia, no error.
            return List.of();
        }

        return items.historyOfSource(
                cycle.getUserId(),
                item.getSourceType(),
                item.getSourceId(),
                cycle.getStartDate(),
                PageRequest.of(0, CICLOS_DE_HISTORIAL));
    }

    /**
     * Fija de una vez el monto de varios renglones.
     *
     * <p>Todo o nada. Si un renglon del lote falla, no se confirma ninguno: una
     * confirmacion a medias dejaria a la persona sin saber cuales quedaron, y
     * averiguarlo le costaria mas que volver a mandar el lote.
     *
     * <p>NO marca pagos. Cambiar el monto planeado de un renglon en revision es
     * justo lo que lo saca de revision; registrar el pago es otra accion.
     */
    @Transactional
    public List<CycleItem> confirmAmounts(BudgetCycle cycle, Map<String, Money> montosPorRenglon) {
        if (!cycle.isMutable()) {
            throw new BusinessRuleException("Este ciclo esta cerrado y no se puede modificar.");
        }
        if (montosPorRenglon.isEmpty()) {
            throw new BusinessRuleException("Manda al menos un renglon por confirmar.");
        }

        Map<String, CycleItem> encontrados = new LinkedHashMap<>();

        for (Map.Entry<String, Money> entrada : montosPorRenglon.entrySet()) {
            CycleItem item = items.findByPublicIdAndBudgetCycleId(entrada.getKey(), cycle.getId())
                    .orElseThrow(() -> ResourceNotFoundException.of(
                            "Renglon del ciclo", entrada.getKey()));

            item.changePlannedAmount(entrada.getValue());
            encontrados.put(entrada.getKey(), item);
        }

        List<CycleItem> guardados = items.saveAll(encontrados.values());

        log.info(
                "Confirmados {} renglon(es) del ciclo {}",
                guardados.size(),
                cycle.getPublicId());

        return guardados;
    }
}
