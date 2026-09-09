package com.flaver.apigateway.web;

import com.flaver.apigateway.security.JwtService;
import com.flaver.apigateway.service.ProxyService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class GatewayController {

    private final ProxyService proxyService;
    private final JwtService jwtService;
    private final String authServiceUrl;
    private final String calendarServiceUrl;
    private final String exportServiceUrl;
    private final String integrationServiceUrl;

    public GatewayController(ProxyService proxyService,
                             JwtService jwtService,
                             @Value("${services.auth.url}") String authServiceUrl,
                             @Value("${services.calendar.url}") String calendarServiceUrl,
                             @Value("${services.export.url}") String exportServiceUrl,
                             @Value("${services.integration.url}") String integrationServiceUrl) {
        this.proxyService = proxyService;
        this.jwtService = jwtService;
        this.authServiceUrl = authServiceUrl;
        this.calendarServiceUrl = calendarServiceUrl;
        this.exportServiceUrl = exportServiceUrl;
        this.integrationServiceUrl = integrationServiceUrl;
    }

    @RequestMapping("/auth/**")
    public ResponseEntity<byte[]> proxyAuth(HttpServletRequest request,
                                            @RequestHeader HttpHeaders headers) throws IOException {
        return forwardRequest(request, headers, authServiceUrl, false);
    }

    @RequestMapping("/calendars/**")
    public ResponseEntity<byte[]> proxyCalendar(HttpServletRequest request,
                                                @RequestHeader HttpHeaders headers) throws IOException {
        return forwardRequest(request, headers, calendarServiceUrl, true);
    }

    @RequestMapping("/calendars/metadata")
    public ResponseEntity<byte[]> proxyCalendarMetadata(HttpServletRequest request,
                                                        @RequestHeader HttpHeaders headers) throws IOException {
        return forwardRequest(request, headers, calendarServiceUrl, false);
    }

    @RequestMapping("/exports/**")
    public ResponseEntity<byte[]> proxyExports(HttpServletRequest request,
                                               @RequestHeader HttpHeaders headers) throws IOException {
        return forwardRequest(request, headers, exportServiceUrl, true);
    }

    @RequestMapping("/integrations/google/callback")
    public ResponseEntity<byte[]> proxyGoogleCallback(HttpServletRequest request,
                                                      @RequestHeader HttpHeaders headers) throws IOException {
        return forwardRequest(request, headers, integrationServiceUrl, false);
    }

    @RequestMapping("/integrations/**")
    public ResponseEntity<byte[]> proxyIntegrations(HttpServletRequest request,
                                                    @RequestHeader HttpHeaders headers) throws IOException {
        return forwardRequest(request, headers, integrationServiceUrl, true);
    }

    private ResponseEntity<byte[]> forwardRequest(HttpServletRequest request,
                                                  HttpHeaders headers,
                                                  String serviceUrl,
                                                  boolean requiresJwt) throws IOException {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        if (method == HttpMethod.OPTIONS) {
            return ResponseEntity.noContent().build();
        }

        String pathAndQuery = request.getRequestURI() +
                (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        HttpHeaders updatedHeaders = new HttpHeaders();
        updatedHeaders.addAll(headers);
        updatedHeaders.remove("X-User-Id");

        if (requiresJwt) {
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Missing or invalid Authorization header".getBytes());
            }

            String token = authHeader.substring(7);
            try {
                String userId = jwtService.extractUserId(token);
                updatedHeaders.set("X-User-Id", userId);
            } catch (JwtException ex) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Invalid or expired token".getBytes());
            }
        }

        byte[] body = request.getInputStream().readAllBytes();
        if (body.length == 0) {
            body = null;
        }

        return proxyService.forward(serviceUrl + pathAndQuery, method, updatedHeaders, body);
    }
}
