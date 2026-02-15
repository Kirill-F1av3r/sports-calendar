package com.flaver.authservice.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flaver.authservice.entity.User;
import com.flaver.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthControllerTest {
    private AuthService authService;
    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void register_returnsId() throws Exception {
        when(authService.register(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> {
                    User u = new User();
                    u.setId(UUID.randomUUID());
                    u.setEmail(inv.getArgument(0));
                    return u;
                });

        var payload = Map.of("email", "a@b.com", "password", "pw", "fullName", "X");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void login_returnsAuthResponse() throws Exception {
        when(authService.login("a@b.com", "pw"))
                .thenReturn("token-value");

        var payload = Map.of("email", "a@b.com", "password", "pw");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-value"));
    }
}
