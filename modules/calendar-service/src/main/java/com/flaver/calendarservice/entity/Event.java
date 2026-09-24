package com.flaver.calendarservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
public class Event {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name="calendar_id")
    private UUID calendarId;

    @Column(nullable = false)
    private String title;

    @Column(name="start_date", nullable = false)
    private LocalDate startDate;

    @Column(name="end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "competition_level")
    private CompetitionLevel competitionLevel;

    private String location;

    @Column(length = 500)
    private String externalUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "event_disciplines",
            joinColumns = @JoinColumn(name = "event_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "discipline", nullable = false)
    private List<String> disciplines = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EventPriority priority;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
