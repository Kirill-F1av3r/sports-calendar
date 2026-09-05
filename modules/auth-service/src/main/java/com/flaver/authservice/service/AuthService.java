package com.flaver.authservice.service;

import com.flaver.authservice.entity.RefreshToken;
import com.flaver.authservice.entity.User;
import com.flaver.authservice.exception.InvalidCredentialsException;
import com.flaver.authservice.exception.UserAlreadyExistsException;
import com.flaver.authservice.repository.RefreshTokenRepository;
import com.flaver.authservice.repository.UserRepository;
import com.flaver.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${security.jwt.secret}") String secret,
                       @Value("${security.jwt.accessTokenSeconds}") long accessTokenSeconds,
                       @Value("${security.jwt.refreshTokenSeconds}") long refreshTokenSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
        this.jwtUtils = new JwtUtils(secret, accessTokenSeconds);
    }

    @Transactional
    public User register(String email, String password, String fullName) {
        String normalizedEmail = email.toLowerCase();
        userRepository.findByEmail(normalizedEmail)
                .ifPresent(user -> {
                    throw new UserAlreadyExistsException("user already exists");
                });

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles("USER");
        return userRepository.save(user);
    }

    @Transactional
    public AuthTokens login(String email, String password) {
        String normalizedEmail = email.toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthTokens refresh(String rawRefreshToken) {
        RefreshToken refreshToken = findActiveRefreshToken(rawRefreshToken);
        refreshToken.setRevokedAt(Instant.now());

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .ifPresent(refreshToken -> {
                    if (refreshToken.getRevokedAt() == null) {
                        refreshToken.setRevokedAt(Instant.now());
                    }
                });
    }

    private AuthTokens issueTokens(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getId().toString(), roles(user));
        String refreshToken = generateRefreshToken();

        RefreshToken entity = new RefreshToken();
        entity.setUserId(user.getId());
        entity.setTokenHash(hashToken(refreshToken));
        entity.setExpiresAt(Instant.now().plusSeconds(refreshTokenSeconds));
        refreshTokenRepository.save(entity);

        return new AuthTokens(accessToken, refreshToken, accessTokenSeconds, refreshTokenSeconds);
    }

    private RefreshToken findActiveRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        Instant now = Instant.now();
        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(now)) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        return refreshToken;
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private List<String> roles(User user) {
        if (user.getRoles() == null || user.getRoles().isBlank()) {
            return List.of("USER");
        }
        return Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .toList();
    }

    public record AuthTokens(String accessToken, String refreshToken, long accessTokenSeconds, long refreshTokenSeconds) {
    }
}
