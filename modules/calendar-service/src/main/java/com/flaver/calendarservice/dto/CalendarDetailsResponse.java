package com.flaver.calendarservice.dto;

import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.Event;

import java.util.List;

public record CalendarDetailsResponse(Calendar calendar, List<Event> events) {
}
