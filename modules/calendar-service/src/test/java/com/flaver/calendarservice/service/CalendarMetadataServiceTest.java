package com.flaver.calendarservice.service;

import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.EventPriority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarMetadataServiceTest {
    private final CalendarMetadataService service = new CalendarMetadataService();

    @Test
    void exposesEveryCompetitionLevelAndPriority() {
        assertThat(service.competitionLevelOptions())
                .hasSize(CompetitionLevel.values().length)
                .extracting("code")
                .containsExactly((Object[]) java.util.Arrays.stream(CompetitionLevel.values()).map(Enum::name).toArray(String[]::new));
        assertThat(service.priorityOptions())
                .hasSize(EventPriority.values().length)
                .extracting("code")
                .containsExactly((Object[]) java.util.Arrays.stream(EventPriority.values()).map(Enum::name).toArray(String[]::new));
    }
}
