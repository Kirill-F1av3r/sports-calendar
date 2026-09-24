package com.flaver.calendarservice.service;

import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.entity.EventPriority;
import com.flaver.calendarservice.repository.EventRepository;
import com.flaver.dto.export.CalendarExportData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalendarExportServiceTest {
    @Test
    void mapsOwnedCalendarAndOrderedEventsForExport() {
        UUID calendarId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Calendar calendar = new Calendar();
        calendar.setId(calendarId);
        calendar.setOwnerId(ownerId);
        calendar.setName("Season");
        calendar.setSportType("Ski");
        calendar.setYear(2026);
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setCalendarId(calendarId);
        event.setTitle("Cup");
        event.setStartDate(LocalDate.of(2026, 1, 1));
        event.setEndDate(LocalDate.of(2026, 1, 2));
        event.setCompetitionLevel(CompetitionLevel.NATIONAL);
        event.setPriority(EventPriority.IMPORTANT);
        event.setDisciplines(List.of("Sprint"));
        EventRepository repository = mock(EventRepository.class);
        CalendarService calendarService = mock(CalendarService.class);
        when(calendarService.findOwnedOrThrow(calendarId, ownerId)).thenReturn(calendar);
        when(repository.findByCalendarIdOrderByStartDateAsc(calendarId)).thenReturn(List.of(event));

        CalendarExportData result = new CalendarExportService(repository, calendarService)
                .getExportData(calendarId, ownerId);

        assertThat(result.calendarName()).isEqualTo("Season");
        assertThat(result.events()).singleElement().satisfies(exported -> {
            assertThat(exported.title()).isEqualTo("Cup");
            assertThat(exported.competitionLevel()).isEqualTo(CompetitionLevel.NATIONAL.getDisplayNameRu());
            assertThat(exported.priority()).isEqualTo(EventPriority.IMPORTANT.getDisplayNameRu());
            assertThat(exported.sportType()).isEqualTo("Ski");
        });
        verify(calendarService).findOwnedOrThrow(calendarId, ownerId);
    }
}
