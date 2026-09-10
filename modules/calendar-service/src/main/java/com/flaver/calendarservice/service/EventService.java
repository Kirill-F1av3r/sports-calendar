package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.dto.UpdateEventRequest;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.exception.NotFoundException;
import com.flaver.calendarservice.repository.EventRepository;
import com.flaver.calendarservice.service.sort.SortParser;
import com.flaver.calendarservice.service.specification.EventSpecifications;
import com.flaver.calendarservice.service.util.EventEnumParser;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.flaver.calendarservice.service.util.TextUtils.trimToNull;

@Service
@AllArgsConstructor
public class EventService {
    private static final int DEFAULT_EVENT_PAGE_SIZE = 50;
    private static final int MAX_EVENT_PAGE_SIZE = 100;
    private static final Map<String, String> EVENT_SORT_FIELDS = Map.of(
            "date", "startDate",
            "title", "title",
            "priority", "priority",
            "level", "competitionLevel",
            "created", "createdAt",
            "updated", "updatedAt"
    );

    private final EventRepository eventRepository;
    private final CalendarService calendarService;
    private final SortParser sortParser;

    @Transactional(readOnly = true)
    public Page<Event> findEvents(UUID calendarId,
                                  UUID ownerId,
                                  LocalDate from,
                                  LocalDate to,
                                  String competitionLevel,
                                  String priority,
                                  String search,
                                  Integer page,
                                  Integer size,
                                  String sort) {
        calendarService.findOwnedOrThrow(calendarId, ownerId);
        Pageable pageable = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                sortParser.parse(sort, "date,asc", EVENT_SORT_FIELDS)
        );
        return eventRepository.findAll(
                EventSpecifications.withFilters(
                        calendarId,
                        from,
                        to,
                        EventEnumParser.parseOptionalCompetitionLevel(competitionLevel),
                        EventEnumParser.parseOptionalPriority(priority),
                        search
                ),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Event findOwnedEventOrThrow(UUID calendarId, UUID eventId, UUID ownerId) {
        calendarService.findOwnedOrThrow(calendarId, ownerId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getCalendarId().equals(calendarId)) {
            throw new NotFoundException("Event not found");
        }
        return event;
    }

    @Transactional
    public Event addEvent(UUID calendarId, UUID ownerId, CreateEventRequest request) {
        calendarService.findOwnedOrThrow(calendarId, ownerId);

        var endDate = request.endDate() != null ? request.endDate() : request.startDate();
        if (endDate.isBefore(request.startDate())) {
            throw new IllegalArgumentException("end before start");
        }

        Event event = new Event();
        event.setCalendarId(calendarId);
        event.setTitle(request.title().trim());
        event.setStartDate(request.startDate());
        event.setEndDate(endDate);
        event.setCompetitionLevel(EventEnumParser.parseCompetitionLevelOrDefault(request.competitionLevel()));
        event.setLocation(trimToNull(request.location()));
        event.setExternalUrl(trimToNull(request.externalUrl()));
        event.setDisciplines(normalizeDisciplines(request.disciplines()));
        event.setPriority(EventEnumParser.parsePriorityOrDefault(request.priority()));
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(UUID calendarId, UUID eventId, UUID ownerId, UpdateEventRequest request) {
        Event event = findOwnedEventOrThrow(calendarId, eventId, ownerId);

        if (request.title() != null) {
            String title = request.title().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("event title must not be blank");
            }
            event.setTitle(title);
        }
        if (request.startDate() != null) {
            event.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            event.setEndDate(request.endDate());
        }
        if (event.getEndDate() == null) {
            event.setEndDate(event.getStartDate());
        }
        if (event.getEndDate().isBefore(event.getStartDate())) {
            throw new IllegalArgumentException("end before start");
        }
        if (request.competitionLevel() != null) {
            event.setCompetitionLevel(EventEnumParser.parseCompetitionLevelOrDefault(request.competitionLevel()));
        }
        if (request.location() != null) {
            event.setLocation(trimToNull(request.location()));
        }
        if (request.externalUrl() != null) {
            event.setExternalUrl(trimToNull(request.externalUrl()));
        }
        if (request.disciplines() != null) {
            event.setDisciplines(normalizeDisciplines(request.disciplines()));
        }
        if (request.priority() != null) {
            event.setPriority(EventEnumParser.parsePriorityOrDefault(request.priority()));
        }

        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(UUID calendarId, UUID eventId, UUID ownerId) {
        Event event = findOwnedEventOrThrow(calendarId, eventId, ownerId);
        eventRepository.delete(event);
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

    private int normalizePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_EVENT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_EVENT_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_EVENT_PAGE_SIZE);
        }
        return size;
    }
}
