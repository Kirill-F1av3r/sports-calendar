package com.flaver.importservice.service;

import com.flaver.importservice.entity.ImportDraftEvent;
import com.flaver.importservice.entity.ImportDraftEventError;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;

@Component
public class DraftEventValidator {
    private static final Set<String> COMPETITION_LEVELS = Set.of(
            "INTERNATIONAL", "NATIONAL", "REGIONAL", "LOCAL", "TRAINING", "OTHER"
    );
    private static final Set<String> PRIORITIES = Set.of("REQUIRED", "IMPORTANT", "OPTIONAL");

    public void validate(ImportDraftEvent event) {
        event.getErrors().clear();

        if (isBlank(event.getTitle())) {
            addError(event, "title", "Название события обязательно");
        } else if (event.getTitle().length() > 255) {
            addError(event, "title", "Название события должно быть не длиннее 255 символов");
        }

        if (event.getStartDate() == null) {
            addError(event, "startDate", "Дата начала обязательна");
        }

        if (event.getEndDate() == null && event.getStartDate() != null) {
            event.setEndDate(event.getStartDate());
        }

        if (event.getStartDate() != null && event.getEndDate() != null && event.getEndDate().isBefore(event.getStartDate())) {
            addError(event, "endDate", "Дата окончания не может быть раньше даты начала");
        }

        if (!isBlank(event.getCompetitionLevel()) && !COMPETITION_LEVELS.contains(event.getCompetitionLevel())) {
            addError(event, "competitionLevel", "Неизвестный уровень соревнования");
        }

        if (!isBlank(event.getPriority()) && !PRIORITIES.contains(event.getPriority())) {
            addError(event, "priority", "Неизвестный приоритет");
        }

        if (!isBlank(event.getExternalUrl())) {
            try {
                URI uri = URI.create(event.getExternalUrl());
                if (isBlank(uri.getScheme()) || isBlank(uri.getHost())) {
                    addError(event, "externalUrl", "Ссылка должна быть корректным URL");
                }
            } catch (IllegalArgumentException ex) {
                addError(event, "externalUrl", "Ссылка должна быть корректным URL");
            }
        }

        event.setValid(event.getErrors().isEmpty());
    }

    private void addError(ImportDraftEvent event, String fieldName, String message) {
        ImportDraftEventError error = new ImportDraftEventError();
        error.setDraftEvent(event);
        error.setFieldName(fieldName);
        error.setMessage(message);
        event.getErrors().add(error);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
