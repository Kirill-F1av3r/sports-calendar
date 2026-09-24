package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.UpdateCalendarRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.exception.ForbiddenException;
import com.flaver.calendarservice.exception.NotFoundException;
import com.flaver.calendarservice.repository.CalendarRepository;
import com.flaver.calendarservice.service.sort.SortParser;
import com.flaver.calendarservice.service.specification.CalendarSpecifications;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.flaver.calendarservice.service.util.TextUtils.trimToNull;

@Service
@AllArgsConstructor
public class CalendarService {
    private static final Map<String, String> CALENDAR_SORT_FIELDS = Map.of(
            "updated", "updatedAt",
            "created", "createdAt",
            "year", "year",
            "name", "name"
    );

    private final CalendarRepository calendarRepository;
    private final SortParser sortParser;

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
                CalendarSpecifications.withFilters(ownerId, year, sportType, search),
                sortParser.parse(sort, "updated,desc", CALENDAR_SORT_FIELDS)
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
}
