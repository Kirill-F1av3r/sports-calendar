package com.flaver.calendarservice.service.specification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpecificationUtilsTest {
    @Test
    void buildsCaseInsensitiveEscapedLikePattern() {
        assertThat(SpecificationUtils.likePattern("Cup_100%\\Final"))
                .isEqualTo("%cup\\_100\\%\\\\final%");
    }
}
