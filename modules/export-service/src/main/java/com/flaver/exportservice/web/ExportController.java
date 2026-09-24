package com.flaver.exportservice.web;

import com.flaver.exportservice.dto.CreateExportRequest;
import com.flaver.exportservice.dto.ExportJobResponse;
import com.flaver.exportservice.service.ExportJobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/exports")
public class ExportController {

    private final ExportJobService exportJobService;

    public ExportController(ExportJobService exportJobService) {
        this.exportJobService = exportJobService;
    }

    @PostMapping
    public ResponseEntity<ExportJobResponse> createExport(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateExportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(exportJobService.createJob(UUID.fromString(userId), request));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ExportJobResponse> getJob(@RequestHeader("X-User-Id") String userId,
                                                    @PathVariable("jobId") UUID jobId) {
        return ResponseEntity.ok(exportJobService.getJob(UUID.fromString(userId), jobId));
    }
}
