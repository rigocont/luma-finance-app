package com.luma.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "El enlace no incluye el codigo de recuperacion") String token,
        @NotBlank(message = "La contrasena es obligatoria")
                @Size(min = 8, max = 100, message = "La contrasena debe tener al menos 8 caracteres")
                String newPassword) {}
