package com.luma.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "El correo es obligatorio")
                @Email(message = "Escribe un correo valido")
                @Size(max = 255)
                String email) {}
