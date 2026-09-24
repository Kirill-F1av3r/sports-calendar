package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.UpdateCalendarRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.repository.CalendarRepository;
import com.flaver.calendarservice.service.sort.SortParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceBehaviorTest {
    @Mock CalendarRepository repository;

    private CalendarService service;
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CalendarService(repository, new SortParser());
    }

    @Test
    void createsCalendarWithTrimmedNullableSport() {
        when(repository.save(any(Calendar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Calendar result = service.createCalendar(ownerId, new CreateCalendarRequest(" Season ", " ", 2026));

        assertThat(result.getName()).isEqualTo("Season");
        assertThat(result.getSportType()).isNull();
        assertThat(result.getYear()).isEqualTo(2026);
    }

    @Test
    void updatesProvidedFieldsAndDeletesOwnedCalendar() {
        Calendar calendar = ownedCalendar();
        when(repository.findById(calendar.getId())).thenReturn(Optional.of(calendar));
        when(repository.save(calendar)).thenReturn(calendar);

        Calendar result = service.updateCalendar(calendar.getId(), ownerId,
                new UpdateCalendarRequest(" New ", " ", 2027));
        service.deleteCalendar(calendar.getId(), ownerId);

        assertThat(result.getName()).isEqualTo("New");
        assertThat(result.getSportType()).isNull();
        assertThat(result.getYear()).isEqualTo(2027);
        verify(repository).delete(calendar);
    }

    @Test
    void leavesOmittedFieldsAndRejectsBlankName() {
        Calendar calendar = ownedCalendar();
        when(repository.findById(calendar.getId())).thenReturn(Optional.of(calendar));
        when(repository.save(calendar)).thenReturn(calendar);

        service.updateCalendar(calendar.getId(), ownerId, new UpdateCalendarRequest(null, null, null));
        assertThat(calendar.getName()).isEqualTo("Old");
        assertThatThrownBy(() -> service.updateCalendar(calendar.getId(), ownerId,
                new UpdateCalendarRequest(" ", null, null)))
                .hasMessage("calendar name must not be blank");
    }

    private Calendar ownedCalendar() {
        Calendar calendar = new Calendar();
        calendar.setOwnerId(ownerId);
        calendar.setName("Old");
        calendar.setSportType("Ski");
        calendar.setYear(2026);
        return calendar;
    }
}
