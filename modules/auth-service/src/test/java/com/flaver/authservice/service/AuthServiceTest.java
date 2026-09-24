package com.flaver.authservice.service;

import com.flaver.authservice.entity.RefreshToken;
import com.flaver.authservice.entity.User;
import com.flaver.authservice.repository.RefreshTokenRepository;
import com.flaver.authservice.repository.UserRepository;
import com.flaver.security.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private AuthService authService;
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;

    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF";
    private static final long TTL = 900L;
    private static final long REFRESH_TTL = 2_592_000L;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, SECRET, TTL, REFRESH_TTL);
    }

    @Test
    void register_success() {
        String email = "Test@Example.COM";
        String password = "password123";
        String fullName = "Ivan Ivanov";

        when(userRepository.findByEmail(email.toLowerCase())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = authService.register(email, password, fullName);

        assertNotNull(saved);
        assertEquals(email.toLowerCase(), saved.getEmail());
        assertEquals(fullName, saved.getFullName());
        assertEquals("USER", saved.getRoles());
        assertNotNull(saved.getPasswordHash());
        assertTrue(passwordEncoder.matches(password, saved.getPasswordHash()));

        verify(userRepository).findByEmail(email.toLowerCase());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_existing_throws() {
        String email = "a@b.com";
        User existing = new User();
        existing.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(email, "p", "n"));
        assertNotNull(ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsJwtWithSubjectAndRoles() {
        User u = new User();
        UUID id = UUID.randomUUID();
        u.setId(id);
        u.setEmail("user@example.com");
        u.setPasswordHash(passwordEncoder.encode("password"));
        u.setRoles("USER");

        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService.AuthTokens tokens = authService.login(u.getEmail(), "password");

        assertNotNull(tokens.accessToken());
        assertFalse(tokens.accessToken().isBlank());
        assertNotNull(tokens.refreshToken());
        assertFalse(tokens.refreshToken().isBlank());
        assertEquals(TTL, tokens.accessTokenSeconds());
        assertEquals(REFRESH_TTL, tokens.refreshTokenSeconds());

        JwtUtils jwtUtils = new JwtUtils(SECRET, TTL);
        Jws<Claims> jws = jwtUtils.parseToken(tokens.accessToken());
        var claims = jws.getBody();
        assertEquals(id.toString(), claims.getSubject());
        Object rolesObj = claims.get("roles");
        assertNotNull(rolesObj);
        assertTrue(rolesObj.toString().contains("USER"));

        verify(refreshTokenRepository).save(argThat(refreshToken ->
                refreshToken.getUserId().equals(id)
                        && refreshToken.getTokenHash() != null
                        && refreshToken.getTokenHash().length() == 64
                        && !refreshToken.getTokenHash().equals(tokens.refreshToken())
                        && refreshToken.getExpiresAt().isAfter(Instant.now())
        ));
    }

    @Test
    void login_userNotFound_throws() {
        when(userRepository.findByEmail("no@one")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.login("no@one", "pw"));
    }

    @Test
    void login_wrongPassword_throws() {
        User u = new User();
        u.setEmail("a@b.com");
        u.setPasswordHash(passwordEncoder.encode("right"));
        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        assertThrows(RuntimeException.class, () -> authService.login(u.getEmail(), "wrong"));
    }

    @Test
    void refresh_success_rotatesRefreshToken() {
        UUID userId = UUID.randomUUID();

        RefreshToken existingRefreshToken = new RefreshToken();
        existingRefreshToken.setUserId(userId);
        existingRefreshToken.setTokenHash("hash");
        existingRefreshToken.setExpiresAt(Instant.now().plusSeconds(60));

        User user = new User();
        user.setId(userId);
        user.setRoles("USER");

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existingRefreshToken));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthService.AuthTokens tokens = authService.refresh("refresh-token");

        assertNotNull(tokens.accessToken());
        assertNotNull(tokens.refreshToken());
        assertNotEquals("refresh-token", tokens.refreshToken());
        assertNotNull(existingRefreshToken.getRevokedAt());

        verify(refreshTokenRepository).findByTokenHash(argThat(hash -> hash != null && hash.length() == 64));
        verify(refreshTokenRepository).save(argThat(refreshToken ->
                refreshToken.getUserId().equals(userId)
                        && refreshToken.getRevokedAt() == null
                        && refreshToken.getExpiresAt().isAfter(Instant.now())
        ));
    }

    @Test
    void refresh_revokedToken_throws() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(UUID.randomUUID());
        refreshToken.setTokenHash("hash");
        refreshToken.setExpiresAt(Instant.now().plusSeconds(60));
        refreshToken.setRevokedAt(Instant.now());

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(refreshToken));

        assertThrows(RuntimeException.class, () -> authService.refresh("refresh-token"));
        verify(refreshTokenRepository, never()).save(any());
    }
}
