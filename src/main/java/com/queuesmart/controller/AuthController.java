package com.queuesmart.controller;

import com.queuesmart.dto.ApiResponse;
import com.queuesmart.dto.AuthDto;
import com.queuesmart.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register
     * Body: { username, email, password, role? }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {

        AuthDto.AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    /**
     * POST /api/auth/login
     * Body: { email, password }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {

        AuthDto.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
