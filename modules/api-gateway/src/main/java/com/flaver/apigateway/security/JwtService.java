package com.flaver.apigateway.security;

import com.flaver.apigateway.config.JwtProperties;
import com.flaver.security.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtUtils jwtUtils;

    public JwtService(JwtProperties properties) {
        this.jwtUtils = new JwtUtils(properties.getSecret(), properties.getAccessTokenSeconds());
    }

    public String extractUserId(String token) {
        Jws<Claims> parsedToken = jwtUtils.parseToken(token);
        String userId = parsedToken.getBody().getSubject();
        if (userId == null || userId.isBlank()) {
            throw new JwtException("Token subject is missing");
        }
        return userId;
    }
}