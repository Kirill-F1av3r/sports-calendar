package com.flaver.integrationservice.service;

import com.flaver.dto.integration.GoogleAccessTokenResponse;
import com.flaver.integrationservice.config.GoogleOAuthProperties;
import com.flaver.integrationservice.dto.ConnectResponse;
import com.flaver.integrationservice.dto.IntegrationStatusResponse;
import com.flaver.integrationservice.entity.ConnectedAccount;
import com.flaver.integrationservice.entity.OAuthState;
import com.flaver.integrationservice.repository.ConnectedAccountRepository;
import com.flaver.integrationservice.repository.OAuthStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {
    @Mock OAuthStateRepository stateRepository;
    @Mock ConnectedAccountRepository accountRepository;
    @Mock TokenEncryptionService encryptionService;

    private MockRestServiceServer server;
    private GoogleOAuthService service;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new GoogleOAuthService(
                new GoogleOAuthProperties("client", "secret", "https://app/callback",
                        "https://accounts.example/auth", "https://accounts.example/token", "scope-a scope-b"),
                stateRepository, accountRepository, encryptionService, builder);
    }

    @Test
    void createsAuthorizationUrlAndPersistsExpiringState() {
        ConnectResponse response = service.createConnectUrl(userId);

        ArgumentCaptor<OAuthState> captor = ArgumentCaptor.forClass(OAuthState.class);
        verify(stateRepository).save(captor.capture());
        OAuthState state = captor.getValue();
        assertThat(state.getUserId()).isEqualTo(userId);
        assertThat(state.getExpiresAt()).isAfter(Instant.now().plusSeconds(500));
        assertThat(response.redirectUrl()).contains("client_id=client", "response_type=code", "state=" + state.getState());
    }

    @Test
    void handlesCallbackAndStoresEncryptedRefreshToken() {
        OAuthState state = state(Instant.now().plusSeconds(60));
        when(stateRepository.findByState("state")).thenReturn(Optional.of(state));
        when(accountRepository.findByUserIdAndProviderAndRevokedAtIsNull(userId, "GOOGLE"))
                .thenReturn(Optional.empty());
        when(encryptionService.encrypt("refresh")).thenReturn("encrypted");
        expectTokenResponse("{\"access_token\":\"access\",\"expires_in\":3600,\"refresh_token\":\"refresh\",\"scope\":\"scope-a\"}");

        service.handleCallback("code", "state");

        ArgumentCaptor<ConnectedAccount> captor = ArgumentCaptor.forClass(ConnectedAccount.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getProvider()).isEqualTo("GOOGLE");
        assertThat(captor.getValue().getEncryptedRefreshToken()).isEqualTo("encrypted");
        verify(stateRepository).delete(state);
        server.verify();
    }

    @Test
    void rejectsExpiredStateBeforeCallingGoogle() {
        OAuthState state = state(Instant.now().minusSeconds(1));
        when(stateRepository.findByState("state")).thenReturn(Optional.of(state));

        assertThatThrownBy(() -> service.handleCallback("code", "state"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("OAuth state expired");

        verify(stateRepository).delete(state);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void reportsStatusAndDisconnectsConnectedAccount() {
        ConnectedAccount account = account();
        account.setEmail("athlete@example.com");
        account.setScopes("scope-a");
        when(accountRepository.findByUserIdAndProviderAndRevokedAtIsNull(userId, "GOOGLE"))
                .thenReturn(Optional.of(account));

        IntegrationStatusResponse status = service.getStatus(userId);
        service.disconnect(userId);

        assertThat(status.connected()).isTrue();
        assertThat(status.email()).isEqualTo("athlete@example.com");
        assertThat(account.getRevokedAt()).isNotNull();
    }

    @Test
    void reportsDisconnectedStatus() {
        when(accountRepository.findByUserIdAndProviderAndRevokedAtIsNull(userId, "GOOGLE"))
                .thenReturn(Optional.empty());

        assertThat(service.getStatus(userId).connected()).isFalse();
        assertThatThrownBy(() -> service.getAccessToken(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google account is not connected");
    }

    @Test
    void refreshesAccessTokenForConnectedAccount() {
        ConnectedAccount account = account();
        account.setEncryptedRefreshToken("encrypted");
        when(accountRepository.findByUserIdAndProviderAndRevokedAtIsNull(userId, "GOOGLE"))
                .thenReturn(Optional.of(account));
        when(encryptionService.decrypt("encrypted")).thenReturn("refresh");
        expectTokenResponse("{\"access_token\":\"new-access\",\"expires_in\":1800,\"scope\":\"scope-a\"}");

        GoogleAccessTokenResponse response = service.getAccessToken(userId);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.expiresIn()).isEqualTo(1800);
        server.verify();
    }

    private void expectTokenResponse(String body) {
        server.expect(once(), requestTo("https://accounts.example/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private OAuthState state(Instant expiresAt) {
        OAuthState state = new OAuthState();
        state.setState("state");
        state.setUserId(userId);
        state.setExpiresAt(expiresAt);
        return state;
    }

    private ConnectedAccount account() {
        ConnectedAccount account = new ConnectedAccount();
        account.setUserId(userId);
        account.setProvider("GOOGLE");
        return account;
    }
}
