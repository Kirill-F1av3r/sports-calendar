package com.flaver.integrationservice.web;

import com.flaver.dto.integration.GoogleAccessTokenRequest;
import com.flaver.dto.integration.GoogleAccessTokenResponse;
import com.flaver.integrationservice.service.GoogleOAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/integrations/google")
public class InternalGoogleIntegrationController {
    private final GoogleOAuthService googleOAuthService;

    public InternalGoogleIntegrationController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @PostMapping("/access-token")
    public ResponseEntity<GoogleAccessTokenResponse> accessToken(@Valid @RequestBody GoogleAccessTokenRequest request) {
        return ResponseEntity.ok(googleOAuthService.getAccessToken(request.userId()));
    }
}
