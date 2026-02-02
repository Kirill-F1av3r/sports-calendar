package com.flaver.calendarservice.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEventRequest(@NotBlank String title,
                                 @NotBlank String startDate,
                                 @NotBlank String endDate,
                                 @NotBlank String location,
                                 String source) {}
