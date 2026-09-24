package com.flaver.calendarservice.service.sort;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.flaver.calendarservice.service.util.TextUtils.trimToNull;

@Component
public class SortParser {
    public Sort parse(String requestedSort, String defaultSort, Map<String, String> allowedFields) {
        String sortValue = trimToNull(requestedSort);
        if (sortValue == null) {
            sortValue = defaultSort;
        }

        String[] parts = sortValue.split(",");
        String requestedField = parts[0].trim();
        String field = allowedFields.get(requestedField);
        if (field == null) {
            throw new IllegalArgumentException("unsupported sort field");
        }

        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("unsupported sort direction");
            }
        }

        return Sort.by(direction, field);
    }
}
