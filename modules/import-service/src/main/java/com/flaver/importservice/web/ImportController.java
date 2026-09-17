package com.flaver.importservice.web;

import com.flaver.importservice.dto.*;
import com.flaver.importservice.service.ImportJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/imports")
public class ImportController {
    private final ImportJobService importJobService;

    public ImportController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportJobResponse> createImport(@RequestHeader("X-User-Id") String userId,
                                                          @RequestParam("calendarId") UUID calendarId,
                                                          @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(importJobService.createJob(UUID.fromString(userId), calendarId, file));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ImportJobResponse> getJob(@RequestHeader("X-User-Id") String userId,
                                                    @PathVariable("jobId") UUID jobId) {
        return ResponseEntity.ok(importJobService.getJob(UUID.fromString(userId), jobId));
    }

    @GetMapping("/{jobId}/events")
    public ResponseEntity<DraftEventsResponse> getEvents(@RequestHeader("X-User-Id") String userId,
                                                         @PathVariable("jobId") UUID jobId) {
        return ResponseEntity.ok(importJobService.getEvents(UUID.fromString(userId), jobId));
    }

    @PatchMapping("/{jobId}/events/{draftEventId}")
    public ResponseEntity<DraftEventResponse> updateEvent(@RequestHeader("X-User-Id") String userId,
                                                          @PathVariable("jobId") UUID jobId,
                                                          @PathVariable("draftEventId") UUID draftEventId,
                                                          @Valid @RequestBody UpdateDraftEventRequest request) {
        return ResponseEntity.ok(importJobService.updateDraftEvent(UUID.fromString(userId), jobId, draftEventId, request));
    }

    @DeleteMapping("/{jobId}/events/{draftEventId}")
    public ResponseEntity<Void> deleteEvent(@RequestHeader("X-User-Id") String userId,
                                            @PathVariable("jobId") UUID jobId,
                                            @PathVariable("draftEventId") UUID draftEventId) {
        importJobService.deleteDraftEvent(UUID.fromString(userId), jobId, draftEventId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/apply")
    public ResponseEntity<ApplyImportResponse> apply(@RequestHeader("X-User-Id") String userId,
                                                     @PathVariable("jobId") UUID jobId) {
        return ResponseEntity.ok(importJobService.apply(UUID.fromString(userId), jobId));
    }
}
