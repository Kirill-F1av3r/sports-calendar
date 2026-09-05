package com.flaver.authservice.dto;

public record AuthResponse(String accessToken, long expiresIn) {
}
