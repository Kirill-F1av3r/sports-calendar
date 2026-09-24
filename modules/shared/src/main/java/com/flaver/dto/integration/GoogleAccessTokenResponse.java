package com.flaver.dto.integration;

public record GoogleAccessTokenResponse(
        String accessToken,
        Long expiresIn,
        String scopes
) {
}
