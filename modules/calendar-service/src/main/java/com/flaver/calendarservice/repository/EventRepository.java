package com.flaver.calendarservice.repository;

import com.flaver.calendarservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByCalendarIdOrderByStartDateTimeAsc(UUID calendarId);
}
