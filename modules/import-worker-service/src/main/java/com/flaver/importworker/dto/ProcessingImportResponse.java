package com.flaver.importworker.dto;

public record ProcessingImportResponse(
        boolean accepted,
        String status
) {
}
