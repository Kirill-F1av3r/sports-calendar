package com.flaver.calendarservice.web;

import com.flaver.calendarservice.dto.CreateCalendarRequest;
import com.flaver.calendarservice.dto.CreateEventRequest;
import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.service.CalendarService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/calendars")
@AllArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;

    @PostMapping
    public ResponseEntity<?> createCalendar(@RequestHeader("X-User-Id") String userId,
                                            @RequestBody CreateCalendarRequest body) {
        UUID ownerId = UUID.fromString(userId);
        Calendar calendar = calendarService.createCalendar(ownerId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", calendar.getId()));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader("X-User-Id") String userId) {
        UUID ownerId = UUID.fromString(userId);
        List<Calendar> list = calendarService.findAllByOwnerId(ownerId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable("id") UUID id,
                                 @RequestHeader("X-User-Id") String userId) {
        UUID ownerId = UUID.fromString(userId);
        Calendar calendar = calendarService.findOwnedOrThrow(id, ownerId);
        List<Event> events = calendarService.listEvents(id, ownerId);
        return ResponseEntity.ok(Map.of("calendar", calendar, "events", events));
    }

    @PostMapping("/{id}/events")
    public ResponseEntity<?> addEvent(@PathVariable("id") UUID id,
                                      @RequestHeader("X-User-Id") String userId,
                                      @RequestBody CreateEventRequest body) {
        UUID ownerId = UUID.fromString(userId);
        Event event = calendarService.addEvent(id, ownerId, body);
        return ResponseEntity.status(201).body(Map.of("id", event.getId()));
    }
}
