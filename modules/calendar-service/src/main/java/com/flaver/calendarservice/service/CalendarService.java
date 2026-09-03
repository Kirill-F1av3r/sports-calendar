package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.entity.EventPriority;
import com.flaver.calendarservice.entity.EventStatus;
import com.flaver.calendarservice.exception.ForbiddenException;
import com.flaver.calendarservice.exception.NotFoundException;
import com.flaver.calendarservice.repository.CalendarRepository;
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
public class CalendarService {
    private final EventRepository eventRepository;
    private final CalendarRepository calendarRepository;

    @Transactional
    public Calendar createCalendar(UUID ownerId, CreateCalendarRequest request) {
        Calendar calendar = new Calendar();
        calendar.setOwnerId(ownerId);
        calendar.setName(request.name().trim());
        calendar.setSportType(request.sportType());
        calendar.setYear(request.year());
        calendar.setGoal(trimToNull(request.goal()));
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
        return eventRepository.findByCalendarIdOrderByStartDateTimeAsc(calendarId);
    }

    @Transactional
    public Event addEvent(UUID calendarId, UUID ownerId, CreateEventRequest request) {
        findOwnedOrThrow(calendarId, ownerId);

        var endDateTime = request.endDateTime() != null ? request.endDateTime() : request.startDateTime();
        if (endDateTime.isBefore(request.startDateTime())) {
            throw new IllegalArgumentException("end before start");
        }

        Event event = new Event();
        event.setCalendarId(calendarId);
        event.setTitle(request.title().trim());
        event.setStartDateTime(request.startDateTime());
        event.setEndDateTime(endDateTime);
        event.setTimezone(request.timezone().trim());
        event.setLocation(trimToNull(request.location()));
        event.setDistance(trimToNull(request.distance()));
        event.setPriority(parsePriority(request.priority()));
        event.setStatus(parseStatus(request.status()));
        event.setSource(trimToNull(request.source()));
        event.setExternalUrl(trimToNull(request.externalUrl()));
        event.setNotes(trimToNull(request.notes()));
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public CalendarExportData getExportData(UUID calendarId, UUID ownerId) {
        Calendar calendar = findOwnedOrThrow(calendarId, ownerId);
        List<CalendarExportEvent> events = eventRepository.findByCalendarIdOrderByStartDateTimeAsc(calendarId)
                .stream()
                .map(event -> new CalendarExportEvent(
                        event.getId(),
                        event.getTitle(),
                        event.getStartDateTime(),
                        event.getEndDateTime(),
                        event.getTimezone(),
                        event.getLocation(),
                        event.getDistance(),
                        event.getPriority() != null ? event.getPriority().name() : null,
                        event.getStatus() != null ? event.getStatus().name() : null,
                        event.getSource(),
                        event.getExternalUrl(),
                        event.getNotes()
                ))
                .toList();

        return new CalendarExportData(calendar.getId(), calendar.getName(), calendar.getYear(), calendar.getGoal(), events);
    }

    private EventPriority parsePriority(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return EventPriority.C;
        }
        try {
            return EventPriority.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("priority must be A, B or C");
        }
    }

    private EventStatus parseStatus(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return EventStatus.PLANNED;
        }
        try {
            return EventStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported event status");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
