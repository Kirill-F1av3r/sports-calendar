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

    private Instant createdAt = Instant.now();

}
