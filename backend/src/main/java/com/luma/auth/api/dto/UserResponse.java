package com.luma.auth.api.dto;

import com.luma.users.domain.User;

/**
 * El usuario, tal como lo ve el cliente.
 *
 * <p>Nunca incluye el hash de la contrasena ni el id interno. El DTO es
 * independiente de la entidad a proposito: el contrato de la API no debe
 * cambiar porque cambie el esquema.
 */
public record UserResponse(
        String id, String email, String name, String status, boolean onboardingCompleted) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getPublicId(),
                user.getEmail(),
                user.getName(),
                user.getStatus().name(),
                user.hasCompletedOnboarding());
    }
}
