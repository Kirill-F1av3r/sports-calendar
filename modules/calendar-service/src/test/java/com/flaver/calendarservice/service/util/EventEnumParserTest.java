package com.flaver.calendarservice.service.util;

import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.EventPriority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventEnumParserTest {
    @Test
    void parsesValuesIgnoringCaseAndWhitespace() {
        assertThat(EventEnumParser.parsePriorityOrDefault(" required ")).isEqualTo(EventPriority.REQUIRED);
        assertThat(EventEnumParser.parseOptionalCompetitionLevel(" national ")).isEqualTo(CompetitionLevel.NATIONAL);
    }

    @Test
    void appliesDefaultsAndSupportsNullOptionals() {
        assertThat(EventEnumParser.parsePriorityOrDefault(" ")).isEqualTo(EventPriority.OPTIONAL);
        assertThat(EventEnumParser.parseCompetitionLevelOrDefault(null)).isEqualTo(CompetitionLevel.OTHER);
        assertThat(EventEnumParser.parseOptionalPriority(null)).isNull();
        assertThat(EventEnumParser.parseOptionalCompetitionLevel(" ")).isNull();
    }

    @Test
    void rejectsUnknownValues() {
        assertThatThrownBy(() -> EventEnumParser.parseOptionalPriority("urgent"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EventEnumParser.parseOptionalCompetitionLevel("world"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
