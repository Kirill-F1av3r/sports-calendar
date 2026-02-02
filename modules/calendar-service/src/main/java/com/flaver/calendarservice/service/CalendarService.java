package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.exception.ForbiddenException;
import com.flaver.calendarservice.exception.NotFoundException;
import com.flaver.calendarservice.repository.CalendarRepository;
import com.flaver.calendarservice.repository.EventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CalendarService {
    private final EventRepository eventRepository;
    private final CalendarRepository calendarRepository;

    @Transactional
    public Calendar createCalendar(UUID ownerId, CreateCalendarRequest request) {
        if (request == null || request.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        Calendar calendar = new Calendar();
        calendar.setOwnerId(ownerId);
        calendar.setName(request.name());
        calendar.setSportType(request.sportType());
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

        if (request.title() == null || request.title().isBlank()){
            throw new IllegalArgumentException("title is required");
        }
        Date startDate = parseDate(request.startDate());
        Date endDate = parseDate(request.endDate());
        if (endDate != null && endDate.before(startDate)) throw new IllegalArgumentException("end before start");

        Event event = new Event();
        event.setCalendarId(calendarId);
        event.setTitle(request.title().trim());
        event.setStartDate(startDate);
        event.setEndDate(endDate);
        event.setLocation(request.location());
        event.setSource(request.source());
        return eventRepository.save(event);
    }

    private Date parseDate(String str) {
        if (str == null) throw new IllegalArgumentException("date is required");
        str = str.trim();
        try {
            return Date.valueOf(str);
        } catch (Exception ignored) {
            throw new IllegalArgumentException("invalid date format");
        }
    }
}
