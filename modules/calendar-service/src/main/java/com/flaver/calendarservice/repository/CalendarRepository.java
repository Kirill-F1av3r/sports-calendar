package com.flaver.calendarservice.repository;

import com.flaver.calendarservice.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CalendarRepository extends JpaRepository<Calendar, UUID> {
    List<Calendar> findByOwnerId(UUID ownerId);
}
