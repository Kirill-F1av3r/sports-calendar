package com.flaver.importservice.entity;

import com.flaver.importservice.service.DraftEventValidator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ImportDraftEventPersistenceTest {
    @Autowired
    private EntityManager entityManager;

    @Test
    void managedDraftEventCanBeCorrectedAndRevalidated() {
        ImportJob job = new ImportJob();
        job.setUserId(UUID.randomUUID());
        job.setCalendarId(UUID.randomUUID());
        job.setStatus(ImportJobStatus.READY);
        job.setSourceFileName("events.csv");
        job.setSourceFileSize(1);
        job.setSourceObjectKey("imports/events.csv");

        ImportDraftEvent event = new ImportDraftEvent();
        event.setJob(job);
        event.setTitle(null);
        event.setDisciplines(List.of("Running"));
        new DraftEventValidator().validate(event);
        job.getDraftEvents().add(event);

        entityManager.persist(job);
        entityManager.flush();
        entityManager.clear();

        ImportDraftEvent managedEvent = entityManager.find(ImportDraftEvent.class, event.getId());
        managedEvent.setTitle("Triathlon");
        managedEvent.setStartDate(LocalDate.of(2026, 9, 20));
        managedEvent.setDisciplines(List.of("Swimming"));
        new DraftEventValidator().validate(managedEvent);
        entityManager.merge(managedEvent);
        entityManager.flush();
        entityManager.clear();

        ImportDraftEvent updatedEvent = entityManager.find(ImportDraftEvent.class, event.getId());
        assertThat(updatedEvent.getDisciplines()).containsExactly("Swimming");
        assertThat(updatedEvent.getErrors()).isEmpty();
        assertThat(updatedEvent.isValid()).isTrue();
    }
}
