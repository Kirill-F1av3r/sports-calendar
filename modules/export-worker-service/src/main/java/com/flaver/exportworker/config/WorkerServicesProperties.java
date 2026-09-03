package com.flaver.exportworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
public record WorkerServicesProperties(Service calendar, Service export, Service integration) {
    public record Service(String url) {
    }
}
