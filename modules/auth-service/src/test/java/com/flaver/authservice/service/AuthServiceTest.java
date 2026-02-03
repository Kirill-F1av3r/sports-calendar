package com.flaver.authservice.service;

import com.flaver.authservice.entity.User;
import com.flaver.authservice.repository.UserRepository;
import com.flaver.security.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private AuthService authService;
    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;

    private static final String SECRET = "0123456789ABCDEF0123456789ABCDEF";
    private static final long TTL = 900L;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, SECRET, TTL);
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

        String token = authService.login(u.getEmail(), "password");

        assertNotNull(token);
        assertFalse(token.isBlank());

        JwtUtils jwtUtils = new JwtUtils(SECRET, TTL);
        Jws<Claims> jws = jwtUtils.parseToken(token);
        var claims = jws.getBody();
        assertEquals(id.toString(), claims.getSubject());
        Object rolesObj = claims.get("roles");
        assertNotNull(rolesObj);
        assertTrue(rolesObj.toString().contains("USER"));
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

}
