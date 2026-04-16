package com.flaver.apigateway.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class ProxyService {

    private final RestClient restClient;

    public ProxyService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public ResponseEntity<byte[]> forward(String targetUrl,
                                          HttpMethod method,
                                          HttpHeaders headers,
                                          byte[] body) {
        HttpHeaders headersToForward = new HttpHeaders();
        headersToForward.addAll(headers);
        headersToForward.remove(HttpHeaders.HOST);
        headersToForward.remove(HttpHeaders.CONTENT_LENGTH);

        RestClient.RequestBodySpec requestSpec = restClient
                .method(method)
                .uri(targetUrl)
                .headers(httpHeaders -> httpHeaders.addAll(headersToForward));

        RestClient.ResponseSpec responseSpec = body != null
                ? requestSpec.body(body).retrieve()
                : requestSpec.retrieve();

        try {
            return responseSpec.toEntity(byte[].class);
        } catch (RestClientResponseException ex) {
            HttpHeaders responseHeaders = new HttpHeaders();
            if (ex.getResponseHeaders() != null) {
                responseHeaders.addAll(ex.getResponseHeaders());
            }

            return ResponseEntity
                    .status(ex.getStatusCode())
                    .headers(responseHeaders)
                    .body(ex.getResponseBodyAsByteArray());
        }
    }
}
