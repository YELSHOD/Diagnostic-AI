package com.yelshod.diagnosticserviceai.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.persistence.entity.RoleEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        RoleEntity role = RoleEntity.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .code("BACKEND")
                .title("Backend")
                .build();
        user = UserEntity.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000101"))
                .email("user@example.com")
                .username("dev.user")
                .passwordHash("encoded-password")
                .status("ACTIVE")
                .createdAt(Instant.parse("2026-04-09T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-09T10:00:00Z"))
                .roles(Set.of(role))
                .build();
    }

    @Test
    void meReturnsCurrentUserSummary() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        MeResponse response = accountService.me(user.getId().toString());

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.roles()).containsExactly("BACKEND");
    }

    @Test
    void updateNormalizesEmailAndPersistsUser() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("new.user")).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MeResponse response = accountService.update(
                user.getId().toString(),
                new UpdateAccountRequest("NEW@example.com", "new.user"));

        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.username()).isEqualTo("new.user");
    }

    @Test
    void changePasswordRequiresCurrentPasswordMatch() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> accountService.changePassword(
                user.getId().toString(),
                new ChangePasswordRequest("wrong-pass", "new-password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePasswordUpdatesStoredHash() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-pass", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-new-password");

        accountService.changePassword(
                user.getId().toString(),
                new ChangePasswordRequest("current-pass", "new-password"));

        assertThat(user.getPasswordHash()).isEqualTo("encoded-new-password");
        verify(userRepository).save(user);
    }
}
