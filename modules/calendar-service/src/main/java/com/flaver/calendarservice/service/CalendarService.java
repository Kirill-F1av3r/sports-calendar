package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.entity.EventPriority;
import com.flaver.calendarservice.exception.ForbiddenException;
import com.flaver.calendarservice.exception.NotFoundException;
import com.flaver.calendarservice.repository.CalendarRepository;
import com.flaver.calendarservice.repository.EventRepository;
import com.flaver.dto.export.CalendarExportData;
import com.flaver.dto.export.CalendarExportEvent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CalendarService {
    private final EventRepository eventRepository;
    private final CalendarRepository calendarRepository;

    @Transactional
    public Calendar createCalendar(UUID ownerId, CreateCalendarRequest request) {
        Calendar calendar = new Calendar();
        calendar.setOwnerId(ownerId);
        calendar.setName(request.name().trim());
        calendar.setSportType(trimToNull(request.sportType()));
        calendar.setYear(request.year());
        return calendarRepository.save(calendar);
    }

    @Transactional(readOnly = true)
    public List<Calendar> findAllByOwnerId(UUID ownerId) {
        return calendarRepository.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public Calendar findOwnedOrThrow(UUID calendarId, UUID ownerId) {
        Calendar calendar = calendarRepository.findById(calendarId)
                .orElseThrow(() -> new NotFoundException("Calendar not found"));
        if (!calendar.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Not owner of calendar");
        }
        return calendar;
    }

    @Transactional(readOnly = true)
    public List<Event> listEvents(UUID calendarId, UUID ownerId) {
        findOwnedOrThrow(calendarId, ownerId);
        return eventRepository.findByCalendarIdOrderByStartDateAsc(calendarId);
    }

    @Transactional
    public Event addEvent(UUID calendarId, UUID ownerId, CreateEventRequest request) {
        findOwnedOrThrow(calendarId, ownerId);

        var endDate = request.endDate() != null ? request.endDate() : request.startDate();
        if (endDate.isBefore(request.startDate())) {
            throw new IllegalArgumentException("end before start");
        }

        Event event = new Event();
        event.setCalendarId(calendarId);
        event.setTitle(request.title().trim());
        event.setStartDate(request.startDate());
        event.setEndDate(endDate);
        event.setCompetitionLevel(parseCompetitionLevel(request.competitionLevel()));
        event.setLocation(trimToNull(request.location()));
        event.setExternalUrl(trimToNull(request.externalUrl()));
        event.setDisciplines(normalizeDisciplines(request.disciplines()));
        event.setPriority(parsePriority(request.priority()));
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public CalendarExportData getExportData(UUID calendarId, UUID ownerId) {
        Calendar calendar = findOwnedOrThrow(calendarId, ownerId);
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

    private EventPriority parsePriority(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return EventPriority.OPTIONAL;
        }
        try {
            return EventPriority.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("priority must be REQUIRED, IMPORTANT or OPTIONAL");
        }
    }

    private CompetitionLevel parseCompetitionLevel(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return CompetitionLevel.OTHER;
        }
        try {
            return CompetitionLevel.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported competition level");
        }
    }

    private List<String> normalizeDisciplines(List<String> disciplines) {
        if (disciplines == null) {
            return new ArrayList<>();
        }
        return disciplines.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
