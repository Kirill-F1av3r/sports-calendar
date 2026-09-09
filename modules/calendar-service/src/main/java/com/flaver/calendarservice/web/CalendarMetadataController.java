package com.flaver.calendarservice.web;

import com.flaver.calendarservice.dto.CalendarMetadataResponse;
import com.flaver.calendarservice.service.CalendarMetadataService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/calendars")
@AllArgsConstructor
public class CalendarMetadataController {
    private final CalendarMetadataService calendarMetadataService;

    @GetMapping("/metadata")
    public ResponseEntity<CalendarMetadataResponse> metadata() {
        return ResponseEntity.ok(new CalendarMetadataResponse(
                calendarMetadataService.competitionLevelOptions(),
                calendarMetadataService.priorityOptions()
        ));
    }
}
