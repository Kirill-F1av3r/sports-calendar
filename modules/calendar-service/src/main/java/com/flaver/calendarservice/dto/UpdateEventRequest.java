package com.flaver.calendarservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record UpdateEventRequest(
        @Size(min = 1, max = 255, message = "Название события должно быть от 1 до 255 символов")
        String title,

        LocalDate startDate,

        LocalDate endDate,

        @Pattern(
                regexp = "INTERNATIONAL|NATIONAL|REGIONAL|LOCAL|TRAINING|OTHER",
                message = "Уровень соревнования должен быть INTERNATIONAL, NATIONAL, REGIONAL, LOCAL, TRAINING или OTHER"
        )
        String competitionLevel,

        @Size(max = 255, message = "Место проведения должно быть не длиннее 255 символов")
        String location,

        @Size(max = 500, message = "Ссылка должна быть не длиннее 500 символов")
        String externalUrl,

        @Size(max = 30, message = "Дисциплин должно быть не больше 30")
        List<@NotBlank(message = "Дисциплина не должна быть пустой")
                @Size(max = 100, message = "Дисциплина должна быть не длиннее 100 символов") String> disciplines,

        @Pattern(
                regexp = "REQUIRED|IMPORTANT|OPTIONAL",
                message = "Приоритет должен быть REQUIRED, IMPORTANT или OPTIONAL"
        )
        String priority
) {
}
