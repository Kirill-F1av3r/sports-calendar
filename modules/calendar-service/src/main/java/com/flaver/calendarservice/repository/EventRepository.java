package com.flaver.calendarservice.repository;

import com.flaver.calendarservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {
    List<Event> findByCalendarIdOrderByStartDateAsc(UUID calendarId);
}
