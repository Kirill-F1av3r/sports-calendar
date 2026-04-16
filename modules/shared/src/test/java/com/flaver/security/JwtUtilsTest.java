package com.flaver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilsTest {

    private static final String TEST_SECRET = "0123456789ABCDEF0123456789ABCDEF";

    @Test
    void generateAndParseToken_returnsClaimsWithSubjectAndRoles() {
        JwtUtils jwtUtils = new JwtUtils(TEST_SECRET, 60);
        String userId = "user-123";
        List<String> roles = List.of("USER", "ADMIN");

        String token = jwtUtils.generateAccessToken(userId, roles);

        assertNotNull(token);
        assertFalse(token.isBlank());

        // parse back
        Jws<Claims> jws = jwtUtils.parseToken(token);
        Claims claims = jws.getBody();

        assertEquals(userId, claims.getSubject());

        Object rolesObj = claims.get("roles");
        assertNotNull(rolesObj);
        String rolesStr = rolesObj.toString();
        assertTrue(rolesStr.contains("USER"));
        assertTrue(rolesStr.contains("ADMIN"));

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
    }

    @Test
    void parseToken_withExpiredToken_throwsExpiredJwtException() {
        // arrange: TTL negative so token already expired
        JwtUtils jwtUtils = new JwtUtils(TEST_SECRET, -1); // negative TTL -> expiration in past
        String token = jwtUtils.generateAccessToken("u", List.of("X"));

        assertThrows(JwtException.class, () -> jwtUtils.parseToken(token));
    }

    @Test
    void parseToken_withWrongSecret_throwsJwtException() {
        JwtUtils signer = new JwtUtils(TEST_SECRET, 60);
        String token = signer.generateAccessToken("someone", List.of("USER"));

        JwtUtils parser = new JwtUtils("FEDCBA9876543210FEDCBA9876543210", 60); // different secret

        assertThrows(JwtException.class, () -> parser.parseToken(token));
    }
}
