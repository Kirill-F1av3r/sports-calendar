package com.flaver.exportservice.repository;

import com.flaver.exportservice.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExportJobRepository extends JpaRepository<ExportJob, UUID> {
    Optional<ExportJob> findByIdAndUserId(UUID id, UUID userId);
}
