package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.entity.EventPriority;
import com.flaver.calendarservice.repository.CalendarRepository;
import com.flaver.calendarservice.repository.EventRepository;
import com.flaver.calendarservice.service.sort.SortParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalendarCopyServiceTest {
    private CalendarRepository calendarRepository;
    private EventRepository eventRepository;
    private CalendarCopyService calendarCopyService;

    @BeforeEach
    void setUp() {
        calendarRepository = mock(CalendarRepository.class);
        eventRepository = mock(EventRepository.class);
        CalendarService calendarService = new CalendarService(calendarRepository, new SortParser());
        calendarCopyService = new CalendarCopyService(calendarService, eventRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void copyCalendar_copiesFilteredEventsToNewCalendar() {
        UUID ownerId = UUID.randomUUID();
        UUID sourceCalendarId = UUID.randomUUID();

        Calendar sourceCalendar = new Calendar();
        sourceCalendar.setId(sourceCalendarId);
        sourceCalendar.setOwnerId(ownerId);

        Event sourceEvent = new Event();
        UUID sourceEventId = sourceEvent.getId();
        sourceEvent.setCalendarId(sourceCalendarId);
        sourceEvent.setTitle("Championship");
        sourceEvent.setStartDate(LocalDate.parse("2027-05-01"));
        sourceEvent.setEndDate(LocalDate.parse("2027-05-03"));
        sourceEvent.setCompetitionLevel(CompetitionLevel.REGIONAL);
        sourceEvent.setLocation("Moscow");
        sourceEvent.setExternalUrl("https://example.com");
        sourceEvent.setDisciplines(List.of("800 м", "1500 м"));
        sourceEvent.setPriority(EventPriority.REQUIRED);

        when(calendarRepository.findById(sourceCalendarId)).thenReturn(Optional.of(sourceCalendar));
        when(calendarRepository.save(any(Calendar.class))).thenAnswer(inv -> inv.getArgument(0));
        when(eventRepository.findAll(any(Specification.class))).thenReturn(List.of(sourceEvent));

        Calendar copiedCalendar = calendarCopyService.copyCalendar(
                sourceCalendarId,
                ownerId,
                new CreateCalendarRequest("Required starts", "Running", 2027),
                LocalDate.parse("2027-01-01"),
                LocalDate.parse("2027-12-31"),
                "REGIONAL",
                "REQUIRED",
                "Moscow"
        );

        assertThat(copiedCalendar.getOwnerId()).isEqualTo(ownerId);
        assertThat(copiedCalendar.getName()).isEqualTo("Required starts");
        assertThat(copiedCalendar.getSportType()).isEqualTo("Running");
        assertThat(copiedCalendar.getYear()).isEqualTo(2027);

        ArgumentCaptor<Iterable<Event>> eventsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(eventRepository).saveAll(eventsCaptor.capture());

        List<Event> copiedEvents = (List<Event>) eventsCaptor.getValue();
        assertThat(copiedEvents).hasSize(1);

        Event copiedEvent = copiedEvents.getFirst();
        assertThat(copiedEvent.getId()).isNotEqualTo(sourceEventId);
        assertThat(copiedEvent.getCalendarId()).isEqualTo(copiedCalendar.getId());
        assertThat(copiedEvent.getTitle()).isEqualTo(sourceEvent.getTitle());
        assertThat(copiedEvent.getStartDate()).isEqualTo(sourceEvent.getStartDate());
        assertThat(copiedEvent.getEndDate()).isEqualTo(sourceEvent.getEndDate());
        assertThat(copiedEvent.getCompetitionLevel()).isEqualTo(sourceEvent.getCompetitionLevel());
        assertThat(copiedEvent.getLocation()).isEqualTo(sourceEvent.getLocation());
        assertThat(copiedEvent.getExternalUrl()).isEqualTo(sourceEvent.getExternalUrl());
        assertThat(copiedEvent.getDisciplines()).containsExactly("800 м", "1500 м");
        assertThat(copiedEvent.getPriority()).isEqualTo(sourceEvent.getPriority());
    }
}
