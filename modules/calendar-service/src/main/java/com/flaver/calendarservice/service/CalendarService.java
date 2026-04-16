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

import java.time.LocalDate;
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

        LocalDate endDate = request.endDate();
        if (endDate.isBefore(request.startDate())) {
            throw new IllegalArgumentException("end before start");
        }

        Event event = new Event();
        event.setCalendarId(calendarId);
        event.setTitle(request.title().trim());
        event.setStartDate(request.startDate());
        event.setEndDate(endDate);
        event.setLocation(request.location().trim());
        event.setSource(request.source());
        return eventRepository.save(event);
    }
}
