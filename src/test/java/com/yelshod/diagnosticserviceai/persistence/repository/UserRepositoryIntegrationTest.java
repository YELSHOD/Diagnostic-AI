package com.yelshod.diagnosticserviceai.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.yelshod.diagnosticserviceai.persistence.entity.RoleEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void persistsAndLoadsUserByEmailAndUsername() {
        userRepository.save(user("user@example.com", "dev.user"));

        assertThat(userRepository.findByEmail("user@example.com")).isPresent();
        assertThat(userRepository.findByUsername("dev.user")).isPresent();
        assertThat(userRepository.existsByEmail("user@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("dev.user")).isTrue();
    }

    private UserEntity user(String email, String username) {
        RoleEntity role = roleRepository.findByCode("BACKEND").orElseThrow();
        Instant now = Instant.parse("2026-04-10T08:00:00Z");
        return UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .username(username)
                .passwordHash("encoded-password")
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .roles(Set.of(role))
                .build();
    }
}
