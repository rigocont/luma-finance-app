package com.luma.auth.infrastructure;

import com.luma.auth.domain.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Sirve para no reenviar el correo si ya hay una solicitud reciente sin usar. */
    @Query("""
            select count(t) from PasswordResetToken t
             where t.userId = :userId
               and t.usedAt is null
               and t.createdAt > :since
            """)
    long countRecentUnused(@Param("userId") Long userId, @Param("since") Instant since);

    /** Al cambiar la contrasena, cualquier enlace pendiente deja de tener sentido. */
    @Modifying
    @Query("""
            update PasswordResetToken t
               set t.usedAt = :now
             where t.userId = :userId
               and t.usedAt is null
            """)
    int invalidateAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
