package com.flaver.importservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public record ImportServicesProperties(Service calendar) {
    public record Service(String url) {
    }
}
