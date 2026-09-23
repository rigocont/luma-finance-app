package com.luma.users.infrastructure;

import com.luma.users.domain.UserPreferences;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

    Optional<UserPreferences> findByUserId(Long userId);
}
