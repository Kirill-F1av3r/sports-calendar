package com.flaver.importservice.repository;

import com.flaver.importservice.entity.ImportDraftEvent;
import com.flaver.importservice.entity.ImportDraftEventError;
import com.flaver.importservice.entity.ImportJob;
import com.flaver.importservice.entity.ImportJobStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class ImportRepositoryFetchStrategyTest {
    @Autowired
    private ImportJobRepository jobRepository;

    @Autowired
    private ImportDraftEventRepository draftEventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;
    private UUID jobId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        userId = UUID.randomUUID();

        ImportJob job = new ImportJob();
        job.setUserId(userId);
        job.setCalendarId(UUID.randomUUID());
        job.setStatus(ImportJobStatus.READY);
        job.setSourceFileName("events.csv");
        job.setSourceFileSize(10);
        job.setSourceObjectKey("imports/events.csv");

        for (int index = 0; index < 3; index++) {
            ImportDraftEvent draft = new ImportDraftEvent();
            draft.setJob(job);
            draft.setTitle("Draft " + index);
            draft.setValid(index < 2);
            draft.setDisciplines(List.of("Discipline " + index));

            ImportDraftEventError error = new ImportDraftEventError();
            error.setDraftEvent(draft);
            error.setFieldName("title");
            error.setMessage("Error " + index);
            draft.getErrors().add(error);
            job.getDraftEvents().add(draft);
        }

        entityManager.persist(job);
        entityManager.flush();
        jobId = job.getId();
        entityManager.clear();
        statistics.clear();
    }

    @Test
    void draftCollectionsLoadInTwoBatchesInsteadOfTwoNQueries() {
        List<ImportDraftEvent> drafts = draftEventRepository.findByJobIdOrderByCreatedAtAsc(jobId);

        assertThat(drafts).hasSize(3).allSatisfy(draft -> {
            assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(draft, "disciplines")).isFalse();
            assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(draft, "errors")).isFalse();
        });

        drafts.forEach(draft -> {
            assertThat(draft.getDisciplines()).hasSize(1);
            assertThat(draft.getErrors()).hasSize(1);
        });

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void jobSummaryUsesOneAggregateQuery() {
        ImportJobSummary summary = jobRepository.findSummaryByIdAndUserId(jobId, userId).orElseThrow();

        assertThat(summary.getTotalEvents()).isEqualTo(3);
        assertThat(summary.getValidEvents()).isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    void applyQueryLoadsOnlyValidDraftsAndBatchesDisciplines() {
        List<ImportDraftEvent> drafts =
                draftEventRepository.findByJobIdAndValidTrueOrderByCreatedAtAsc(jobId);
        drafts.forEach(draft -> assertThat(draft.getDisciplines()).hasSize(1));

        assertThat(drafts).hasSize(2).allMatch(ImportDraftEvent::isValid);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }
}
