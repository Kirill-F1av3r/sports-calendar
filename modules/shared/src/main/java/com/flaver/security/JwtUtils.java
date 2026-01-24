package com.flaver.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.List;

public class JwtUtils {
    private final Key key;
    private final long accessTokenMillis;

    public JwtUtils(String secret, long accessTokenSeconds) {
        // secret must be at least 32 bytes for HS256
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenMillis = accessTokenSeconds * 1000;
    }

    public String generateAccessToken(String userId, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(userId)
                .claim("roles", roles)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessTokenMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}
