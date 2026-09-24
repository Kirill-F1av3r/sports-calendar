package com.flaver.dto.integration;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GoogleAccessTokenRequest(
        @NotNull(message = "userId обязателен")
        UUID userId
) {
}
