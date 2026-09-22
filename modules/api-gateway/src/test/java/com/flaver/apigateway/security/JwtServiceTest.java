package com.flaver.apigateway.security;

import com.flaver.apigateway.config.JwtProperties;
import com.flaver.security.JwtUtils;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "a-very-long-test-secret-key-that-is-at-least-32-bytes";

    @Test
    void extractsSubjectFromValidToken() {
        JwtProperties properties = properties();
        String userId = UUID.randomUUID().toString();
        String token = new JwtUtils(SECRET, 300).generateAccessToken(userId, List.of("USER"));

        assertThat(new JwtService(properties).extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void rejectsInvalidToken() {
        assertThatThrownBy(() -> new JwtService(properties()).extractUserId("not-a-token"))
                .isInstanceOf(JwtException.class);
    }

    private JwtProperties properties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenSeconds(300);
        return properties;
    }
}
