package com.flaver.exportworker.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WorkerServicesProperties.class)
public class AppPropertiesConfig {
}
