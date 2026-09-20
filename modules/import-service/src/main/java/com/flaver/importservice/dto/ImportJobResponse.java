package com.flaver.importservice.dto;

import java.util.UUID;

public record ImportJobResponse(
        UUID jobId,
        UUID calendarId,
        String fileName,
        String status,
        int totalEvents,
        int validEvents,
        int invalidEvents,
        String errorMessage
) {
}
