package com.flaver.importservice.web;

import com.flaver.importservice.dto.CompleteImportRequest;
import com.flaver.importservice.dto.FailImportRequest;
import com.flaver.importservice.dto.ProcessingImportResponse;
import com.flaver.importservice.service.ImportJobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/imports")
public class InternalImportController {
    private final ImportJobService importJobService;

    public InternalImportController(ImportJobService importJobService) {
        this.importJobService = importJobService;
    }

    @PostMapping("/{jobId}/processing")
    public ResponseEntity<ProcessingImportResponse> markProcessing(@PathVariable("jobId") UUID jobId) {
        return ResponseEntity.ok(importJobService.markProcessing(jobId));
    }

    @PostMapping("/{jobId}/complete")
    public ResponseEntity<Void> markComplete(@PathVariable("jobId") UUID jobId,
                                             @Valid @RequestBody CompleteImportRequest request) {
        importJobService.markComplete(jobId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/failed")
    public ResponseEntity<Void> markFailed(@PathVariable("jobId") UUID jobId,
                                           @Valid @RequestBody FailImportRequest request) {
        importJobService.markFailed(jobId, request);
        return ResponseEntity.noContent().build();
    }
}
