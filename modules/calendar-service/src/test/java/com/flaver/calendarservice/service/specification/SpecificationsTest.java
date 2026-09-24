package com.flaver.calendarservice.service.specification;

import com.flaver.calendarservice.entity.Calendar;
import com.flaver.calendarservice.entity.CompetitionLevel;
import com.flaver.calendarservice.entity.Event;
import com.flaver.calendarservice.entity.EventPriority;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpecificationsTest {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void calendarSpecificationBuildsAllRequestedPredicates() {
        Root<Calendar> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Expression<String> lowered = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get(anyString())).thenReturn(path);
        when(builder.lower(any(Expression.class))).thenReturn(lowered);
        when(builder.equal(any(Expression.class), org.mockito.ArgumentMatchers.<Object>any())).thenReturn(predicate);
        when(builder.like(any(Expression.class), anyString(), eq('\\'))).thenReturn(predicate);
        when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);

        Predicate result = CalendarSpecifications.withFilters(
                UUID.randomUUID(), 2026, " Ski ", " 100% Cup ").toPredicate(root, query, builder);

        assertThat(result).isSameAs(predicate);
        verify(builder, org.mockito.Mockito.times(2))
                .like(any(Expression.class), eq("%100\\% cup%"), eq('\\'));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void eventSpecificationBuildsRangeEnumAndSearchPredicates() {
        Root<Event> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        ListJoin join = mock(ListJoin.class);
        Expression<String> lowered = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get(anyString())).thenReturn(path);
        when(root.joinList(anyString(), any())).thenReturn(join);
        when(builder.lower(any(Expression.class))).thenReturn(lowered);
        when(builder.equal(any(Expression.class), org.mockito.ArgumentMatchers.<Object>any())).thenReturn(predicate);
        when(builder.greaterThanOrEqualTo(any(Expression.class), any(LocalDate.class))).thenReturn(predicate);
        when(builder.lessThanOrEqualTo(any(Expression.class), any(LocalDate.class))).thenReturn(predicate);
        when(builder.like(any(Expression.class), anyString(), eq('\\'))).thenReturn(predicate);
        when(builder.or(any(Predicate[].class))).thenReturn(predicate);
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);

        Predicate result = EventSpecifications.withFilters(UUID.randomUUID(), LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), CompetitionLevel.NATIONAL, EventPriority.IMPORTANT, " Cup ")
                .toPredicate(root, query, builder);

        assertThat(result).isSameAs(predicate);
        verify(query).distinct(true);
        verify(builder, org.mockito.Mockito.times(3)).like(any(Expression.class), eq("%cup%"), eq('\\'));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void specificationsSupportOnlyMandatoryPredicates() {
        Root root = mock(Root.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path path = mock(Path.class);
        Predicate predicate = mock(Predicate.class);
        when(root.get(anyString())).thenReturn(path);
        when(builder.equal(any(Expression.class), org.mockito.ArgumentMatchers.<Object>any())).thenReturn(predicate);
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);

        assertThat(CalendarSpecifications.withFilters(UUID.randomUUID(), null, null, null)
                .toPredicate(root, null, builder)).isSameAs(predicate);
        assertThat(EventSpecifications.withFilters(UUID.randomUUID(), null, null, null, null, null)
                .toPredicate(root, null, builder)).isSameAs(predicate);
    }
}
