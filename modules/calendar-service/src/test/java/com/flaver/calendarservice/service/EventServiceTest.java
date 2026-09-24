package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.repository.CalendarRepository;
import com.flaver.calendarservice.repository.EventRepository;
import com.flaver.calendarservice.service.sort.SortParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventServiceTest {
    private EventService eventService;
    private EventRepository eventRepository;
    private CalendarRepository calendarRepository;

    @BeforeEach
    void setUp() {
        calendarRepository = mock(CalendarRepository.class);
        eventRepository = mock(EventRepository.class);
        SortParser sortParser = new SortParser();
        CalendarService calendarService = new CalendarService(calendarRepository, sortParser);
        eventService = new EventService(eventRepository, calendarService, sortParser);
    }

    @Test
    void addEvent_success() {
        UUID ownerId = UUID.randomUUID();
        UUID calendarId = UUID.randomUUID();
        String title = "Title";
        String startDate = "2026-05-01";
        String endDate = "2026-05-02";
        String location = "Location";

        Calendar calendar = new Calendar();
        calendar.setId(calendarId);
        calendar.setOwnerId(ownerId);

        when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateEventRequest request = new CreateEventRequest(
                title,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate),
                "REGIONAL",
                location,
                "https://example.com",
                List.of("кросс-классика", "кросс-лонг"),
                "REQUIRED"
        );
        Event event = eventService.addEvent(calendarId, ownerId, request);

        assertThat(event).isNotNull();
        assertThat(event.getCalendarId()).isEqualTo(calendarId);
        assertThat(event.getTitle()).isEqualTo(title);
        assertThat(event.getStartDate()).isEqualTo(LocalDate.parse(startDate));
        assertThat(event.getEndDate()).isEqualTo(LocalDate.parse(endDate));
        assertThat(event.getCompetitionLevel()).isEqualTo(CompetitionLevel.REGIONAL);
        assertThat(event.getLocation()).isEqualTo(location);
        assertThat(event.getExternalUrl()).isEqualTo("https://example.com");
        assertThat(event.getDisciplines()).containsExactly("кросс-классика", "кросс-лонг");
        assertThat(event.getPriority().name()).isEqualTo("REQUIRED");
        assertThat(event.getCompetitionLevel().getDisplayNameRu()).isEqualTo("региональные");
        assertThat(event.getPriority().getDisplayNameRu()).isEqualTo("обязательный");

        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getTitle()).isEqualTo(title);
    }

    @Test
    void addEvent_endBeforeStart_throws() {
        UUID ownerId = UUID.randomUUID();
        UUID calendarId = UUID.randomUUID();
        Calendar calendar = new Calendar();
        calendar.setId(calendarId);
        calendar.setOwnerId(ownerId);

        when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));

        CreateEventRequest request = new CreateEventRequest("T", LocalDate.parse("2026-05-03"),
                LocalDate.parse("2026-05-01"), "loc", null);
        assertThatThrownBy(() -> eventService.addEvent(calendarId, ownerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("end before start");
    }
}
