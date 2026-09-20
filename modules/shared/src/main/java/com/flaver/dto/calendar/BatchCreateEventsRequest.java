package com.flaver.dto.calendar;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchCreateEventsRequest(
        @NotEmpty(message = "Список событий не должен быть пустым")
        @Size(max = 1000, message = "За один запрос можно создать не больше 1000 событий")
        List<@Valid BatchCreateEventRequest> events
) {
}
