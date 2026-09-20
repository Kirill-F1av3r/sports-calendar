package com.flaver.importservice.service;

import com.flaver.dto.importing.ImportRequestedEvent;
import com.flaver.importservice.config.ImportFilesProperties;
import com.flaver.importservice.dto.DraftEventResponse;
import com.flaver.importservice.dto.UpdateDraftEventRequest;
import com.flaver.importservice.entity.ImportDraftEvent;
import com.flaver.importservice.entity.ImportJob;
import com.flaver.importservice.entity.ImportJobStatus;
import com.flaver.importservice.repository.ImportDraftEventRepository;
import com.flaver.importservice.repository.ImportJobRepository;
import com.flaver.importservice.storage.ObjectStorage;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportJobServiceTest {

    @Test
    void updateDraftEventReplacesFieldsAndAllowsOptionalValuesToBeCleared() {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID draftEventId = UUID.randomUUID();

        ImportJob job = new ImportJob();
        job.setId(jobId);
        job.setUserId(userId);
        job.setStatus(ImportJobStatus.READY);

        ImportDraftEvent draftEvent = new ImportDraftEvent();
        draftEvent.setId(draftEventId);
        draftEvent.setJob(job);
        draftEvent.setTitle("Старое название");
        draftEvent.setStartDate(LocalDate.of(2026, 5, 1));
        draftEvent.setEndDate(LocalDate.of(2026, 5, 2));
        draftEvent.setCompetitionLevel("REGIONAL");
        draftEvent.setLocation("Старое место");
        draftEvent.setExternalUrl("https://example.com/old");
        draftEvent.setDisciplines(List.of("Старая дисциплина"));
        draftEvent.setPriority("IMPORTANT");

        ImportJobRepository jobRepository = mock(ImportJobRepository.class);
        ImportDraftEventRepository draftEventRepository = mock(ImportDraftEventRepository.class);
        when(jobRepository.findByIdAndUserId(jobId, userId)).thenReturn(Optional.of(job));
        when(draftEventRepository.findByIdAndJobId(draftEventId, jobId)).thenReturn(Optional.of(draftEvent));
        when(draftEventRepository.save(any(ImportDraftEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ImportJobService service = new ImportJobService(
                jobRepository,
                draftEventRepository,
                mock(CalendarClient.class),
                mock(ObjectStorage.class),
                mockKafkaTemplate(),
                mock(ImportFilesProperties.class),
                new DraftEventValidator()
        );

        UpdateDraftEventRequest request = new UpdateDraftEventRequest(
                "Новое название",
                LocalDate.of(2026, 6, 10),
                null,
                null,
                "Новое место",
                null,
                List.of("Новая дисциплина"),
                null
        );

        DraftEventResponse response = service.updateDraftEvent(userId, jobId, draftEventId, request);

        assertThat(response.title()).isEqualTo("Новое название");
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.endDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.competitionLevel()).isNull();
        assertThat(response.location()).isEqualTo("Новое место");
        assertThat(response.externalUrl()).isNull();
        assertThat(response.disciplines()).containsExactly("Новая дисциплина");
        assertThat(response.priority()).isNull();
        assertThat(response.valid()).isTrue();
        assertThat(response.errors()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, ImportRequestedEvent> mockKafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
}
