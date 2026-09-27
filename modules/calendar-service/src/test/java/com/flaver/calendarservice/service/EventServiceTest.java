package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.dto.EventResponse;
import com.flaver.calendarservice.entity.Calendar;
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
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 2);

        Calendar calendar = new Calendar();
        calendar.setId(calendarId);
        calendar.setOwnerId(ownerId);

        when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEventRequest request = new CreateEventRequest(
                title,
                startDate,
                endDate,
                "REGIONAL",
                "Location",
                "https://example.com",
                List.of("Sprint", "Relay"),
                "REQUIRED"
        );
        EventResponse event = eventService.addEvent(calendarId, ownerId, request);

        assertThat(event.calendarId()).isEqualTo(calendarId);
        assertThat(event.title()).isEqualTo(title);
        assertThat(event.startDate()).isEqualTo(startDate);
        assertThat(event.endDate()).isEqualTo(endDate);
        assertThat(event.competitionLevel()).isEqualTo("REGIONAL");
        assertThat(event.location()).isEqualTo("Location");
        assertThat(event.externalUrl()).isEqualTo("https://example.com");
        assertThat(event.disciplines()).containsExactly("Sprint", "Relay");
        assertThat(event.priority()).isEqualTo("REQUIRED");

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTitle()).isEqualTo(title);
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
