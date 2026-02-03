package com.flaver.calendarservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCalendarRequest(@NotBlank String name, String sportType) {}
