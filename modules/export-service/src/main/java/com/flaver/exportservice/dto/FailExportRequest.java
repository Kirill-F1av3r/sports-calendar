package com.flaver.exportservice.dto;

import jakarta.validation.constraints.Size;

public record FailExportRequest(
        @Size(max = 1000, message = "Сообщение об ошибке должно быть не длиннее 1000 символов")
        String errorMessage
) {
}
