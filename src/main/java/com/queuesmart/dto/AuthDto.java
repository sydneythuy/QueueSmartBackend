package com.queuesmart.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

public class AuthDto {

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must not exceed 100 characters")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        private String password;

        private String role; // "USER" or "ADMIN", defaults to USER
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String userId;
        private String username;
        private String email;
        private String role;

        public AuthResponse(String token, String userId, String username, String email, String role) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.role = role;
        }
    }
}
