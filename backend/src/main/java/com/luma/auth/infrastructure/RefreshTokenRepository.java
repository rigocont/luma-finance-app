package com.luma.auth.infrastructure;

import com.luma.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoca de golpe toda la sesion de un usuario. Se usa al detectar un reuso. */
    @Modifying
    @Query("""
            update RefreshToken t
               set t.revokedAt = :now
             where t.userId = :userId
               and t.revokedAt is null
            """)
    int revokeAllActiveForUser(@Param("userId") Long userId, @Param("now") Instant now);

    /** Limpieza: los tokens vencidos hace tiempo ya no sirven ni para auditar. */
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
