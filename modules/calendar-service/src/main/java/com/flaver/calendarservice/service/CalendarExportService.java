package com.flaver.calendarservice.service;

import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.repository.EventRepository;
import com.flaver.dto.export.CalendarExportData;
import com.flaver.dto.export.CalendarExportEvent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CalendarExportService {
    private final EventRepository eventRepository;
    private final CalendarService calendarService;

    @Transactional(readOnly = true)
    public CalendarExportData getExportData(UUID calendarId, UUID ownerId) {
        Calendar calendar = calendarService.findOwnedOrThrow(calendarId, ownerId);
        List<CalendarExportEvent> events = eventRepository.findByCalendarIdOrderByStartDateAsc(calendarId)
                .stream()
                .map(event -> new CalendarExportEvent(
                        event.getId(),
                        event.getTitle(),
                        event.getStartDate(),
                        event.getEndDate(),
                        event.getCompetitionLevel() != null ? event.getCompetitionLevel().getDisplayNameRu() : "",
                        event.getLocation(),
                        event.getDisciplines(),
                        event.getPriority() != null ? event.getPriority().getDisplayNameRu() : "",
                        event.getExternalUrl(),
                        calendar.getSportType()
                ))
                .toList();

        return new CalendarExportData(calendar.getId(), calendar.getName(), calendar.getSportType(), calendar.getYear(), events);
    }
}
