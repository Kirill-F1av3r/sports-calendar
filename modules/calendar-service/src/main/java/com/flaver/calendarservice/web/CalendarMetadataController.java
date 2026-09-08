package com.flaver.calendarservice.web;

import com.flaver.calendarservice.dto.CalendarMetadataResponse;
import com.flaver.calendarservice.service.CalendarService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class CalendarMetadataController {
    private final CalendarService calendarService;

    @GetMapping("/calendar-metadata")
    public ResponseEntity<CalendarMetadataResponse> metadata() {
        return ResponseEntity.ok(new CalendarMetadataResponse(
                calendarService.competitionLevelOptions(),
                calendarService.priorityOptions()
        ));
    }
}
