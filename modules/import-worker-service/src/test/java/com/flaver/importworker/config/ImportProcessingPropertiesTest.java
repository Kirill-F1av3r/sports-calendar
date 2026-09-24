package com.flaver.importworker.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImportProcessingPropertiesTest {
    @Test
    void normalizesUnsafeValues() {
        ImportProcessingProperties properties = new ImportProcessingProperties(0, -1, 0, -10, 20);

        assertThat(properties.normalizedTableChunkRowCount()).isEqualTo(1);
        assertThat(properties.normalizedTextChunkLineCount()).isEqualTo(1);
        assertThat(properties.normalizedFailedTextChunkLineCount()).isEqualTo(1);
        assertThat(properties.normalizedPdfMinTextChars()).isZero();
        assertThat(properties.normalizedPdfImageDpi()).isEqualTo(72F);
    }
}
