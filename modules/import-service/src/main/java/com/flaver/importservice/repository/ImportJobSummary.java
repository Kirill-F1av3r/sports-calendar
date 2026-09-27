package com.flaver.importservice.repository;

import com.flaver.importservice.entity.ImportJobStatus;

import java.util.UUID;

public interface ImportJobSummary {
    UUID getJobId();

    UUID getCalendarId();

    String getFileName();

    ImportJobStatus getStatus();

    long getTotalEvents();

    long getValidEvents();

    String getErrorMessage();
}
