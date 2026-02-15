package com.flaver.authservice.web;

import com.flaver.authservice.dto.CreatedIdResponse;
import com.flaver.authservice.dto.LoginRequest;
import com.flaver.authservice.dto.RegisterRequest;
import com.flaver.authservice.entity.User;
import com.flaver.authservice.service.AuthService;
import com.flaver.dto.AuthResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<CreatedIdResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        User user = authService.register(registerRequest.email(), registerRequest.password(),
                registerRequest.fullName());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedIdResponse((user.getId())));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = authService.login(loginRequest.email(), loginRequest.password());
        return ResponseEntity.ok(new AuthResponse(token, "", 900));
    }
}
