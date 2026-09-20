package com.flaver.importservice.dto;

import jakarta.validation.constraints.NotBlank;

public record FailImportRequest(
        @NotBlank(message = "Сообщение об ошибке обязательно")
        String errorMessage
) {
}
