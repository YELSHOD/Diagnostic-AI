package com.yelshod.diagnosticserviceai.auth;

import com.yelshod.diagnosticserviceai.persistence.entity.RefreshTokenEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.RoleEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.RefreshTokenRepository;
import com.yelshod.diagnosticserviceai.persistence.repository.RoleRepository;
import com.yelshod.diagnosticserviceai.persistence.repository.UserRepository;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }

        RoleEntity role = roleRepository.findByCode(request.role())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role"));
        Instant now = Instant.now();
        UserEntity user = userRepository.save(UserEntity.builder()
                .id(UUID.randomUUID())
                .email(normalizedEmail)
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .roles(Set.of(role))
                .build());

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = findUserByLogin(request.login())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        String tokenHash = hashToken(refreshToken);
        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .filter(token -> !token.isRevoked())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        Claims claims = jwtService.extractAllClaims(refreshToken);
        if (claims.getExpiration().before(java.util.Date.from(Instant.now()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        String expectedSubject = storedToken.getUser().getId().toString();
        if (!jwtService.isTokenValid(refreshToken, expectedSubject)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        return issueTokens(storedToken.getUser());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenHash(hashToken(request.refreshToken()))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private Optional<UserEntity> findUserByLogin(String login) {
        if (login.contains("@")) {
            return userRepository.findByEmail(normalizeEmail(login));
        }
        return userRepository.findByUsername(login);
    }

    private AuthResponse issueTokens(UserEntity user) {
        Set<String> roles = user.getRoles().stream()
                .map(RoleEntity::getCode)
                .collect(Collectors.toSet());
        Map<String, Object> claims = Map.of(
                "email", user.getEmail(),
                "username", user.getUsername(),
                "roles", roles);
        String subject = user.getId().toString();
        String accessToken = jwtService.generateAccessToken(subject, claims);
        String refreshToken = jwtService.generateRefreshToken(subject, Map.of("type", "refresh"));

        persistRefreshToken(user, refreshToken);
        return new AuthResponse(
                accessToken,
                refreshToken,
                new AuthResponse.UserSummary(
                        user.getId(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getStatus(),
                        roles));
    }

    private void persistRefreshToken(UserEntity user, String refreshToken) {
        Claims claims = jwtService.extractAllClaims(refreshToken);
        refreshTokenRepository.save(RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(claims.getExpiration().toInstant())
                .revoked(false)
                .createdAt(Instant.now())
                .build());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
