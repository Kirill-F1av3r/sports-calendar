package com.flaver.importservice.service;

import com.flaver.importservice.dto.DraftEventErrorResponse;
import com.flaver.importservice.dto.DraftEventResponse;
import com.flaver.importservice.dto.ImportJobResponse;
import com.flaver.importservice.entity.ImportDraftEvent;
import com.flaver.importservice.entity.ImportJob;

public final class ImportMapper {
    private ImportMapper() {
    }

    public static ImportJobResponse toJobResponse(ImportJob job) {
        int total = job.getDraftEvents().size();
        int valid = (int) job.getDraftEvents().stream().filter(ImportDraftEvent::isValid).count();
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
