package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.entity.EventPriority;
import com.flaver.calendarservice.repository.EventRepository;
import com.flaver.calendarservice.service.specification.EventSpecifications;
import com.flaver.calendarservice.service.util.EventEnumParser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CalendarCopyService {
    private final CalendarService calendarService;
    private final EventRepository eventRepository;

    @Transactional
    public Calendar copyCalendar(UUID sourceCalendarId,
                                 UUID ownerId,
                                 CreateCalendarRequest request,
                                 LocalDate from,
                                 LocalDate to,
                                 String competitionLevel,
                                 String priority,
                                 String search) {
        calendarService.findOwnedOrThrow(sourceCalendarId, ownerId);

        CompetitionLevel parsedCompetitionLevel = EventEnumParser.parseOptionalCompetitionLevel(competitionLevel);
        EventPriority parsedPriority = EventEnumParser.parseOptionalPriority(priority);

        Calendar copiedCalendar = calendarService.createCalendar(ownerId, request);
        List<Event> sourceEvents = eventRepository.findAll(
                EventSpecifications.withFilters(sourceCalendarId, from, to, parsedCompetitionLevel, parsedPriority, search)
        );
        List<Event> copiedEvents = sourceEvents.stream()
                .map(sourceEvent -> copyEvent(sourceEvent, copiedCalendar.getId()))
                .toList();

        eventRepository.saveAll(copiedEvents);
        return copiedCalendar;
    }

    private Event copyEvent(Event sourceEvent, UUID targetCalendarId) {
        Event copiedEvent = new Event();
        copiedEvent.setCalendarId(targetCalendarId);
        copiedEvent.setTitle(sourceEvent.getTitle());
        copiedEvent.setStartDate(sourceEvent.getStartDate());
        copiedEvent.setEndDate(sourceEvent.getEndDate());
        copiedEvent.setCompetitionLevel(sourceEvent.getCompetitionLevel());
        copiedEvent.setLocation(sourceEvent.getLocation());
        copiedEvent.setExternalUrl(sourceEvent.getExternalUrl());
        copiedEvent.setDisciplines(sourceEvent.getDisciplines() != null
                ? new ArrayList<>(sourceEvent.getDisciplines())
                : new ArrayList<>());
        copiedEvent.setPriority(sourceEvent.getPriority());
        return copiedEvent;
    }
}
