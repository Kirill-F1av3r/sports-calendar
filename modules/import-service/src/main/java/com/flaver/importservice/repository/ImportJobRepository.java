package com.flaver.importservice.repository;

import com.flaver.importservice.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {
    Optional<ImportJob> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            select job.id as jobId,
                   job.calendarId as calendarId,
                   job.sourceFileName as fileName,
                   job.status as status,
                   count(draft) as totalEvents,
                   coalesce(sum(case when draft.valid = true then 1L else 0L end), 0L) as validEvents,
                   job.errorMessage as errorMessage
            from ImportJob job
            left join job.draftEvents draft
            where job.id = :jobId and job.userId = :userId
            group by job.id, job.calendarId, job.sourceFileName, job.status, job.errorMessage
            """)
    Optional<ImportJobSummary> findSummaryByIdAndUserId(@Param("jobId") UUID jobId,
                                                        @Param("userId") UUID userId);
}
