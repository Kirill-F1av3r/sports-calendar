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

    public GatewayController(ProxyService proxyService,
                             JwtService jwtService,
                             @Value("${services.auth.url}") String authServiceUrl,
                             @Value("${services.calendar.url}") String calendarServiceUrl) {
        this.proxyService = proxyService;
        this.jwtService = jwtService;
        this.authServiceUrl = authServiceUrl;
        this.calendarServiceUrl = calendarServiceUrl;
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

    private ResponseEntity<byte[]> forwardRequest(HttpServletRequest request,
                                                  HttpHeaders headers,
                                                  String serviceUrl,
                                                  boolean requiresJwt) throws IOException {
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String pathAndQuery = request.getRequestURI() +
                (request.getQueryString() != null ? "?" + request.getQueryString() : "");

        HttpHeaders updatedHeaders = new HttpHeaders();
        updatedHeaders.addAll(headers);

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
