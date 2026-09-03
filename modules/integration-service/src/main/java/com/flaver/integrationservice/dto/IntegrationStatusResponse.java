package com.flaver.integrationservice.dto;

public record IntegrationStatusResponse(
        String provider,
        boolean connected,
        String email,
        String scopes
) {
}
