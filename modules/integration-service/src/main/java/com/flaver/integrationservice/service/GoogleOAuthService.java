package com.flaver.integrationservice.service;

import com.flaver.dto.integration.GoogleAccessTokenResponse;
import com.flaver.integrationservice.config.GoogleOAuthProperties;
import com.flaver.integrationservice.dto.ConnectResponse;
import com.flaver.integrationservice.dto.GoogleTokenResponse;
import com.flaver.integrationservice.dto.IntegrationStatusResponse;
import com.flaver.integrationservice.entity.ConnectedAccount;
import com.flaver.integrationservice.entity.OAuthState;
import com.flaver.integrationservice.repository.ConnectedAccountRepository;
import com.flaver.integrationservice.repository.OAuthStateRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class GoogleOAuthService {
    private static final String PROVIDER = "GOOGLE";

    private final GoogleOAuthProperties properties;
    private final OAuthStateRepository stateRepository;
    private final ConnectedAccountRepository accountRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final RestClient restClient;

    public GoogleOAuthService(GoogleOAuthProperties properties,
                              OAuthStateRepository stateRepository,
                              ConnectedAccountRepository accountRepository,
                              TokenEncryptionService tokenEncryptionService,
                              RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.stateRepository = stateRepository;
        this.accountRepository = accountRepository;
        this.tokenEncryptionService = tokenEncryptionService;
        this.restClient = restClientBuilder.build();
    }

    @Transactional
    public ConnectResponse createConnectUrl(UUID userId) {
        OAuthState state = new OAuthState();
        state.setUserId(userId);
        state.setState(UUID.randomUUID().toString());
        state.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        stateRepository.save(state);

        String redirectUrl = UriComponentsBuilder.fromUriString(properties.authorizationUri())
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", properties.scopes())
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .queryParam("include_granted_scopes", "true")
                .queryParam("state", state.getState())
                .build()
                .toUriString();

        return new ConnectResponse(redirectUrl);
    }

    @Transactional
    public void handleCallback(String code, String stateValue) {
        OAuthState state = stateRepository.findByState(stateValue)
                .orElseThrow(() -> new IllegalArgumentException("OAuth state not found"));
        if (state.getExpiresAt().isBefore(Instant.now())) {
            stateRepository.delete(state);
            throw new IllegalArgumentException("OAuth state expired");
        }

        GoogleTokenResponse token = exchangeAuthorizationCode(code);
        if (token.refreshToken() == null || token.refreshToken().isBlank()) {
            throw new IllegalArgumentException("Google did not return refresh token. Reconnect Google account with consent prompt.");
        }

        ConnectedAccount account = accountRepository.findByUserIdAndProviderAndRevokedAtIsNull(state.getUserId(), PROVIDER)
                .orElseGet(ConnectedAccount::new);
        account.setUserId(state.getUserId());
        account.setProvider(PROVIDER);
        account.setScopes(token.scope());
        account.setEncryptedRefreshToken(tokenEncryptionService.encrypt(token.refreshToken()));
        account.setRevokedAt(null);
        accountRepository.save(account);
        stateRepository.delete(state);
    }

    @Transactional(readOnly = true)
    public IntegrationStatusResponse getStatus(UUID userId) {
        return accountRepository.findByUserIdAndProviderAndRevokedAtIsNull(userId, PROVIDER)
                .map(account -> new IntegrationStatusResponse(PROVIDER, true, account.getEmail(), account.getScopes()))
                .orElseGet(() -> new IntegrationStatusResponse(PROVIDER, false, null, null));
    }

    @Transactional
    public void disconnect(UUID userId) {
        accountRepository.findByUserIdAndProviderAndRevokedAtIsNull(userId, PROVIDER)
                .ifPresent(account -> account.setRevokedAt(Instant.now()));
    }

    @Transactional(readOnly = true)
    public GoogleAccessTokenResponse getAccessToken(UUID userId) {
        ConnectedAccount account = accountRepository.findByUserIdAndProviderAndRevokedAtIsNull(userId, PROVIDER)
                .orElseThrow(() -> new IllegalArgumentException("Google account is not connected"));
        String refreshToken = tokenEncryptionService.decrypt(account.getEncryptedRefreshToken());
        GoogleTokenResponse token = refreshAccessToken(refreshToken);
        return new GoogleAccessTokenResponse(token.accessToken(), token.expiresIn(), token.scope());
    }

    private GoogleTokenResponse exchangeAuthorizationCode(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("redirect_uri", properties.redirectUri());
        body.add("grant_type", "authorization_code");
        return postTokenRequest(body);
    }

    private GoogleTokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("refresh_token", refreshToken);
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("grant_type", "refresh_token");
        return postTokenRequest(body);
    }

    private GoogleTokenResponse postTokenRequest(MultiValueMap<String, String> body) {
        return restClient.post()
                .uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(GoogleTokenResponse.class);
    }
}
