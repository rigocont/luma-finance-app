package com.luma.users.infrastructure;

import com.luma.users.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByPublicId(String publicId);

    /**
     * Cuentas que se registraron antes del corte y nunca terminaron el alta.
     *
     * <p>Es un primer filtro barato, no el veredicto: alguien registrado hace un
     * mes puede estar capturando ahora mismo. Quien decide es la ultima
     * actividad, y eso se mira por cuenta.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.onboardingCompletedAt IS NULL
              AND u.deletedAt IS NULL
              AND u.createdAt < :before
            """)
    List<User> findAbandonedOnboarding(@Param("before") Instant before);
}
