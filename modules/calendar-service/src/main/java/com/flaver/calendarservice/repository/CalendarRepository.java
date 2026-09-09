package com.flaver.calendarservice.repository;

import com.flaver.calendarservice.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface CalendarRepository extends JpaRepository<Calendar, UUID>, JpaSpecificationExecutor<Calendar> {
}
