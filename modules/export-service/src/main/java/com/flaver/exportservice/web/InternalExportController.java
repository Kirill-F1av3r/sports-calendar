package com.flaver.exportservice.web;

import com.flaver.exportservice.dto.CompleteExportRequest;
import com.flaver.exportservice.dto.FailExportRequest;
import com.flaver.exportservice.service.ExportJobService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/exports")
public class InternalExportController {
    private final ExportJobService exportJobService;

    public InternalExportController(ExportJobService exportJobService) {
        this.exportJobService = exportJobService;
    }

    @PostMapping("/{jobId}/processing")
    public ResponseEntity<Void> markProcessing(@PathVariable("jobId") UUID jobId) {
        exportJobService.markProcessing(jobId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/success")
    public ResponseEntity<Void> markSuccess(@PathVariable("jobId") UUID jobId,
                                            @Valid @RequestBody CompleteExportRequest request) {
        exportJobService.markSuccess(jobId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobId}/failed")
    public ResponseEntity<Void> markFailed(@PathVariable("jobId") UUID jobId,
                                           @Valid @RequestBody FailExportRequest request) {
        exportJobService.markFailed(jobId, request);
        return ResponseEntity.noContent().build();
    }
}
