package com.flaver.exportservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_jobs")
@Getter
@Setter
public class ExportJob {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID calendarId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExportProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExportStatus status;

    private String spreadsheetId;

    private String spreadsheetUrl;

    @Column(length = 2000)
    private String errorMessage;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
