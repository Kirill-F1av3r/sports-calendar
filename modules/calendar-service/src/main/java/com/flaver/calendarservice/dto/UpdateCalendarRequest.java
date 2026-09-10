package com.flaver.calendarservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateCalendarRequest(
        @Size(min = 1, max = 255, message = "Название календаря должно быть от 1 до 255 символов")
        String name,

        @Size(max = 100, message = "Вид спорта должен быть не длиннее 100 символов")
        String sportType,

        @Min(value = 2000, message = "Год должен быть не меньше 2000")
        @Max(value = 2100, message = "Год должен быть не больше 2100")
        Integer year
) {
}
