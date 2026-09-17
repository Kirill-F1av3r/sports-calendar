package com.flaver.importservice.repository;

import com.flaver.importservice.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
    Optional<ImportJob> findByIdAndUserId(UUID id, UUID userId);
}
