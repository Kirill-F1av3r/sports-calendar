package com.flaver.calendarservice.service.specification;

import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.entity.EventPriority;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static com.flaver.calendarservice.service.specification.SpecificationUtils.likePattern;
import static com.flaver.calendarservice.service.util.TextUtils.trimToNull;

public final class EventSpecifications {
    private EventSpecifications() {
    }

    public static Specification<Event> withFilters(UUID calendarId,
                                                   LocalDate from,
                                                   LocalDate to,
                                                   CompetitionLevel competitionLevel,
                                                   EventPriority priority,
                                                   String search) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("calendarId"), calendarId));

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), to));
            }
            if (competitionLevel != null) {
                predicates.add(cb.equal(root.get("competitionLevel"), competitionLevel));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            String normalizedSearch = trimToNull(search);
            if (normalizedSearch != null) {
                if (query != null) {
                    query.distinct(true);
                }
                ListJoin<Event, String> disciplineJoin = root.joinList("disciplines", JoinType.LEFT);
                String pattern = likePattern(normalizedSearch);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("location")), pattern, '\\'),
                        cb.like(cb.lower(disciplineJoin), pattern, '\\')
                ));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
