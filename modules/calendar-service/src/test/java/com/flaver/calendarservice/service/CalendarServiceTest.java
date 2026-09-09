package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.exception.ForbiddenException;
import com.flaver.calendarservice.exception.NotFoundException;
import com.flaver.calendarservice.repository.CalendarRepository;
import com.flaver.calendarservice.service.sort.SortParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CalendarServiceTest {
    private CalendarService calendarService;
    private CalendarRepository calendarRepository;

    @BeforeEach
    void setUp() {
        calendarRepository = mock(CalendarRepository.class);
        calendarService = new CalendarService(calendarRepository, new SortParser());
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
    void listByOwner_returns() {
        UUID ownerId = UUID.randomUUID();

        Calendar c1 = new Calendar();
        c1.setOwnerId(ownerId);
        c1.setName("A");

        Calendar c2 = new Calendar();
        c2.setOwnerId(ownerId);
        c2.setName("B");

        when(calendarRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(c1, c2));

        var list = calendarService.findCalendars(ownerId, null, null, null, null);

        assertThat(list).hasSize(2).containsExactly(c1, c2);
        verify(calendarRepository).findAll(any(Specification.class), any(Sort.class));
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
}
