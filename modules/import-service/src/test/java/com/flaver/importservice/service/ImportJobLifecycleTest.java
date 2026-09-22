package com.flaver.importservice.service;

import com.flaver.dto.calendar.BatchCreateEventsRequest;
import com.flaver.dto.importing.ImportRequestedEvent;
import com.flaver.importservice.config.ImportFilesProperties;
import com.flaver.importservice.dto.CompleteImportRequest;
import com.flaver.importservice.dto.FailImportRequest;
import com.flaver.importservice.entity.ImportDraftEvent;
import com.flaver.importservice.entity.ImportJob;
import com.flaver.importservice.entity.ImportJobStatus;
import com.flaver.importservice.repository.ImportDraftEventRepository;
import com.flaver.importservice.repository.ImportJobRepository;
import com.flaver.importservice.storage.ObjectStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobLifecycleTest {
    @Mock ImportJobRepository jobRepository;
    @Mock ImportDraftEventRepository eventRepository;
    @Mock CalendarClient calendarClient;
    @Mock ObjectStorage objectStorage;
    @Mock KafkaTemplate<String, ImportRequestedEvent> kafkaTemplate;

    private ImportJobService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID calendarId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ImportJobService(jobRepository, eventRepository, calendarClient, objectStorage, kafkaTemplate,
                new ImportFilesProperties(1024, List.of("text/csv")), new DraftEventValidator());
    }

    @Test
    void createsJobStoresFileAndPublishesCalendarContext() {
        MockMultipartFile file = new MockMultipartFile("file", "../events.csv", "text/csv", "a,b".getBytes());
        when(calendarClient.getCalendar(calendarId, userId))
                .thenReturn(new CalendarClient.CalendarInfo(calendarId, "Season", "Ski", 2026));
        when(jobRepository.save(any(ImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createJob(userId, calendarId, file);

        ArgumentCaptor<ImportRequestedEvent> event = ArgumentCaptor.forClass(ImportRequestedEvent.class);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("import.jobs.requested"),
                org.mockito.ArgumentMatchers.anyString(), event.capture());
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.fileName()).isEqualTo(".._events.csv");
        assertThat(event.getValue().calendarYear()).isEqualTo(2026);
        assertThat(event.getValue().sportType()).isEqualTo("Ski");
        verify(objectStorage).put(org.mockito.ArgumentMatchers.contains(".._events.csv"), org.mockito.ArgumentMatchers.eq(file));
    }

    @Test
    void rejectsEmptyLargeAndUnsupportedFilesBeforeDependencies() {
        assertThatThrownBy(() -> service.createJob(userId, calendarId,
                new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0])))
                .hasMessage("file is required");
        assertThatThrownBy(() -> service.createJob(userId, calendarId,
                new MockMultipartFile("file", "large.csv", "text/csv", new byte[1025])))
                .hasMessage("file is too large");
        assertThatThrownBy(() -> service.createJob(userId, calendarId,
                new MockMultipartFile("file", "events.bin", "application/octet-stream", new byte[]{1})))
                .hasMessageContaining("unsupported file content type");
        verify(calendarClient, never()).getCalendar(any(), any());
    }

    @Test
    void handlesProcessingStateIdempotently() {
        ImportJob pending = job(ImportJobStatus.PENDING);
        when(jobRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        assertThat(service.markProcessing(pending.getId()).accepted()).isTrue();
        assertThat(pending.getStatus()).isEqualTo(ImportJobStatus.PROCESSING);
        assertThat(service.markProcessing(pending.getId()).accepted()).isTrue();

        pending.setStatus(ImportJobStatus.READY);
        assertThat(service.markProcessing(pending.getId()).accepted()).isFalse();
    }

    @Test
    void completesJobNormalizingDraftFieldsAndCanFailIt() {
        ImportJob job = job(ImportJobStatus.PROCESSING);
        when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
        CompleteImportRequest.ImportedDraftEventRequest draft = new CompleteImportRequest.ImportedDraftEventRequest(
                " Cup ", LocalDate.of(2026, 2, 1), null, " national ", " Moscow ", null,
                java.util.Arrays.asList(" Sprint ", "", null), " important ", " row 1 ", " raw ");

        service.markComplete(job.getId(), new CompleteImportRequest(List.of(draft)));

        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.READY);
        assertThat(job.getDraftEvents()).singleElement().satisfies(event -> {
            assertThat(event.getTitle()).isEqualTo("Cup");
            assertThat(event.getEndDate()).isEqualTo(event.getStartDate());
            assertThat(event.getCompetitionLevel()).isEqualTo("NATIONAL");
            assertThat(event.getPriority()).isEqualTo("IMPORTANT");
            assertThat(event.getDisciplines()).containsExactly("Sprint");
            assertThat(event.isValid()).isTrue();
        });

        service.markFailed(job.getId(), new FailImportRequest("failed"));
        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("failed");
    }

    @Test
    void appliesOnlyValidReadyDraftsAndIsIdempotent() {
        ImportJob job = job(ImportJobStatus.READY);
        ImportDraftEvent valid = new ImportDraftEvent();
        valid.setTitle("Cup");
        valid.setStartDate(LocalDate.of(2026, 3, 1));
        valid.setEndDate(LocalDate.of(2026, 3, 2));
        valid.setValid(true);
        ImportDraftEvent invalid = new ImportDraftEvent();
        invalid.setValid(false);
        when(jobRepository.findByIdAndUserId(job.getId(), userId)).thenReturn(Optional.of(job));
        when(eventRepository.findByJobIdOrderByCreatedAtAsc(job.getId())).thenReturn(List.of(valid, invalid));
        when(calendarClient.createEvents(org.mockito.ArgumentMatchers.eq(calendarId), org.mockito.ArgumentMatchers.eq(userId),
                any(BatchCreateEventsRequest.class))).thenReturn(1);

        assertThat(service.apply(userId, job.getId()).createdEvents()).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(ImportJobStatus.APPLIED);
        assertThat(service.apply(userId, job.getId()).createdEvents()).isZero();
    }

    private ImportJob job(ImportJobStatus status) {
        ImportJob job = new ImportJob();
        job.setUserId(userId);
        job.setCalendarId(calendarId);
        job.setStatus(status);
        job.setSourceFileName("events.csv");
        job.setSourceObjectKey("imports/events.csv");
        return job;
    }
}
