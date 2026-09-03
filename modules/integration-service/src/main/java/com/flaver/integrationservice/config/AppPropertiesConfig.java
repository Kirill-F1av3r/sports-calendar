package com.flaver.integrationservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GoogleOAuthProperties.class, IntegrationSecurityProperties.class})
public class AppPropertiesConfig {
}
