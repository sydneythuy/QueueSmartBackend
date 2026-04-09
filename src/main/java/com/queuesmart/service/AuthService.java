package com.queuesmart.service;

import com.queuesmart.config.JwtUtil;
import com.queuesmart.dto.AuthDto;
import com.queuesmart.model.UserCredential;
import com.queuesmart.model.UserProfile;
import com.queuesmart.repository.UserCredentialRepository;
import com.queuesmart.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserCredentialRepository credentialRepo;
    private final UserProfileRepository    profileRepo;
    private final PasswordEncoder          passwordEncoder;
    private final JwtUtil                  jwtUtil;

    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (credentialRepo.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (profileRepo.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }

        UserCredential.Role role = "ADMIN".equalsIgnoreCase(request.getRole())
                ? UserCredential.Role.ADMIN : UserCredential.Role.USER;

        UserCredential credential = UserCredential.builder()
                .id(UUID.randomUUID().toString())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        credentialRepo.save(credential);

        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID().toString())
                .credential(credential)
                .username(request.getUsername())
                .emailVerified(false)
                .build();
        profileRepo.save(profile);

        String token = jwtUtil.generateToken(credential.getId(), credential.getEmail(), role.name());
        return new AuthDto.AuthResponse(token, credential.getId(),
                profile.getUsername(), credential.getEmail(), role.name());
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        UserCredential credential = credentialRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), credential.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String username = profileRepo.findByCredentialId(credential.getId())
                .map(UserProfile::getUsername)
                .orElse(credential.getEmail());

        String token = jwtUtil.generateToken(credential.getId(), credential.getEmail(),
                credential.getRole().name());
        return new AuthDto.AuthResponse(token, credential.getId(),
                username, credential.getEmail(), credential.getRole().name());
    }

    @Transactional(readOnly = true)
    public UserCredential getUserById(String userId) {
        return credentialRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
