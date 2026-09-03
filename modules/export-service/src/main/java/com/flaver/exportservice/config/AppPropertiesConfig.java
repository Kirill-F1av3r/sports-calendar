package com.flaver.exportservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExportServiceProperties.class)
public class AppPropertiesConfig {
}
