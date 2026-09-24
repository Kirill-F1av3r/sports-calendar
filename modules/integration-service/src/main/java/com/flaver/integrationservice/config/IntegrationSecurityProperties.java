package com.flaver.integrationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "integration")
public record IntegrationSecurityProperties(String tokenEncryptionSecret) {
}
