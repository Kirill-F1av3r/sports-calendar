package com.flaver.calendarservice.service.sort;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortParserTest {
    private final SortParser parser = new SortParser();
    private final Map<String, String> fields = Map.of("date", "startDate", "title", "title");

    @Test
    void usesDefaultAndMapsPublicField() {
        Sort.Order order = parser.parse(" ", "date,desc", fields).getOrderFor("startDate");

        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void defaultsDirectionToAscending() {
        assertThat(parser.parse("title", "date,desc", fields).getOrderFor("title").isAscending()).isTrue();
    }

    @Test
    void rejectsUnsupportedFieldAndDirection() {
        assertThatThrownBy(() -> parser.parse("unknown", "date", fields))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported sort field");
        assertThatThrownBy(() -> parser.parse("date,sideways", "date", fields))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unsupported sort direction");
    }
}
