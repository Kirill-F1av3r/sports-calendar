package com.flaver.calendarservice.service;

import com.flaver.calendarservice.dto.EnumOptionResponse;
import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.EventPriority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class CalendarMetadataService {
    @Transactional(readOnly = true)
    public List<EnumOptionResponse> competitionLevelOptions() {
        return Arrays.stream(CompetitionLevel.values())
                .map(level -> new EnumOptionResponse(level.name(), level.getDisplayNameRu()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnumOptionResponse> priorityOptions() {
        return Arrays.stream(EventPriority.values())
                .map(priority -> new EnumOptionResponse(priority.name(), priority.getDisplayNameRu()))
                .toList();
    }
}
