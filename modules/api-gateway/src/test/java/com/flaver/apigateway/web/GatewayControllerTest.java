package com.flaver.apigateway.web;

import com.flaver.apigateway.config.SecurityConfig;
import com.flaver.apigateway.security.JwtService;
import com.flaver.apigateway.service.ProxyService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GatewayController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "services.auth.url=http://localhost:8081",
        "services.calendar.url=http://localhost:8082",
        "services.export.url=http://localhost:8083",
        "services.integration.url=http://localhost:8085",
        "security.jwt.secret=0123456789ABCDEF0123456789ABCDEF",
        "security.jwt.accessTokenSeconds=900"
})
class GatewayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProxyService proxyService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void shouldReturn401WhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/calendars"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Missing or invalid Authorization header"));
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid() throws Exception {
        given(jwtService.extractUserId("bad-token")).willThrow(new JwtException("bad token"));

        mockMvc.perform(get("/calendars").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or expired token"));
    }

    @Test
    void shouldProxyCalendarsRequestWithExtractedUserId() throws Exception {
        given(jwtService.extractUserId("good-token")).willReturn("user-123");
        given(proxyService.forward(anyString(), any(), any(), any()))
                .willAnswer(invocation -> {
                    HttpHeaders headers = invocation.getArgument(2);
                    String xUserId = headers.getFirst("X-User-Id");
                    return ResponseEntity.ok(("proxied-for-" + xUserId).getBytes());
                });

        mockMvc.perform(get("/calendars").header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("proxied-for-user-123"));
    }
    @Test
    void shouldReturnDownstreamStatusAndBody() throws Exception {
        given(jwtService.extractUserId("good-token")).willReturn("user-123");
        given(proxyService.forward(anyString(), any(), any(), any()))
                .willReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body("calendar not found".getBytes()));

        mockMvc.perform(get("/calendars/").header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("calendar not found"));
    }

}
