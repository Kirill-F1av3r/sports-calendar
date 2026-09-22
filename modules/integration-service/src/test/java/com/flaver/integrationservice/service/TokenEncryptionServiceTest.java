package com.flaver.integrationservice.service;

import com.flaver.integrationservice.config.IntegrationSecurityProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptionServiceTest {
    @Test
    void encryptsAndDecryptsWithoutDeterministicCiphertext() {
        TokenEncryptionService service = service("secret-one");

        String first = service.encrypt("refresh-token");
        String second = service.encrypt("refresh-token");

        assertThat(first).isNotEqualTo(second);
        assertThat(service.decrypt(first)).isEqualTo("refresh-token");
        assertThat(service.decrypt(second)).isEqualTo("refresh-token");
    }

    @Test
    void rejectsMalformedAndWrongKeyCiphertext() {
        String encrypted = service("secret-one").encrypt("refresh-token");

        assertThatThrownBy(() -> service("secret-two").decrypt(encrypted))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to decrypt token");
        assertThatThrownBy(() -> service("secret-one").decrypt("invalid"))
                .isInstanceOf(IllegalStateException.class);
    }

    private TokenEncryptionService service(String secret) {
        return new TokenEncryptionService(new IntegrationSecurityProperties(secret));
    }
}
