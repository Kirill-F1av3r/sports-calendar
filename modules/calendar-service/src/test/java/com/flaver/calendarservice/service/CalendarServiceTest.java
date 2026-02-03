package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.exception.ForbiddenException;
import com.flaver.calendarservice.exception.NotFoundException;
import com.flaver.calendarservice.repository.CalendarRepository;
import com.flaver.calendarservice.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CalendarServiceTest {
    private CalendarService calendarService;
    private EventRepository eventRepository;
    private CalendarRepository calendarRepository;

    @BeforeEach
    void setUp() {
        calendarRepository = mock(CalendarRepository.class);
        eventRepository = mock(EventRepository.class);
        calendarService = new CalendarService(eventRepository, calendarRepository);
    }

    @Test
    void createCalendar_success() {
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID ownerId = UUID.randomUUID();
        String calendarName = "My calendar";
        String sportType = "Running";
        CreateCalendarRequest request = new CreateCalendarRequest(calendarName, sportType);

        Calendar saved = calendarService.createCalendar(ownerId, request);

        assertThat(saved).isNotNull();
        assertThat(saved.getOwnerId()).isEqualTo(ownerId);
        assertThat(saved.getName()).isEqualTo(calendarName);
        assertThat(saved.getSportType()).isEqualTo(sportType);

        verify(calendarRepository).save(any(Calendar.class));
    }

    @Test
    void createCalendar_blankName_throws() {
        UUID ownerId = UUID.randomUUID();
        CreateCalendarRequest request = new CreateCalendarRequest("  ", null);
        assertThatThrownBy(() -> calendarService.createCalendar(ownerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name is required");
        verifyNoInteractions(calendarRepository);
    }

    @Test
    void listByOwner_returns() {
        UUID ownerId = UUID.randomUUID();

        Calendar c1 = new Calendar();
        c1.setOwnerId(ownerId);
        c1.setName("A");

        Calendar c2 = new Calendar();
        c2.setOwnerId(ownerId);
        c2.setName("B");

        when(calendarRepository.findByOwnerId(ownerId)).thenReturn(List.of(c1, c2));

        var list = calendarService.findAllByOwnerId(ownerId);

        assertThat(list).hasSize(2).containsExactly(c1, c2);
        verify(calendarRepository).findByOwnerId(ownerId);
    }

    @Test
    void findOwnedOrThrow_notFound_throws() {
        UUID ownerId = UUID.randomUUID();
        UUID calendarId = UUID.randomUUID();
        when(calendarRepository.findById(calendarId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calendarService.findOwnedOrThrow(calendarId, ownerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findOwnedOrThrow_forbidden_throws() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID calendarId = UUID.randomUUID();

        Calendar calendar = new Calendar();
        calendar.setId(calendarId);
        calendar.setOwnerId(otherId);

        when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));

        assertThatThrownBy(() -> calendarService.findOwnedOrThrow(calendarId, ownerId))
                .isInstanceOf(ForbiddenException.class);
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

        CreateEventRequest request = new CreateEventRequest(title, startDate, endDate, location, null);
        Event event = calendarService.addEvent(calendarId, ownerId, request);

        assertThat(event).isNotNull();
        assertThat(event.getCalendarId()).isEqualTo(calendarId);
        assertThat(event.getTitle()).isEqualTo(title);
        assertThat(event.getSource()).isNull();
        assertThat(event.getStartDate()).isEqualTo(Date.valueOf(startDate));
        assertThat(event.getEndDate()).isEqualTo(Date.valueOf(endDate));
        assertThat(event.getLocation()).isEqualTo(location);

        ArgumentCaptor<Event> cap = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(cap.capture());
        assertThat(cap.getValue().getTitle()).isEqualTo(title);
    }

    @Test
    void addEvent_invalidDate_throws() {
        UUID ownerId = UUID.randomUUID();
        UUID calendarId = UUID.randomUUID();
        Calendar calendar = new Calendar();
        calendar.setId(calendarId);
        calendar.setOwnerId(ownerId);

        when(calendarRepository.findById(calendarId)).thenReturn(Optional.of(calendar));

        CreateEventRequest request = new CreateEventRequest("T", "not-a-date", "not-a-date",
                "loc", null);
        assertThatThrownBy(() -> calendarService.addEvent(calendarId, ownerId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid date format");
    }
}
