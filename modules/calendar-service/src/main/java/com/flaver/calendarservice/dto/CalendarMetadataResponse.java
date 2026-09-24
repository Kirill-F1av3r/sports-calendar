package com.flaver.calendarservice.dto;

import java.util.List;

public record CalendarMetadataResponse(
        List<EnumOptionResponse> competitionLevels,
        List<EnumOptionResponse> priorities
) {
}
