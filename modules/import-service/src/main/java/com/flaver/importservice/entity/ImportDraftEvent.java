package com.flaver.importservice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "import_draft_events")
@Getter
@Setter
public class ImportDraftEvent {
    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJob job;

    private String title;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "competition_level")
    private String competitionLevel;

    private String location;

    @Column(name = "external_url", length = 500)
    private String externalUrl;

    private String priority;

    @Column(nullable = false)
    private boolean valid;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "raw_text")
    private String rawText;

    @Column(name = "created_event_id")
    private UUID createdEventId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "import_draft_event_disciplines",
            joinColumns = @JoinColumn(name = "draft_event_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "discipline", nullable = false)
    @Setter(AccessLevel.NONE)
    private List<String> disciplines = new ArrayList<>();

    @OneToMany(mappedBy = "draftEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ImportDraftEventError> errors = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
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

    public void setDisciplines(List<String> disciplines) {
        List<String> values = disciplines == null ? List.of() : new ArrayList<>(disciplines);
        this.disciplines.clear();
        this.disciplines.addAll(values);
    }
}
