package com.luma.notifications.infrastructure;

import com.luma.notifications.domain.Notification;
import com.luma.notifications.domain.NotificationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Todas las consultas llevan userId: es la misma barrera que en el resto de la API. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Optional<Notification> findByPublicIdAndUserId(String publicId, Long userId);

    long countByUserIdAndReadFalse(Long userId);

    List<Notification> findByUserIdAndReadFalse(Long userId);

    /**
     * La comprobacion previa a insertar. Existe ademas de la llave unica en la
     * base porque un {@code DataIntegrityViolationException} es una forma tosca
     * de decir "no hay nada que hacer": es un caso normal, no una excepcion.
     */
    boolean existsByUserIdAndTypeAndReferenceId(Long userId, NotificationType type, String referenceId);
}
