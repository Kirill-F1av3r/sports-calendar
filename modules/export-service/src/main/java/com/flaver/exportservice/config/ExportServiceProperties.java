package com.flaver.exportservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.calendar")
public record ExportServiceProperties(String url) {
}
