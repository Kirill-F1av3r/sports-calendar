package com.flaver.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {}
