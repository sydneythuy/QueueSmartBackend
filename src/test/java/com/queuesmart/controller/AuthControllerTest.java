package com.queuesmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuesmart.config.JwtUtil;
import com.queuesmart.dto.AuthDto;
import com.queuesmart.repository.UserRepository;
import com.queuesmart.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserRepository userRepository;

    // ---- REGISTER ----

    @Test
    void register_ValidRequest_Returns201() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        AuthDto.AuthResponse mockResponse = new AuthDto.AuthResponse(
                "jwt.token", "user-1", "alice", "alice@example.com", "USER");

        when(authService.register(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.token").value("jwt.token"));
    }

    @Test
    void register_BlankEmail_Returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("alice");
        request.setEmail("");           // invalid
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_InvalidEmail_Returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("alice");
        request.setEmail("not-an-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShortPassword_Returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("123");    // too short (min 6)

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShortUsername_Returns400() throws Exception {
        AuthDto.RegisterRequest request = new AuthDto.RegisterRequest();
        request.setUsername("ab");     // too short (min 3)
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ---- LOGIN ----

    @Test
    void login_ValidCredentials_Returns200() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        AuthDto.AuthResponse mockResponse = new AuthDto.AuthResponse(
                "jwt.token", "user-1", "alice", "alice@example.com", "USER");

        when(authService.login(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt.token"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void login_BlankEmail_Returns400() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_InvalidEmailFormat_Returns400() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("not-an-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_BadCredentials_Returns400() throws Exception {
        AuthDto.LoginRequest request = new AuthDto.LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("wrongpassword");

        when(authService.login(any()))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
