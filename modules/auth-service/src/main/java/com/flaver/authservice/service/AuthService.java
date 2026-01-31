package com.flaver.authservice.service;

import com.flaver.authservice.entity.User;
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
                       @Value("${spring.security.jwt.secret}") String secret,
                       @Value("${spring.security.jwt.accessTokenSeconds}") long accessTokenSeconds) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = new JwtUtils(secret, accessTokenSeconds);
    }

    public User register(String email, String password, String fullName) {
        userRepository.findByEmail(email).ifPresent(u -> {throw new RuntimeException("user exists");});
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles("USER");
        return userRepository.save(user);
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if  (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        return jwtUtils.generateAccessToken(user.getId().toString(), List.of("USER"));
    }
}
