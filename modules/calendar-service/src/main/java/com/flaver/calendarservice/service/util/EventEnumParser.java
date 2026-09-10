package com.flaver.calendarservice.service.util;

import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.EventPriority;

public final class EventEnumParser {
    private EventEnumParser() {
    }

    public static EventPriority parsePriorityOrDefault(String value) {
        String normalized = TextUtils.trimToNull(value);
        if (normalized == null) {
            return EventPriority.OPTIONAL;
        }
        return parseOptionalPriority(normalized);
    }

    public static EventPriority parseOptionalPriority(String value) {
        String normalized = TextUtils.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return EventPriority.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("priority must be REQUIRED, IMPORTANT or OPTIONAL");
        }
    }

    public static CompetitionLevel parseCompetitionLevelOrDefault(String value) {
        String normalized = TextUtils.trimToNull(value);
        if (normalized == null) {
            return CompetitionLevel.OTHER;
        }
        return parseOptionalCompetitionLevel(normalized);
    }

    public static CompetitionLevel parseOptionalCompetitionLevel(String value) {
        String normalized = TextUtils.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return CompetitionLevel.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("unsupported competition level");
        }
    }
}
