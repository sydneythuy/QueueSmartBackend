package com.queuesmart.service;

import com.queuesmart.config.JwtUtil;
import com.queuesmart.dto.AuthDto;
import com.queuesmart.model.UserCredential;
import com.queuesmart.model.UserProfile;
import com.queuesmart.repository.UserCredentialRepository;
import com.queuesmart.repository.UserProfileRepository;
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

    @Mock private UserCredentialRepository credentialRepo;
    @Mock private UserProfileRepository    profileRepo;
    @Mock private PasswordEncoder          passwordEncoder;
    @Mock private JwtUtil                  jwtUtil;

    @InjectMocks private AuthService authService;

    private AuthDto.RegisterRequest registerRequest;
    private AuthDto.LoginRequest    loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthDto.RegisterRequest();
        registerRequest.setUsername("alice");
        registerRequest.setEmail("alice@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new AuthDto.LoginRequest();
        loginRequest.setEmail("alice@example.com");
        loginRequest.setPassword("password123");
    }

    // ── REGISTER ──────────────────────────────────────────────

    @Test
    void register_ValidRequest_ReturnsAuthResponse() {
        when(credentialRepo.existsByEmail(anyString())).thenReturn(false);
        when(profileRepo.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(credentialRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profileRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt.token");

        AuthDto.AuthResponse resp = authService.register(registerRequest);

        assertNotNull(resp);
        assertEquals("alice", resp.getUsername());
        assertEquals("alice@example.com", resp.getEmail());
        assertEquals("USER", resp.getRole());
        assertEquals("jwt.token", resp.getToken());
        verify(credentialRepo).save(any(UserCredential.class));
        verify(profileRepo).save(any(UserProfile.class));
    }

    @Test
    void register_AdminRole_SetsAdminRole() {
        registerRequest.setRole("ADMIN");
        when(credentialRepo.existsByEmail(anyString())).thenReturn(false);
        when(profileRepo.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(credentialRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profileRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("token");

        AuthDto.AuthResponse resp = authService.register(registerRequest);
        assertEquals("ADMIN", resp.getRole());
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        when(credentialRepo.existsByEmail("alice@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(registerRequest));
        assertEquals("Email is already registered", ex.getMessage());
        verify(credentialRepo, never()).save(any());
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        when(credentialRepo.existsByEmail(anyString())).thenReturn(false);
        when(profileRepo.existsByUsername("alice")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));
        verify(credentialRepo, never()).save(any());
    }

    @Test
    void register_PasswordIsHashed_NeverStoredPlainText() {
        when(credentialRepo.existsByEmail(anyString())).thenReturn(false);
        when(profileRepo.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$hashed");
        when(credentialRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(profileRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("t");

        authService.register(registerRequest);

        verify(passwordEncoder).encode("password123");
        verify(credentialRepo).save(argThat(c ->
                c.getPassword().equals("$2a$hashed") &&
                !c.getPassword().equals("password123")
        ));
    }

    // ── LOGIN ─────────────────────────────────────────────────

    @Test
    void login_ValidCredentials_ReturnsToken() {
        UserCredential cred = UserCredential.builder()
                .id("u1").email("alice@example.com")
                .password("hashed").role(UserCredential.Role.USER).build();
        UserProfile profile = UserProfile.builder().username("alice").build();

        when(credentialRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(profileRepo.findByCredentialId("u1")).thenReturn(Optional.of(profile));
        when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn("jwt");

        AuthDto.AuthResponse resp = authService.login(loginRequest);
        assertEquals("alice", resp.getUsername());
        assertEquals("jwt", resp.getToken());
    }

    @Test
    void login_EmailNotFound_ThrowsException() {
        when(credentialRepo.findByEmail("alice@example.com")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.login(loginRequest));
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        UserCredential cred = UserCredential.builder()
                .id("u1").email("alice@example.com")
                .password("hashed").role(UserCredential.Role.USER).build();
        when(credentialRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(cred));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(loginRequest));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    // ── GET USER ──────────────────────────────────────────────

    @Test
    void getUserById_Found_ReturnsCredential() {
        UserCredential cred = UserCredential.builder().id("u1").email("a@b.com").build();
        when(credentialRepo.findById("u1")).thenReturn(Optional.of(cred));
        assertEquals("a@b.com", authService.getUserById("u1").getEmail());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(credentialRepo.findById("bad")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.getUserById("bad"));
    }
}
