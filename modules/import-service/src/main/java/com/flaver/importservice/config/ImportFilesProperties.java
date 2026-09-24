package com.flaver.importservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "import.files")
public record ImportFilesProperties(long maxSizeBytes, List<String> allowedContentTypes) {
}
