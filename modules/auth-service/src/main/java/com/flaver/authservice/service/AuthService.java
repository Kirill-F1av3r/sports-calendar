package com.flaver.authservice.service;

import com.flaver.authservice.entity.User;
import com.flaver.authservice.exception.InvalidCredentialsException;
import com.flaver.authservice.exception.UserAlreadyExistsException;
import com.flaver.authservice.repository.UserRepository;
import com.flaver.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${security.jwt.secret}") String secret,
                       @Value("${security.jwt.accessTokenSeconds}") long accessTokenSeconds) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = new JwtUtils(secret, accessTokenSeconds);
    }

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

    public String login(String email, String password) {
        String normalizedEmail = email.toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        return jwtUtils.generateAccessToken(user.getId().toString(), List.of("USER"));
    }
}
