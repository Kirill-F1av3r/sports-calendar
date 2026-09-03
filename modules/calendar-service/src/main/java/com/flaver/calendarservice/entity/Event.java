package com.flaver.calendarservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
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

    @Column(name="start_ts")
    private LocalDateTime startDateTime;

    @Column(name="end_ts")
    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private String timezone;

    private String location;

    private String distance;

    @Enumerated(EnumType.STRING)
    private EventPriority priority;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    private String source;

    private String externalUrl;

    @Column(length = 2000)
    private String notes;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
