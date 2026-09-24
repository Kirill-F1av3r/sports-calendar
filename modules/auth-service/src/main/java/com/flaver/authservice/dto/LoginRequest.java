package com.flaver.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Email не должен быть пустым")
        @Email(message = "Email должен быть корректным")
        @Size(max = 255, message = "Email должен быть не длиннее 255 символов")
        String email,

        @NotBlank(message = "Пароль не должен быть пустым")
        @Size(max = 72, message = "Пароль должен быть не длиннее 72 символов")
        String password
) {
}
