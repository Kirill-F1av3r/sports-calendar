package com.flaver.calendarservice.service.specification;

import java.util.Locale;

public final class SpecificationUtils {
    private SpecificationUtils() {
    }

    public static String likePattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
    }
}
