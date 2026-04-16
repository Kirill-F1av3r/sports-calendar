package com.flaver.calendarservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "events",
        uniqueConstraints = @UniqueConstraint(columnNames = {"title"}))
@Getter
@Setter
public class Event {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(name="calendar_id")
    private UUID calendarId;

    private String title;

    @Column(name="start_ts")
    private LocalDate startDate;

    @Column(name="end_ts")
    private LocalDate endDate;

    private String location;

    private String source;

    private Instant createdAt = Instant.now();
}
