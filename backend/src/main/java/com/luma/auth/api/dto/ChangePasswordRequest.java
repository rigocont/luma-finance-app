package com.luma.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Escribe tu contrasena actual") String currentPassword,
        @NotBlank(message = "La contrasena nueva es obligatoria")
                @Size(min = 8, max = 100, message = "La contrasena debe tener al menos 8 caracteres")
                String newPassword) {}
