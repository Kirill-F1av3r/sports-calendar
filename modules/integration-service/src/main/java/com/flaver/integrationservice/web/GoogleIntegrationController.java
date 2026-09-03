package com.flaver.integrationservice.web;

import com.flaver.integrationservice.dto.ConnectResponse;
import com.flaver.integrationservice.dto.IntegrationStatusResponse;
import com.flaver.integrationservice.service.GoogleOAuthService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/integrations/google")
public class GoogleIntegrationController {
    private final GoogleOAuthService googleOAuthService;

    public GoogleIntegrationController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @GetMapping("/connect")
    public ResponseEntity<ConnectResponse> connect(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(googleOAuthService.createConnectUrl(UUID.fromString(userId)));
    }

    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> callback(@RequestParam("code") String code,
                                           @RequestParam("state") String state) {
        googleOAuthService.handleCallback(code, state);
        return ResponseEntity.ok("Google account connected. You can close this page.");
    }

    @GetMapping("/status")
    public ResponseEntity<IntegrationStatusResponse> status(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(googleOAuthService.getStatus(UUID.fromString(userId)));
    }

    @DeleteMapping
    public ResponseEntity<Void> disconnect(@RequestHeader("X-User-Id") String userId) {
        googleOAuthService.disconnect(UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }
}
