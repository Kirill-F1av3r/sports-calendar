package com.flaver.importservice.dto;

import java.util.List;

public record ErrorResponse(String code, String message, List<String> details) {
}
