package com.flaver.authservice.dto;

import java.util.List;

public record ErrorResponse(String code, String message, List<String> details) {
}
