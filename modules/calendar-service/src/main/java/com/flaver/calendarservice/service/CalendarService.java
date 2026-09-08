package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.dto.EnumOptionResponse;
import com.flaver.calendarservice.dto.UpdateCalendarRequest;
import com.flaver.calendarservice.dto.UpdateEventRequest;
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
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CalendarService {
    private static final int DEFAULT_EVENT_PAGE_SIZE = 50;
    private static final int MAX_EVENT_PAGE_SIZE = 100;
    private static final Map<String, String> CALENDAR_SORT_FIELDS = Map.of(
            "updated", "updatedAt",
            "created", "createdAt",
            "year", "year",
            "name", "name"
    );
    private static final Map<String, String> EVENT_SORT_FIELDS = Map.of(
            "date", "startDate",
            "title", "title",
            "priority", "priority",
            "level", "competitionLevel",
            "created", "createdAt",
            "updated", "updatedAt"
    );

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
    public List<Calendar> findCalendars(UUID ownerId, Integer year, String sportType, String search, String sort) {
        return calendarRepository.findAll(
                calendarSpecification(ownerId, year, sportType, search),
                parseSort(sort, "updated,desc", CALENDAR_SORT_FIELDS)
        );
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
        findOwnedOrThrow(calendarId, ownerId);
        CompetitionLevel parsedCompetitionLevel = parseOptionalCompetitionLevel(competitionLevel);
        EventPriority parsedPriority = parseOptionalPriority(priority);
        Pageable pageable = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                parseSort(sort, "date,asc", EVENT_SORT_FIELDS)
        );
        return eventRepository.findAll(
                eventSpecification(calendarId, from, to, parsedCompetitionLevel, parsedPriority, search),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Event findOwnedEventOrThrow(UUID calendarId, UUID eventId, UUID ownerId) {
        findOwnedOrThrow(calendarId, ownerId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!event.getCalendarId().equals(calendarId)) {
            throw new NotFoundException("Event not found");
        }
        return event;
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

    @Transactional
    public Calendar updateCalendar(UUID calendarId, UUID ownerId, UpdateCalendarRequest request) {
        Calendar calendar = findOwnedOrThrow(calendarId, ownerId);

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("calendar name must not be blank");
            }
            calendar.setName(name);
        }
        if (request.sportType() != null) {
            calendar.setSportType(trimToNull(request.sportType()));
        }
        if (request.year() != null) {
            calendar.setYear(request.year());
        }

        return calendarRepository.save(calendar);
    }

    @Transactional
    public void deleteCalendar(UUID calendarId, UUID ownerId) {
        Calendar calendar = findOwnedOrThrow(calendarId, ownerId);
        calendarRepository.delete(calendar);
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
            event.setCompetitionLevel(parseCompetitionLevel(request.competitionLevel()));
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
            event.setPriority(parsePriority(request.priority()));
        }

        return eventRepository.save(event);
    }

    @Transactional
    public void deleteEvent(UUID calendarId, UUID eventId, UUID ownerId) {
        Event event = findOwnedEventOrThrow(calendarId, eventId, ownerId);
        eventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public List<EnumOptionResponse> competitionLevelOptions() {
        return Arrays.stream(CompetitionLevel.values())
                .map(level -> new EnumOptionResponse(level.name(), level.getDisplayNameRu()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnumOptionResponse> priorityOptions() {
        return Arrays.stream(EventPriority.values())
                .map(priority -> new EnumOptionResponse(priority.name(), priority.getDisplayNameRu()))
                .toList();
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
        return parseOptionalPriority(normalized);
    }

    private EventPriority parseOptionalPriority(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
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
        return parseOptionalCompetitionLevel(normalized);
    }

    private CompetitionLevel parseOptionalCompetitionLevel(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
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

    private Specification<Calendar> calendarSpecification(UUID ownerId, Integer year, String sportType, String search) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), ownerId));

            if (year != null) {
                predicates.add(cb.equal(root.get("year"), year));
            }

            String normalizedSportType = trimToNull(sportType);
            if (normalizedSportType != null) {
                predicates.add(cb.equal(cb.lower(root.get("sportType")), normalizedSportType.toLowerCase(Locale.ROOT)));
            }

            String normalizedSearch = trimToNull(search);
            if (normalizedSearch != null) {
                String pattern = likePattern(normalizedSearch);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("sportType")), pattern, '\\')
                ));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Specification<Event> eventSpecification(UUID calendarId,
                                                    LocalDate from,
                                                    LocalDate to,
                                                    CompetitionLevel competitionLevel,
                                                    EventPriority priority,
                                                    String search) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("calendarId"), calendarId));

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), to));
            }
            if (competitionLevel != null) {
                predicates.add(cb.equal(root.get("competitionLevel"), competitionLevel));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            String normalizedSearch = trimToNull(search);
            if (normalizedSearch != null) {
                if (query != null) {
                    query.distinct(true);
                }
                ListJoin<Event, String> disciplineJoin = root.joinList("disciplines", JoinType.LEFT);
                String pattern = likePattern(normalizedSearch);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("location")), pattern, '\\'),
                        cb.like(cb.lower(disciplineJoin), pattern, '\\')
                ));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private Sort parseSort(String requestedSort, String defaultSort, Map<String, String> allowedFields) {
        String sortValue = trimToNull(requestedSort);
        if (sortValue == null) {
            sortValue = defaultSort;
        }

        String[] parts = sortValue.split(",");
        String requestedField = parts[0].trim();
        String field = allowedFields.get(requestedField);
        if (field == null) {
            throw new IllegalArgumentException("unsupported sort field");
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("unsupported sort direction");
            }
        }

        return Sort.by(direction, field);
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

    private String likePattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
