package com.flaver.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email не должен быть пустым")
        @Email(message = "Email должен быть корректным")
        @Size(max = 255, message = "Email должен быть не длиннее 255 символов")
        String email,

        @NotBlank(message = "Пароль не должен быть пустым")
        @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов")
        String password,

        @NotBlank(message = "Имя не должно быть пустым")
        @Size(max = 255, message = "Имя должно быть не длиннее 255 символов")
        String fullName
) {
}
