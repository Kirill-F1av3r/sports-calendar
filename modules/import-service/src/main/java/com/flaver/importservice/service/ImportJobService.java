package com.flaver.importservice.service;

import com.flaver.dto.calendar.BatchCreateEventRequest;
import com.flaver.dto.calendar.BatchCreateEventsRequest;
import com.flaver.dto.importing.ImportRequestedEvent;
import com.flaver.importservice.config.ImportFilesProperties;
import com.flaver.importservice.dto.*;
import com.flaver.importservice.entity.ImportDraftEvent;
import com.flaver.importservice.entity.ImportJob;
import com.flaver.importservice.entity.ImportJobStatus;
import com.flaver.importservice.repository.ImportDraftEventRepository;
import com.flaver.importservice.repository.ImportJobRepository;
import com.flaver.importservice.storage.ObjectStorage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class ImportJobService {
    private static final String IMPORT_REQUESTED_TOPIC = "import.jobs.requested";

    private final ImportJobRepository importJobRepository;
    private final ImportDraftEventRepository draftEventRepository;
    private final CalendarClient calendarClient;
    private final ObjectStorage objectStorage;
    private final KafkaTemplate<String, ImportRequestedEvent> kafkaTemplate;
    private final ImportFilesProperties filesProperties;
    private final DraftEventValidator draftEventValidator;

    public ImportJobService(ImportJobRepository importJobRepository,
                            ImportDraftEventRepository draftEventRepository,
                            CalendarClient calendarClient,
                            ObjectStorage objectStorage,
                            KafkaTemplate<String, ImportRequestedEvent> kafkaTemplate,
                            ImportFilesProperties filesProperties,
                            DraftEventValidator draftEventValidator) {
        this.importJobRepository = importJobRepository;
        this.draftEventRepository = draftEventRepository;
        this.calendarClient = calendarClient;
        this.objectStorage = objectStorage;
        this.kafkaTemplate = kafkaTemplate;
        this.filesProperties = filesProperties;
        this.draftEventValidator = draftEventValidator;
    }

    @Transactional
    public ImportJobResponse createJob(UUID userId, UUID calendarId, MultipartFile file) {
        validateFile(file);
        CalendarClient.CalendarInfo calendar = calendarClient.getCalendar(calendarId, userId);

        ImportJob job = new ImportJob();
        job.setUserId(userId);
        job.setCalendarId(calendarId);
        job.setStatus(ImportJobStatus.PENDING);
        job.setSourceFileName(normalizeFileName(file.getOriginalFilename()));
        job.setSourceContentType(file.getContentType());
        job.setSourceFileSize(file.getSize());
        job.setSourceObjectKey(buildObjectKey(userId, job.getId(), job.getSourceFileName()));

        ImportJob saved = importJobRepository.save(job);
        objectStorage.put(saved.getSourceObjectKey(), file);

        kafkaTemplate.send(IMPORT_REQUESTED_TOPIC, saved.getId().toString(),
                new ImportRequestedEvent(
                        saved.getId(),
                        userId,
                        calendarId,
                        calendar != null ? calendar.year() : null,
                        calendar != null ? calendar.sportType() : null,
                        saved.getSourceObjectKey(),
                        saved.getSourceFileName(),
                        saved.getSourceContentType()
                ));

        return ImportMapper.toJobResponse(saved);
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getJob(UUID userId, UUID jobId) {
        return ImportMapper.toJobResponse(findOwned(jobId, userId));
    }

    @Transactional(readOnly = true)
    public DraftEventsResponse getEvents(UUID userId, UUID jobId) {
        ImportJob job = findOwned(jobId, userId);
        List<DraftEventResponse> events = draftEventRepository.findByJobIdOrderByCreatedAtAsc(job.getId()).stream()
                .map(ImportMapper::toDraftEventResponse)
                .toList();
        return new DraftEventsResponse(events);
    }

    @Transactional
    public DraftEventResponse updateDraftEvent(UUID userId, UUID jobId, UUID draftEventId, UpdateDraftEventRequest request) {
        ImportJob job = findOwned(jobId, userId);
        ensureEditable(job);
        ImportDraftEvent event = draftEventRepository.findByIdAndJobId(draftEventId, jobId)
                .orElseThrow(() -> new IllegalArgumentException("draft event not found"));

        event.setTitle(trimToNull(request.title()));
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setCompetitionLevel(trimToNull(request.competitionLevel()));
        event.setLocation(trimToNull(request.location()));
        event.setExternalUrl(trimToNull(request.externalUrl()));
        event.setPriority(trimToNull(request.priority()));
        event.setDisciplines(normalizeDisciplines(request.disciplines()));

        draftEventValidator.validate(event);
        return ImportMapper.toDraftEventResponse(draftEventRepository.save(event));
    }

    @Transactional
    public void deleteDraftEvent(UUID userId, UUID jobId, UUID draftEventId) {
        ImportJob job = findOwned(jobId, userId);
        ensureEditable(job);
        ImportDraftEvent event = draftEventRepository.findByIdAndJobId(draftEventId, jobId)
                .orElseThrow(() -> new IllegalArgumentException("draft event not found"));
        draftEventRepository.delete(event);
    }

    @Transactional
    public ApplyImportResponse apply(UUID userId, UUID jobId) {
        ImportJob job = findOwned(jobId, userId);
        if (job.getStatus() == ImportJobStatus.APPLIED) {
            return new ApplyImportResponse(0);
        }
        if (job.getStatus() != ImportJobStatus.READY) {
            throw new IllegalArgumentException("import job is not ready");
        }

        List<ImportDraftEvent> validEvents = draftEventRepository.findByJobIdOrderByCreatedAtAsc(jobId).stream()
                .filter(ImportDraftEvent::isValid)
                .toList();
        if (validEvents.isEmpty()) {
            throw new IllegalArgumentException("there are no valid events to apply");
        }

        int created = calendarClient.createEvents(job.getCalendarId(), userId,
                new BatchCreateEventsRequest(validEvents.stream()
                        .map(this::toBatchRequest)
                        .toList()));

        job.setStatus(ImportJobStatus.APPLIED);
        job.setAppliedAt(Instant.now());
        return new ApplyImportResponse(created);
    }

    @Transactional
    public ProcessingImportResponse markProcessing(UUID jobId) {
        ImportJob job = find(jobId);
        if (job.getStatus() == ImportJobStatus.PENDING) {
            job.setStatus(ImportJobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            job.setErrorMessage(null);
            return new ProcessingImportResponse(true, job.getStatus().name());
        }
        return new ProcessingImportResponse(job.getStatus() == ImportJobStatus.PROCESSING, job.getStatus().name());
    }

    @Transactional
    public void markComplete(UUID jobId, CompleteImportRequest request) {
        ImportJob job = find(jobId);
        if (job.getStatus() == ImportJobStatus.APPLIED) {
            return;
        }

        job.getDraftEvents().clear();
        for (CompleteImportRequest.ImportedDraftEventRequest draftRequest : request.events()) {
            ImportDraftEvent event = toDraftEvent(job, draftRequest);
            draftEventValidator.validate(event);
            job.getDraftEvents().add(event);
        }

        job.setStatus(ImportJobStatus.READY);
        job.setFinishedAt(Instant.now());
        job.setErrorMessage(null);
    }

    @Transactional
    public void markFailed(UUID jobId, FailImportRequest request) {
        ImportJob job = find(jobId);
        if (job.getStatus() == ImportJobStatus.APPLIED) {
            return;
        }
        job.setStatus(ImportJobStatus.FAILED);
        job.setFinishedAt(Instant.now());
        job.setErrorMessage(request.errorMessage());
    }

    private ImportJob find(UUID jobId) {
        return importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("import job not found"));
    }

    private ImportJob findOwned(UUID jobId, UUID userId) {
        return importJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new IllegalArgumentException("import job not found"));
    }

    private void ensureEditable(ImportJob job) {
        if (job.getStatus() != ImportJobStatus.READY) {
            throw new IllegalArgumentException("draft events can be edited only when import is ready");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (file.getSize() > filesProperties.maxSizeBytes()) {
            throw new IllegalArgumentException("file is too large");
        }
        String contentType = file.getContentType();
        if (!hasSupportedExtension(file.getOriginalFilename())
                && contentType != null
                && filesProperties.allowedContentTypes() != null
                && !filesProperties.allowedContentTypes().isEmpty()
                && !filesProperties.allowedContentTypes().contains(contentType)) {
            throw new IllegalArgumentException("unsupported file content type: " + contentType);
        }
    }

    private String buildObjectKey(UUID userId, UUID jobId, String fileName) {
        return "imports/" + userId + "/" + jobId + "/" + fileName;
    }

    private String normalizeFileName(String fileName) {
        String normalized = trimToNull(fileName);
        if (normalized == null) {
            return "import-file";
        }
        return normalized.replace("\\", "_").replace("/", "_");
    }

    private boolean hasSupportedExtension(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".xlsx")
                || lower.endsWith(".xls")
                || lower.endsWith(".csv")
                || lower.endsWith(".txt")
                || lower.endsWith(".pdf")
                || lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".webp");
    }

    private ImportDraftEvent toDraftEvent(ImportJob job, CompleteImportRequest.ImportedDraftEventRequest request) {
        ImportDraftEvent event = new ImportDraftEvent();
        event.setJob(job);
        event.setTitle(trimToNull(request.title()));
        event.setStartDate(request.startDate());
        event.setEndDate(request.endDate());
        event.setCompetitionLevel(upperToNull(request.competitionLevel()));
        event.setLocation(trimToNull(request.location()));
        event.setExternalUrl(trimToNull(request.externalUrl()));
        event.setDisciplines(normalizeDisciplines(request.disciplines()));
        event.setPriority(upperToNull(request.priority()));
        event.setSourceReference(trimToNull(request.sourceReference()));
        event.setRawText(trimToNull(request.rawText()));
        return event;
    }

    private BatchCreateEventRequest toBatchRequest(ImportDraftEvent event) {
        return new BatchCreateEventRequest(
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getCompetitionLevel(),
                event.getLocation(),
                event.getExternalUrl(),
                event.getDisciplines(),
                event.getPriority()
        );
    }

    private List<String> normalizeDisciplines(List<String> disciplines) {
        if (disciplines == null) {
            return new ArrayList<>();
        }
        return disciplines.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private String upperToNull(String value) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.equalsIgnoreCase("null") || normalized.equals("-")) {
            return null;
        }
        return normalized.toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
