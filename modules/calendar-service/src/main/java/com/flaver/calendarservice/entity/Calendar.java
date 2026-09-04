package com.flaver.calendarservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendars")
@Getter
@Setter
public class Calendar {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID ownerId;

    private String name;

    private String sportType;

    @Column(name = "season_year")
    private Integer year;

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
