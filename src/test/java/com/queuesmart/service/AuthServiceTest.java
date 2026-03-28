package com.queuesmart.service;

import com.queuesmart.config.JwtUtil;
import com.queuesmart.dto.AuthDto;
import com.queuesmart.model.User;
import com.queuesmart.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthService authService;

    private AuthDto.RegisterRequest registerRequest;
    private AuthDto.LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthDto.RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new AuthDto.LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    // ---- REGISTER TESTS ----

    @Test
    void register_Success_ReturnsAuthResponse() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("mock.jwt.token");

        AuthDto.AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("USER", response.getRole());
        assertEquals("mock.jwt.token", response.getToken());
    }

    @Test
    void register_WithAdminRole_SetsAdminRole() {
        registerRequest.setRole("ADMIN");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("token");

        AuthDto.AuthResponse response = authService.register(registerRequest);

        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email is already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(registerRequest));

        assertEquals("Username is already taken", ex.getMessage());
    }

    // ---- LOGIN TESTS ----

    @Test
    void login_Success_ReturnsAuthResponse() {
        User storedUser = User.builder()
                .id("user-1")
                .username("testuser")
                .email("test@example.com")
                .password("hashed_password")
                .role(User.Role.USER)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt.token");

        AuthDto.AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("jwt.token", response.getToken());
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        User storedUser = User.builder()
                .id("user-1")
                .email("test@example.com")
                .password("hashed_password")
                .role(User.Role.USER)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", ex.getMessage());
    }

    // ---- GET USER TESTS ----

    @Test
    void getUserById_Found_ReturnsUser() {
        User user = User.builder().id("user-1").username("testuser").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        User result = authService.getUserById("user-1");
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.getUserById("bad-id"));
    }


    @Test
    void testRegister_shortUsername_throwsException() {
        com.queuesmart.dto.AuthDto.RegisterRequest req = new com.queuesmart.dto.AuthDto.RegisterRequest();
        req.setUsername("ab");
        req.setEmail("short@example.com");
        req.setPassword("password123");
        assertThrows(Exception.class, () -> authService.register(req));
    }

    @Test
    void testRegister_adminRole_assignedCorrectly() {
        com.queuesmart.dto.AuthDto.RegisterRequest req = new com.queuesmart.dto.AuthDto.RegisterRequest();
        req.setUsername("adminuser");
        req.setEmail("admin2@example.com");
        req.setPassword("adminpass1");
        req.setRole("ADMIN");
        var response = authService.register(req);
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void testLogin_nonExistentEmail_throwsException() {
        com.queuesmart.dto.AuthDto.LoginRequest req = new com.queuesmart.dto.AuthDto.LoginRequest();
        req.setEmail("ghost@example.com");
        req.setPassword("doesntmatter");
        assertThrows(Exception.class, () -> authService.login(req));
    }

}
