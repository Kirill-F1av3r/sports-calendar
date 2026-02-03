package com.flaver.authservice.web;

import com.flaver.authservice.entity.User;
import com.flaver.authservice.service.AuthService;
import com.flaver.dto.AuthResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> payload) {
        User user = authService.register(payload.get("email"), payload.get("password"), payload.get("fullName"));
        return ResponseEntity.ok(Map.of("id", user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String token = authService.login(payload.get("email"), payload.get("password"));
        return ResponseEntity.ok(new AuthResponse(token, "", 900));
    }
}
