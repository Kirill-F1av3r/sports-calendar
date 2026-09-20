package com.flaver.importservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "import_draft_event_errors")
@Getter
@Setter
public class ImportDraftEventError {
    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_event_id", nullable = false)
    private ImportDraftEvent draftEvent;

    @Column(name = "field_name")
    private String fieldName;

    @Column(nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
