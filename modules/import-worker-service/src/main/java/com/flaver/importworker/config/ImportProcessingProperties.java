package com.flaver.importworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "import.processing")
public record ImportProcessingProperties(
        int tableChunkRowCount,
        int textChunkLineCount,
        int failedTextChunkLineCount,
        int pdfMinTextChars,
        float pdfImageDpi
) {
    public int normalizedTableChunkRowCount() {
        return Math.max(1, tableChunkRowCount);
    }

    public int normalizedTextChunkLineCount() {
        return Math.max(1, textChunkLineCount);
    }

    public int normalizedFailedTextChunkLineCount() {
        return Math.max(1, failedTextChunkLineCount);
    }

    public int normalizedPdfMinTextChars() {
        return Math.max(0, pdfMinTextChars);
    }

    public float normalizedPdfImageDpi() {
        return Math.max(72F, pdfImageDpi);
    }
}
