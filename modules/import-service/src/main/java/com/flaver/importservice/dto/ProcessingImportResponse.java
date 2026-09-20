package com.flaver.importservice.dto;

public record ProcessingImportResponse(
        boolean accepted,
        String status
) {
}
