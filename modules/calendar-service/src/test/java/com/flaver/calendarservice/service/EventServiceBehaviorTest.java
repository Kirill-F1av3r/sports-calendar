package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.dto.UpdateEventRequest;
import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.entity.EventPriority;
import com.flaver.calendarservice.exception.NotFoundException;
import com.flaver.calendarservice.repository.EventRepository;
import com.flaver.calendarservice.service.sort.SortParser;
import com.flaver.dto.calendar.BatchCreateEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceBehaviorTest {
    @Mock EventRepository repository;
    @Mock CalendarService calendarService;

    private EventService service;
    private final UUID calendarId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EventService(repository, calendarService, new SortParser());
    }

    @Test
    void addsEventWithDefaultsAndNormalizedOptionalFields() {
        when(repository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateEventRequest request = new CreateEventRequest(" Cup ", LocalDate.of(2026, 1, 1), null,
                null, " ", " https://event ", Arrays.asList(" Sprint ", "", null), null);

        Event result = service.addEvent(calendarId, ownerId, request);

        assertThat(result.getTitle()).isEqualTo("Cup");
        assertThat(result.getEndDate()).isEqualTo(result.getStartDate());
        assertThat(result.getCompetitionLevel()).isEqualTo(CompetitionLevel.OTHER);
        assertThat(result.getPriority()).isEqualTo(EventPriority.OPTIONAL);
        assertThat(result.getLocation()).isNull();
        assertThat(result.getExternalUrl()).isEqualTo("https://event");
        assertThat(result.getDisciplines()).containsExactly("Sprint");
    }

    @Test
    void findsEventsWithDefaultAndExplicitPagination() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.findEvents(calendarId, ownerId, null, null, null, null, null, null, null, null);
        service.findEvents(calendarId, ownerId, null, null, "national", "important", "cup", 2, 10, "title,desc");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, org.mockito.Mockito.times(2)).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getAllValues().get(0).getPageNumber()).isZero();
        assertThat(pageable.getAllValues().get(0).getPageSize()).isEqualTo(50);
        assertThat(pageable.getAllValues().get(1).getPageNumber()).isEqualTo(2);
        assertThat(pageable.getAllValues().get(1).getSort().getOrderFor("title").isDescending()).isTrue();
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> service.findEvents(calendarId, ownerId, null, null, null, null, null, -1, 10, null))
                .hasMessage("page must not be negative");
        assertThatThrownBy(() -> service.findEvents(calendarId, ownerId, null, null, null, null, null, 0, 101, null))
                .hasMessageContaining("size must be between");
        verify(repository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void checksEventBelongsToRequestedCalendar() {
        UUID eventId = UUID.randomUUID();
        Event event = event(eventId);
        event.setCalendarId(UUID.randomUUID());
        when(repository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.findOwnedEventOrThrow(calendarId, eventId, ownerId))
                .isInstanceOf(NotFoundException.class);
        when(repository.findById(eventId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findOwnedEventOrThrow(calendarId, eventId, ownerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatesEveryMutableFieldAndDeletesEvent() {
        UUID eventId = UUID.randomUUID();
        Event event = event(eventId);
        when(repository.findById(eventId)).thenReturn(Optional.of(event));
        when(repository.save(event)).thenReturn(event);
        UpdateEventRequest request = new UpdateEventRequest(" New ", LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 3), "INTERNATIONAL", " ", " https://new ",
                Arrays.asList(" Long ", ""), "REQUIRED");

        Event result = service.updateEvent(calendarId, eventId, ownerId, request);
        service.deleteEvent(calendarId, eventId, ownerId);

        assertThat(result.getTitle()).isEqualTo("New");
        assertThat(result.getCompetitionLevel()).isEqualTo(CompetitionLevel.INTERNATIONAL);
        assertThat(result.getPriority()).isEqualTo(EventPriority.REQUIRED);
        assertThat(result.getLocation()).isNull();
        assertThat(result.getDisciplines()).containsExactly("Long");
        verify(repository).delete(event);
    }

    @Test
    void rejectsBlankTitleAndInvalidUpdatedRange() {
        UUID eventId = UUID.randomUUID();
        Event event = event(eventId);
        when(repository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.updateEvent(calendarId, eventId, ownerId,
                new UpdateEventRequest(" ", null, null, null, null, null, null, null)))
                .hasMessage("event title must not be blank");
        assertThatThrownBy(() -> service.updateEvent(calendarId, eventId, ownerId,
                new UpdateEventRequest(null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1),
                        null, null, null, null, null)))
                .hasMessage("end before start");
    }

    @Test
    void createsBatchAndRejectsInvalidBatchRange() {
        BatchCreateEventRequest request = new BatchCreateEventRequest(" Cup ", LocalDate.of(2026, 4, 1), null,
                "LOCAL", null, null, null, "OPTIONAL");
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.addEventsBatch(calendarId, ownerId, List.of(request))).isEqualTo(1);
        assertThatThrownBy(() -> service.addEventsBatch(calendarId, ownerId, List.of(
                new BatchCreateEventRequest("Bad", LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 1),
                        null, null, null, null, null))))
                .hasMessage("end before start");
    }

    private Event event(UUID eventId) {
        Event event = new Event();
        event.setId(eventId);
        event.setCalendarId(calendarId);
        event.setTitle("Old");
        event.setStartDate(LocalDate.of(2026, 1, 1));
        event.setEndDate(LocalDate.of(2026, 1, 2));
        return event;
    }
}
