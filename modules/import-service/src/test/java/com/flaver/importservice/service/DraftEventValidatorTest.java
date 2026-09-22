package com.flaver.importservice.service;

import com.flaver.importservice.entity.ImportDraftEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DraftEventValidatorTest {
    private final DraftEventValidator validator = new DraftEventValidator();

    @Test
    void acceptsValidDraftAndDefaultsEndDate() {
        ImportDraftEvent event = draft();
        event.setEndDate(null);

        validator.validate(event);

        assertThat(event.isValid()).isTrue();
        assertThat(event.getEndDate()).isEqualTo(event.getStartDate());
        assertThat(event.getErrors()).isEmpty();
    }

    @Test
    void reportsAllInvalidFieldsAndClearsOldErrors() {
        ImportDraftEvent event = draft();
        event.setTitle(" ");
        event.setStartDate(null);
        event.setEndDate(LocalDate.of(2026, 1, 1));
        event.setCompetitionLevel("UNKNOWN");
        event.setPriority("URGENT");
        event.setExternalUrl("not a url");

        validator.validate(event);
        validator.validate(event);

        assertThat(event.isValid()).isFalse();
        assertThat(event.getErrors()).extracting("fieldName")
                .containsExactlyInAnyOrder("title", "startDate", "competitionLevel", "priority", "externalUrl");
    }

    @Test
    void rejectsTooLongTitleAndReverseDateRange() {
        ImportDraftEvent event = draft();
        event.setTitle("x".repeat(256));
        event.setEndDate(event.getStartDate().minusDays(1));

        validator.validate(event);

        assertThat(event.getErrors()).extracting("fieldName").containsExactlyInAnyOrder("title", "endDate");
    }

    private ImportDraftEvent draft() {
        ImportDraftEvent event = new ImportDraftEvent();
        event.setTitle("Championship");
        event.setStartDate(LocalDate.of(2026, 6, 1));
        event.setEndDate(LocalDate.of(2026, 6, 2));
        event.setCompetitionLevel("NATIONAL");
        event.setPriority("IMPORTANT");
        event.setExternalUrl("https://example.com/event");
        return event;
    }
}
