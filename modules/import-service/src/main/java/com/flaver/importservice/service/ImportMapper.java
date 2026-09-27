package com.flaver.importservice.service;

import com.flaver.importservice.dto.DraftEventErrorResponse;
import com.flaver.importservice.dto.DraftEventResponse;
import com.flaver.importservice.dto.ImportJobResponse;
import com.flaver.importservice.entity.ImportDraftEvent;
import com.flaver.importservice.entity.ImportJob;
import com.flaver.importservice.repository.ImportJobSummary;

public final class ImportMapper {
    private ImportMapper() {
    }

    public static ImportJobResponse toJobResponse(ImportJob job, int total, int valid) {
        return new ImportJobResponse(
                job.getId(),
                job.getCalendarId(),
                job.getSourceFileName(),
                job.getStatus().name(),
                total,
                valid,
                total - valid,
                job.getErrorMessage()
        );
    }

    public static ImportJobResponse toJobResponse(ImportJobSummary summary) {
        int total = Math.toIntExact(summary.getTotalEvents());
        int valid = Math.toIntExact(summary.getValidEvents());
        return new ImportJobResponse(
                summary.getJobId(),
                summary.getCalendarId(),
                summary.getFileName(),
                summary.getStatus().name(),
                total,
                valid,
                total - valid,
                summary.getErrorMessage()
        );
    }

    public static DraftEventResponse toDraftEventResponse(ImportDraftEvent event) {
        return new DraftEventResponse(
                event.getId(),
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getCompetitionLevel(),
                event.getLocation(),
                event.getExternalUrl(),
                event.getDisciplines(),
                event.getPriority(),
                event.isValid(),
                event.getSourceReference(),
                event.getRawText(),
                event.getErrors().stream()
                        .map(error -> new DraftEventErrorResponse(error.getFieldName(), error.getMessage()))
                        .toList()
        );
    }
}
