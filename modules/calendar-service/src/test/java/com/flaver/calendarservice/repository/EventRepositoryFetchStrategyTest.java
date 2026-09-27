package com.flaver.calendarservice.repository;

import com.flaver.calendarservice.entity.Event;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class EventRepositoryFetchStrategyTest {
    @Autowired
    private EventRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;
    private UUID calendarId;

    @BeforeEach
    void setUp() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        calendarId = UUID.randomUUID();
        for (int index = 0; index < 3; index++) {
            Event event = new Event();
            event.setCalendarId(calendarId);
            event.setTitle("Event " + index);
            event.setStartDate(LocalDate.of(2026, 1, index + 1));
            event.setEndDate(event.getStartDate());
            event.setDisciplines(List.of("Discipline " + index));
            entityManager.persist(event);
        }
        entityManager.flush();
        entityManager.clear();
        statistics.clear();
    }

    @Test
    void pageableEventsLoadLazyDisciplinesInOneBatch() {
        Page<Event> page = repository.findAll(
                (root, query, builder) -> builder.equal(root.get("calendarId"), calendarId),
                PageRequest.of(0, 2)
        );

        assertThat(page.getContent()).allSatisfy(event ->
                assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(event, "disciplines")).isFalse());

        page.getContent().forEach(event -> assertThat(event.getDisciplines()).hasSize(1));

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void exportQueryLoadsDisciplinesInOneBatch() {
        List<Event> events = repository.findByCalendarIdOrderByStartDateAsc(calendarId);

        assertThat(events).hasSize(3).allSatisfy(event ->
                assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(event, "disciplines")).isFalse());
        events.forEach(event -> assertThat(event.getDisciplines()).hasSize(1));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void nonPageableSpecificationLoadsDisciplinesInOneBatch() {
        List<Event> events = repository.findAll(
                (root, query, builder) -> builder.equal(root.get("calendarId"), calendarId)
        );

        assertThat(events).hasSize(3).allSatisfy(event ->
                assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(event, "disciplines")).isFalse());
        events.forEach(event -> assertThat(event.getDisciplines()).hasSize(1));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void singleEventKeepsDisciplinesLazy() {
        UUID eventId = repository.findAll().getFirst().getId();
        entityManager.clear();
        statistics.clear();

        Event event = repository.findById(eventId).orElseThrow();

        assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(event, "disciplines")).isFalse();
        assertThat(event.getDisciplines()).hasSize(1);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }
}
