package com.flaver.apigateway.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyServiceTest {

    @Test
    void shouldReturnDownstreamErrorResponseInsteadOfThrowing() {
        RestClient.Builder builder = mock(RestClient.Builder.class);
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.build()).thenReturn(restClient);
        when(restClient.method(HttpMethod.POST)).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(URI.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.headers(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(byte[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        HttpHeaders downstreamHeaders = new HttpHeaders();
        downstreamHeaders.setContentType(MediaType.APPLICATION_JSON);
        byte[] downstreamBody = "{\"message\":\"User already exists\"}".getBytes();

        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.CONFLICT,
                "Conflict",
                downstreamHeaders,
                downstreamBody,
                null
        );

        when(responseSpec.toEntity(byte[].class)).thenThrow(exception);

        ProxyService proxyService = new ProxyService(builder);

        ResponseEntity<byte[]> response = proxyService.forward(
                "http://auth-service/auth/register",
                HttpMethod.POST,
                new HttpHeaders(),
                "{}".getBytes()
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertArrayEquals(downstreamBody, response.getBody());
    }
}
