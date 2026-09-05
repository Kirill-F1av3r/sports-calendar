package com.flaver.authservice.web;

import com.flaver.authservice.dto.CreatedIdResponse;
import com.flaver.authservice.dto.AuthResponse;
import com.flaver.authservice.dto.LoginRequest;
import com.flaver.authservice.dto.RegisterRequest;
import com.flaver.authservice.entity.User;
import com.flaver.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthService authService;
    private final boolean refreshTokenCookieSecure;
    private final String refreshTokenCookieSameSite;

    public AuthController(AuthService authService,
                          @Value("${security.cookies.refreshToken.secure}") boolean refreshTokenCookieSecure,
                          @Value("${security.cookies.refreshToken.sameSite}") String refreshTokenCookieSameSite) {
        this.authService = authService;
        this.refreshTokenCookieSecure = refreshTokenCookieSecure;
        this.refreshTokenCookieSameSite = refreshTokenCookieSameSite;
    }

    @PostMapping("/register")
    public ResponseEntity<CreatedIdResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = authService.register(registerRequest.email(), registerRequest.password(),
                registerRequest.fullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedIdResponse((user.getId())));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthService.AuthTokens tokens = authService.login(loginRequest.email(), loginRequest.password());
        return authResponse(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        AuthService.AuthTokens tokens = authService.refresh(refreshToken);
        return authResponse(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expireRefreshTokenCookie().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> authResponse(AuthService.AuthTokens tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(tokens.refreshToken(), tokens.refreshTokenSeconds()).toString())
                .body(new AuthResponse(tokens.accessToken(), tokens.accessTokenSeconds()));
    }

    private ResponseCookie refreshTokenCookie(String refreshToken, long refreshTokenSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path("/auth")
                .maxAge(Duration.ofSeconds(refreshTokenSeconds))
                .build();
    }

    private ResponseCookie expireRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
