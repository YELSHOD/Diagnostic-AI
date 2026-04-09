package com.yelshod.diagnosticserviceai.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.persistence.entity.RefreshTokenEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.RoleEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.RefreshTokenRepository;
import com.yelshod.diagnosticserviceai.persistence.repository.RoleRepository;
import com.yelshod.diagnosticserviceai.persistence.repository.UserRepository;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private RoleEntity backendRole;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        backendRole = RoleEntity.builder()
                .id(ROLE_ID)
                .code("BACKEND")
                .title("Backend")
                .build();

        user = UserEntity.builder()
                .id(USER_ID)
                .email("user@example.com")
                .username("dev.user")
                .passwordHash("encoded-password")
                .status("ACTIVE")
                .createdAt(Instant.parse("2026-04-09T10:00:00Z"))
                .updatedAt(Instant.parse("2026-04-09T10:00:00Z"))
                .roles(Set.of(backendRole))
                .build();
    }

    @Test
    void registerCreatesUserAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest("USER@Example.com", "dev.user", "strong-pass", "BACKEND");
        Claims refreshClaims = claimsExpiringAt(Instant.now().plusSeconds(600));

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("dev.user")).thenReturn(false);
        when(roleRepository.findByCode("BACKEND")).thenReturn(Optional.of(backendRole));
        when(passwordEncoder.encode("strong-pass")).thenReturn("encoded-password");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtService.extractAllClaims("refresh-token")).thenReturn(refreshClaims);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.user().username()).isEqualTo("dev.user");
        assertThat(response.user().roles()).containsExactly("BACKEND");

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        UserEntity savedUser = userCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(savedUser.getRoles()).extracting(RoleEntity::getCode).containsExactly("BACKEND");

        verify(refreshTokenRepository).save(any(RefreshTokenEntity.class));
    }

    @Test
    void loginSupportsEmail() {
        LoginRequest request = new LoginRequest("USER@EXAMPLE.COM", "secret");
        Claims refreshClaims = claimsExpiringAt(Instant.now().plusSeconds(600));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded-password")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");
        when(jwtService.extractAllClaims("refresh-token")).thenReturn(refreshClaims);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.user().roles()).containsExactly("BACKEND");
    }

    @Test
    void refreshRotatesExistingToken() {
        String refreshToken = "refresh-token";
        String rotatedToken = "refresh-token-2";
        String accessToken = "access-token-2";
        RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(AuthService.hashToken(refreshToken))
                .expiresAt(Instant.now().plusSeconds(600))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        Claims claims = claimsExpiringAt(Instant.now().plusSeconds(600));
        Claims rotatedClaims = claimsExpiringAt(Instant.now().plusSeconds(1200));

        when(jwtService.extractAllClaims(refreshToken)).thenReturn(claims);
        when(jwtService.isTokenValid(refreshToken, user.getId().toString())).thenReturn(true);
        when(refreshTokenRepository.findByTokenHash(AuthService.hashToken(refreshToken)))
                .thenReturn(Optional.of(storedToken));
        when(jwtService.generateAccessToken(any(), any())).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(any(), any())).thenReturn(rotatedToken);
        when(jwtService.extractAllClaims(rotatedToken)).thenReturn(rotatedClaims);
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refresh(new RefreshTokenRequest(refreshToken));

        assertThat(response.accessToken()).isEqualTo(accessToken);
        assertThat(response.refreshToken()).isEqualTo(rotatedToken);
        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshTokenEntity.class));
    }

    @Test
    void logoutRevokesKnownRefreshToken() {
        String refreshToken = "refresh-token";
        RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(AuthService.hashToken(refreshToken))
                .expiresAt(Instant.now().plusSeconds(600))
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        when(refreshTokenRepository.findByTokenHash(AuthService.hashToken(refreshToken)))
                .thenReturn(Optional.of(storedToken));

        authService.logout(new RefreshTokenRequest(refreshToken));

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void registerRejectsUnknownRole() {
        RegisterRequest request = new RegisterRequest("user@example.com", "dev.user", "strong-pass", "UNKNOWN");

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("dev.user")).thenReturn(false);
        when(roleRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void loginRejectsInvalidCredentials() {
        LoginRequest request = new LoginRequest("dev.user", "bad-pass");

        when(userRepository.findByUsername("dev.user")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad-pass", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Claims claimsExpiringAt(Instant expiration) {
        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(java.util.Date.from(expiration));
        return claims;
    }
}
