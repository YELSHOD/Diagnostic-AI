package com.yelshod.diagnosticserviceai.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.yelshod.diagnosticserviceai.persistence.entity.RefreshTokenEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.RoleEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RefreshTokenRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void persistsAndLoadsRefreshTokenByHash() {
        UserEntity user = userRepository.save(user("user@example.com", "dev.user"));
        RefreshTokenEntity token = refreshTokenRepository.save(RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("token-hash")
                .expiresAt(Instant.parse("2026-04-11T08:00:00Z"))
                .revoked(false)
                .createdAt(Instant.parse("2026-04-10T08:00:00Z"))
                .build());

        assertThat(refreshTokenRepository.findByTokenHash("token-hash"))
                .get()
                .extracting(RefreshTokenEntity::getId, value -> value.getUser().getId())
                .containsExactly(token.getId(), user.getId());
        assertThat(refreshTokenRepository.findAllByUser(user)).hasSize(1);
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
