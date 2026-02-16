package com.flaver.authservice.web;

import com.flaver.authservice.dto.CreatedIdResponse;
import com.flaver.authservice.dto.LoginRequest;
import com.flaver.authservice.dto.RegisterRequest;
import com.flaver.authservice.entity.User;
import com.flaver.authservice.service.AuthService;
import com.flaver.dto.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final long accessTokenSeconds;

    public AuthController(AuthService authService,
                          @Value("${security.jwt.accessTokenSeconds}") long accessTokenSeconds) {
        this.authService = authService;
        this.accessTokenSeconds = accessTokenSeconds;
    }

    @PostMapping("/register")
    public ResponseEntity<CreatedIdResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = authService.register(registerRequest.email(), registerRequest.password(),
                registerRequest.fullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedIdResponse((user.getId())));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = authService.login(loginRequest.email(), loginRequest.password());
        return ResponseEntity.ok(new AuthResponse(token, "", accessTokenSeconds));
    }
}
