package com.yelshod.diagnosticserviceai.auth;

import com.yelshod.diagnosticserviceai.persistence.entity.RoleEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MeResponse me(String principalName) {
        UserEntity user = loadUser(principalName);
        log.info("Account profile fetched userId={}", user.getId());
        return toMeResponse(user);
    }

    @Transactional
    public MeResponse update(String principalName, UpdateAccountRequest request) {
        UserEntity user = loadUser(principalName);
        String normalizedEmail = normalizeEmail(request.email());
        if (!user.getEmail().equals(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Account profile update rejected duplicate email={} userId={}", normalizedEmail, user.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already in use");
        }
        if (!user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            log.warn("Account profile update rejected duplicate username={} userId={}", request.username(), user.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use");
        }

        user.setEmail(normalizedEmail);
        user.setUsername(request.username());
        user.setUpdatedAt(Instant.now());
        UserEntity updatedUser = userRepository.save(user);
        log.info("Account profile updated userId={} email={} username={}",
                updatedUser.getId(), updatedUser.getEmail(), updatedUser.getUsername());
        return toMeResponse(updatedUser);
    }

    @Transactional
    public void changePassword(String principalName, ChangePasswordRequest request) {
        UserEntity user = loadUser(principalName);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("Password change rejected userId={} reason=invalid-current-password", user.getId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is invalid");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        log.info("Password changed userId={}", user.getId());
    }

    private UserEntity loadUser(String principalName) {
        try {
            UUID userId = UUID.fromString(principalName);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid principal");
        }
    }

    private MeResponse toMeResponse(UserEntity user) {
        Set<String> roles = user.getRoles().stream()
                .map(RoleEntity::getCode)
                .collect(Collectors.toSet());
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getStatus(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
