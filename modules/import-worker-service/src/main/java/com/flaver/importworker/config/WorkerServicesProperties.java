package com.flaver.importworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public record WorkerServicesProperties(Service importService) {
    public record Service(String url) {
    }
}
