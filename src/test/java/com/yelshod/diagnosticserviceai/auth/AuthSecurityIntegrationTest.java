package com.yelshod.diagnosticserviceai.auth;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yelshod.diagnosticserviceai.persistence.entity.RoleEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.UserEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.RefreshTokenRepository;
import com.yelshod.diagnosticserviceai.persistence.repository.RoleRepository;
import com.yelshod.diagnosticserviceai.persistence.repository.UserRepository;
import com.yelshod.diagnosticserviceai.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AuthSecurityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void rejectsProtectedAccountEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsPublicLoginEndpointWithoutToken() throws Exception {
        createUser("user@example.com", "dev.user");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "user@example.com",
                                  "password": "strong-pass"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.user.email").value("user@example.com"));
    }

    @Test
    void allowsCorsPreflightForPublicLoginEndpoint() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
    }

    @Test
    void allowsCorsPreflightForPublicRegisterEndpointFromLanOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/register")
                        .header("Origin", "http://192.168.1.25:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://192.168.1.25:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
    }

    @Test
    void includesCorsHeadersOnActualPublicRegisterResponse() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Origin", "http://localhost:5173")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "new-user@example.com",
                                  "username": "new.user",
                                  "password": "strong-pass",
                                  "role": "BACKEND"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.user.email").value("new-user@example.com"));
    }

    @Test
    void allowsProtectedAccountEndpointWithValidBearerToken() throws Exception {
        UserEntity user = createUser("user@example.com", "dev.user");
        String accessToken = jwtService.generateAccessToken(user.getId().toString(), Map.of(
                "email", user.getEmail(),
                "username", user.getUsername(),
                "roles", Set.of("BACKEND")));

        mockMvc.perform(get("/api/account")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.username").value("dev.user"))
                .andExpect(jsonPath("$.roles[0]").value("BACKEND"));
    }

    private UserEntity createUser(String email, String username) {
        RoleEntity role = roleRepository.findByCode("BACKEND").orElseThrow();
        Instant now = Instant.parse("2026-04-10T08:00:00Z");
        return userRepository.save(UserEntity.builder()
                .id(UUID.randomUUID())
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode("strong-pass"))
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .roles(Set.of(role))
                .build());
    }
}
