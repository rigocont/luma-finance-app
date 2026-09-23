package com.luma.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El correo es obligatorio")
                @Email(message = "Escribe un correo valido")
                @Size(max = 255)
                String email,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String name,
        @NotBlank(message = "La contrasena es obligatoria")
                @Size(min = 8, max = 100, message = "La contrasena debe tener al menos 8 caracteres")
                String password) {}
