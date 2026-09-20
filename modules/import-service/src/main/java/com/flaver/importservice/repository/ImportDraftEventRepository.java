package com.flaver.importservice.repository;

import com.flaver.importservice.entity.ImportDraftEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportDraftEventRepository extends JpaRepository<ImportDraftEvent, UUID> {
    List<ImportDraftEvent> findByJobIdOrderByCreatedAtAsc(UUID jobId);

    Optional<ImportDraftEvent> findByIdAndJobId(UUID id, UUID jobId);
}
