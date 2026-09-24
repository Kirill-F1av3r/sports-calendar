package com.flaver.exportservice.service;

import com.flaver.dto.export.ExportRequestedEvent;
import com.flaver.exportservice.dto.CompleteExportRequest;
import com.flaver.exportservice.dto.CreateExportRequest;
import com.flaver.exportservice.dto.ExportJobResponse;
import com.flaver.exportservice.dto.FailExportRequest;
import com.flaver.exportservice.entity.ExportJob;
import com.flaver.exportservice.entity.ExportProvider;
import com.flaver.exportservice.entity.ExportStatus;
import com.flaver.exportservice.repository.ExportJobRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ExportJobService {
    private static final String EXPORT_REQUESTED_TOPIC = "export.jobs.requested";

    private final ExportJobRepository exportJobRepository;
    private final CalendarClient calendarClient;
    private final KafkaTemplate<String, ExportRequestedEvent> kafkaTemplate;

    public ExportJobService(ExportJobRepository exportJobRepository,
                            CalendarClient calendarClient,
                            KafkaTemplate<String, ExportRequestedEvent> kafkaTemplate) {
        this.exportJobRepository = exportJobRepository;
        this.calendarClient = calendarClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public ExportJobResponse createJob(UUID userId, CreateExportRequest request) {
        ExportProvider provider = parseProvider(request.provider());

        calendarClient.checkAccess(request.calendarId(), userId);

        ExportJob job = new ExportJob();
        job.setUserId(userId);
        job.setCalendarId(request.calendarId());
        job.setProvider(provider);
        job.setStatus(ExportStatus.PENDING);

        ExportJob saved = exportJobRepository.save(job);
        kafkaTemplate.send(EXPORT_REQUESTED_TOPIC, saved.getId().toString(),
                new ExportRequestedEvent(saved.getId(), userId, request.calendarId(), provider.name()));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ExportJobResponse getJob(UUID userId, UUID jobId) {
        return toResponse(findOwned(jobId, userId));
    }

    @Transactional
    public void markProcessing(UUID jobId) {
        ExportJob job = find(jobId);
        if (job.getStatus() == ExportStatus.PENDING) {
            job.setStatus(ExportStatus.PROCESSING);
        }
    }

    @Transactional
    public void markSuccess(UUID jobId, CompleteExportRequest request) {
        ExportJob job = find(jobId);
        job.setStatus(ExportStatus.SUCCESS);
        job.setSpreadsheetId(request.spreadsheetId());
        job.setSpreadsheetUrl(request.spreadsheetUrl());
        job.setErrorMessage(null);
    }

    @Transactional
    public void markFailed(UUID jobId, FailExportRequest request) {
        ExportJob job = find(jobId);
        job.setStatus(ExportStatus.FAILED);
        job.setErrorMessage(request.errorMessage() != null ? request.errorMessage() : "Export failed");
    }

    private ExportJob find(UUID jobId) {
        return exportJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("export job not found"));
    }

    private ExportJob findOwned(UUID jobId, UUID userId) {
        return exportJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new IllegalArgumentException("export job not found"));
    }

    private ExportProvider parseProvider(String provider) {
        try {
            return ExportProvider.valueOf(provider.trim().toUpperCase());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("unsupported export provider");
        }
    }

    private ExportJobResponse toResponse(ExportJob job) {
        return new ExportJobResponse(
                job.getId().toString(),
                job.getProvider().name(),
                job.getStatus().name(),
                job.getSpreadsheetId(),
                job.getSpreadsheetUrl(),
                job.getErrorMessage()
        );
    }
}
