package com.flaver.exportservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsBadRequest() {
        var response = handler.handleBadRequest(new IllegalArgumentException("bad provider"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "bad provider");
    }

    @Test
    void mapsCalendarClientErrorsWithoutLeakingRemoteBody() {
        HttpClientErrorException notFound = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not found", HttpHeaders.EMPTY, "secret".getBytes(), StandardCharsets.UTF_8);
        HttpClientErrorException forbidden = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);

        assertThat(handler.handleCalendarNotFound((HttpClientErrorException.NotFound) notFound).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(handler.handleCalendarForbidden((HttpClientErrorException.Forbidden) forbidden).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void mapsCalendarServerFailureToServiceUnavailable() {
        var exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        var response = handler.handleCalendarUnavailable(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "Calendar service is unavailable");
    }
}
