package com.flaver.calendarservice.service.specification;

import com.flaver.calendarservice.entity.Calendar;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import static com.flaver.calendarservice.service.specification.SpecificationUtils.likePattern;
import static com.flaver.calendarservice.service.util.TextUtils.trimToNull;

public final class CalendarSpecifications {
    private CalendarSpecifications() {
    }

    public static Specification<Calendar> withFilters(UUID ownerId, Integer year, String sportType, String search) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerId"), ownerId));

            if (year != null) {
                predicates.add(cb.equal(root.get("year"), year));
            }

            String normalizedSportType = trimToNull(sportType);
            if (normalizedSportType != null) {
                predicates.add(cb.equal(cb.lower(root.get("sportType")), normalizedSportType.toLowerCase(Locale.ROOT)));
            }

            String normalizedSearch = trimToNull(search);
            if (normalizedSearch != null) {
                String pattern = likePattern(normalizedSearch);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, '\\'),
                        cb.like(cb.lower(root.get("sportType")), pattern, '\\')
                ));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
