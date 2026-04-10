package com.queuesmart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queuesmart.dto.AuthDto;
import com.queuesmart.service.AuthService;
import com.queuesmart.config.JwtUtil;
import com.queuesmart.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class AuthControllerTest {

    @Autowired private MockMvc       mockMvc;
    @Autowired private ObjectMapper  objectMapper;

    @MockBean private AuthService              authService;
    @MockBean private JwtUtil                  jwtUtil;
    @MockBean private UserCredentialRepository credentialRepository;

    // ── REGISTER ──────────────────────────────────────────────

    @Test
    void register_ValidRequest_Returns201WithToken() throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com"); req.setPassword("password123");

        when(authService.register(any()))
                .thenReturn(new AuthDto.AuthResponse("jwt.tok", "u1", "alice", "alice@example.com", "USER"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt.tok"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void register_BlankUsername_Returns400() throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setUsername("ab"); // too short — min 3
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void register_InvalidEmail_Returns400() throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setUsername("alice"); req.setEmail("not-an-email"); req.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShortPassword_Returns400() throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com"); req.setPassword("123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_DuplicateEmail_Returns400WithMessage() throws Exception {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setUsername("alice"); req.setEmail("alice@example.com"); req.setPassword("password123");

        when(authService.register(any()))
                .thenThrow(new IllegalArgumentException("Email is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    // ── LOGIN ─────────────────────────────────────────────────

    @Test
    void login_ValidCredentials_Returns200() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("alice@example.com"); req.setPassword("password123");

        when(authService.login(any()))
                .thenReturn(new AuthDto.AuthResponse("jwt.tok", "u1", "alice", "alice@example.com", "USER"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void login_BlankEmail_Returns400() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail(""); req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WrongPassword_Returns400() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("alice@example.com"); req.setPassword("wrongpass");

        when(authService.login(any()))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_InvalidEmailFormat_Returns400() throws Exception {
        AuthDto.LoginRequest req = new AuthDto.LoginRequest();
        req.setEmail("not-email"); req.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
