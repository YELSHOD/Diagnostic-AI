package com.yelshod.diagnosticserviceai.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.auth.JwtService;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.UserRepository;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthHandshakeInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WebSocketAuthHandshakeInterceptor interceptor;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000101"))
                .email("user@example.com")
                .username("dev.user")
                .passwordHash("encoded")
                .status("ACTIVE")
                .createdAt(Instant.parse("2026-04-10T08:00:00Z"))
                .updatedAt(Instant.parse("2026-04-10T08:00:00Z"))
                .roles(Set.of())
                .build();
    }

    @Test
    void rejectsHandshakeWithoutToken() throws Exception {
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(
                request("ws://localhost/ws/logs?containerId=orders"),
                response(),
                null,
                attributes);

        assertThat(allowed).isFalse();
        assertThat(attributes).isEmpty();
    }

    @Test
    void rejectsHandshakeWithUnknownUser() throws Exception {
        Map<String, Object> attributes = new HashMap<>();

        when(jwtService.extractSubject("valid-token")).thenReturn(user.getId().toString());
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        boolean allowed = interceptor.beforeHandshake(
                request("ws://localhost/ws/logs?containerId=orders&token=valid-token"),
                response(),
                null,
                attributes);

        assertThat(allowed).isFalse();
    }

    @Test
    void storesAuthenticatedUserInHandshakeAttributes() throws Exception {
        Map<String, Object> attributes = new HashMap<>();

        when(jwtService.extractSubject("valid-token")).thenReturn(user.getId().toString());
        when(jwtService.isTokenValid("valid-token", user.getId().toString())).thenReturn(true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        boolean allowed = interceptor.beforeHandshake(
                request("ws://localhost/ws/logs?containerId=orders&token=valid-token"),
                response(),
                null,
                attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry("userId", user.getId().toString());
    }

    private ServerHttpRequest request(String uri) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", URI.create(uri).getPath());
        servletRequest.setQueryString(URI.create(uri).getQuery());
        return new ServletServerHttpRequest(servletRequest);
    }

    private ServerHttpResponse response() {
        return new org.springframework.http.server.ServletServerHttpResponse(new MockHttpServletResponse());
    }
}
