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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportJobServiceTest {
    @Mock ExportJobRepository repository;
    @Mock CalendarClient calendarClient;
    @Mock KafkaTemplate<String, ExportRequestedEvent> kafkaTemplate;

    private ExportJobService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID calendarId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ExportJobService(repository, calendarClient, kafkaTemplate);
    }

    @Test
    void createsJobAfterAccessCheckAndPublishesEvent() {
        when(repository.save(any(ExportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExportJobResponse response = service.createJob(userId, new CreateExportRequest(calendarId, " google_sheets "));

        ArgumentCaptor<ExportRequestedEvent> event = ArgumentCaptor.forClass(ExportRequestedEvent.class);
        verify(calendarClient).checkAccess(calendarId, userId);
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("export.jobs.requested"),
                org.mockito.ArgumentMatchers.anyString(), event.capture());
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.provider()).isEqualTo("GOOGLE_SHEETS");
        assertThat(event.getValue().jobId().toString()).isEqualTo(response.jobId());
    }

    @Test
    void rejectsUnsupportedProviderBeforeAccessCheck() {
        assertThatThrownBy(() -> service.createJob(userId, new CreateExportRequest(calendarId, "csv")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported export provider");
        verify(calendarClient, never()).checkAccess(any(), any());
    }

    @Test
    void returnsOnlyOwnedJob() {
        ExportJob job = job(ExportStatus.PROCESSING);
        when(repository.findByIdAndUserId(job.getId(), userId)).thenReturn(Optional.of(job));

        assertThat(service.getJob(userId, job.getId()).status()).isEqualTo("PROCESSING");
        assertThatThrownBy(() -> service.getJob(userId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("export job not found");
    }

    @Test
    void transitionsPendingJobThroughSuccess() {
        ExportJob job = job(ExportStatus.PENDING);
        when(repository.findById(job.getId())).thenReturn(Optional.of(job));

        service.markProcessing(job.getId());
        service.markSuccess(job.getId(), new CompleteExportRequest("sheet-1", "https://sheets/sheet-1"));

        assertThat(job.getStatus()).isEqualTo(ExportStatus.SUCCESS);
        assertThat(job.getSpreadsheetId()).isEqualTo("sheet-1");
        assertThat(job.getSpreadsheetUrl()).isEqualTo("https://sheets/sheet-1");
        assertThat(job.getErrorMessage()).isNull();
    }

    @Test
    void keepsNonPendingStatusAndUsesDefaultFailureMessage() {
        ExportJob job = job(ExportStatus.SUCCESS);
        when(repository.findById(job.getId())).thenReturn(Optional.of(job));

        service.markProcessing(job.getId());
        assertThat(job.getStatus()).isEqualTo(ExportStatus.SUCCESS);

        service.markFailed(job.getId(), new FailExportRequest(null));
        assertThat(job.getStatus()).isEqualTo(ExportStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("Export failed");
    }

    private ExportJob job(ExportStatus status) {
        ExportJob job = new ExportJob();
        job.setUserId(userId);
        job.setCalendarId(calendarId);
        job.setProvider(ExportProvider.GOOGLE_SHEETS);
        job.setStatus(status);
        return job;
    }
}
