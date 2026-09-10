package com.flaver.calendarservice.web;

import com.flaver.calendarservice.dto.CalendarMapper;
import com.flaver.calendarservice.dto.CalendarResponse;
import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.dto.EventResponse;
import com.flaver.calendarservice.dto.PageResponse;
import com.flaver.calendarservice.dto.UpdateCalendarRequest;
import com.flaver.calendarservice.dto.UpdateEventRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.service.CalendarExportService;
import com.flaver.calendarservice.service.CalendarCopyService;
import com.flaver.calendarservice.service.CalendarService;
import com.flaver.calendarservice.service.EventService;
import com.flaver.dto.export.CalendarExportData;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/calendars")
@AllArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;
    private final EventService eventService;
    private final CalendarExportService calendarExportService;
    private final CalendarCopyService calendarCopyService;

    @PostMapping
    public ResponseEntity<CalendarResponse> createCalendar(@RequestHeader("X-User-Id") String userId,
                                                           @Valid @RequestBody CreateCalendarRequest body) {
        UUID ownerId = UUID.fromString(userId);
        Calendar calendar = calendarService.createCalendar(ownerId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(CalendarMapper.toCalendarResponse(calendar));
    }

    @GetMapping
    public ResponseEntity<List<CalendarResponse>> list(@RequestHeader("X-User-Id") String userId,
                                                       @RequestParam(name = "year", required = false) Integer year,
                                                       @RequestParam(name = "sportType", required = false) String sportType,
                                                       @RequestParam(name = "search", required = false) String search,
                                                       @RequestParam(name = "sort", required = false) String sort) {
        UUID ownerId = UUID.fromString(userId);
        List<CalendarResponse> list = calendarService.findCalendars(ownerId, year, sportType, search, sort)
                .stream()
                .map(CalendarMapper::toCalendarResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CalendarResponse> get(@PathVariable("id") UUID id,
                                                @RequestHeader("X-User-Id") String userId) {
        UUID ownerId = UUID.fromString(userId);
        Calendar calendar = calendarService.findOwnedOrThrow(id, ownerId);
        return ResponseEntity.ok(CalendarMapper.toCalendarResponse(calendar));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CalendarResponse> updateCalendar(@PathVariable("id") UUID id,
                                                           @RequestHeader("X-User-Id") String userId,
                                                           @Valid @RequestBody UpdateCalendarRequest body) {
        UUID ownerId = UUID.fromString(userId);
        Calendar calendar = calendarService.updateCalendar(id, ownerId, body);
        return ResponseEntity.ok(CalendarMapper.toCalendarResponse(calendar));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCalendar(@PathVariable("id") UUID id,
                                               @RequestHeader("X-User-Id") String userId) {
        UUID ownerId = UUID.fromString(userId);
        calendarService.deleteCalendar(id, ownerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/copies")
    public ResponseEntity<CalendarResponse> copyCalendar(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "competitionLevel", required = false) String competitionLevel,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "search", required = false) String search,
            @Valid @RequestBody CreateCalendarRequest body) {
        UUID ownerId = UUID.fromString(userId);
        Calendar calendar = calendarCopyService.copyCalendar(id, ownerId, body, from, to, competitionLevel, priority, search);
        return ResponseEntity.status(HttpStatus.CREATED).body(CalendarMapper.toCalendarResponse(calendar));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<PageResponse<EventResponse>> listEvents(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "competitionLevel", required = false) String competitionLevel,
            @RequestParam(name = "priority", required = false) String priority,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "sort", required = false) String sort) {
        UUID ownerId = UUID.fromString(userId);
        Page<EventResponse> events = eventService.findEvents(id, ownerId, from, to, competitionLevel, priority,
                        search, page, size, sort)
                .map(CalendarMapper::toEventResponse);
        return ResponseEntity.ok(new PageResponse<>(
                events.getContent(),
                events.getNumber(),
                events.getSize(),
                events.getTotalElements(),
                events.getTotalPages(),
                events.isLast()
        ));
    }

    @GetMapping("/{calendarId}/events/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable("calendarId") UUID calendarId,
                                                  @PathVariable("eventId") UUID eventId,
                                                  @RequestHeader("X-User-Id") String userId) {
        UUID ownerId = UUID.fromString(userId);
        Event event = eventService.findOwnedEventOrThrow(calendarId, eventId, ownerId);
        return ResponseEntity.ok(CalendarMapper.toEventResponse(event));
    }

    @PostMapping("/{id}/events")
    public ResponseEntity<EventResponse> addEvent(@PathVariable("id") UUID id,
                                                  @RequestHeader("X-User-Id") String userId,
                                                  @Valid @RequestBody CreateEventRequest body) {
        UUID ownerId = UUID.fromString(userId);
        Event event = eventService.addEvent(id, ownerId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(CalendarMapper.toEventResponse(event));
    }

    @PatchMapping("/{calendarId}/events/{eventId}")
    public ResponseEntity<EventResponse> updateEvent(@PathVariable("calendarId") UUID calendarId,
                                                     @PathVariable("eventId") UUID eventId,
                                                     @RequestHeader("X-User-Id") String userId,
                                                     @Valid @RequestBody UpdateEventRequest body) {
        UUID ownerId = UUID.fromString(userId);
        Event event = eventService.updateEvent(calendarId, eventId, ownerId, body);
        return ResponseEntity.ok(CalendarMapper.toEventResponse(event));
    }

    @DeleteMapping("/{calendarId}/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("calendarId") UUID calendarId,
                                            @PathVariable("eventId") UUID eventId,
                                            @RequestHeader("X-User-Id") String userId) {
        UUID ownerId = UUID.fromString(userId);
        eventService.deleteEvent(calendarId, eventId, ownerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/{id}/access-check")
    public ResponseEntity<Void> checkAccess(@PathVariable("id") UUID id,
                                            @RequestHeader("X-User-Id") String userId) {
        UUID ownerId = UUID.fromString(userId);
        calendarService.findOwnedOrThrow(id, ownerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/{id}/export-data")
    public ResponseEntity<CalendarExportData> exportData(@PathVariable("id") UUID id,
                                                         @RequestHeader("X-User-Id") String userId) {
        UUID ownerId = UUID.fromString(userId);
        return ResponseEntity.ok(calendarExportService.getExportData(id, ownerId));
    }
}
