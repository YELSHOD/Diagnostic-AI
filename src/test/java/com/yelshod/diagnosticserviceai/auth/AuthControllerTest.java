package com.yelshod.diagnosticserviceai.auth;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yelshod.diagnosticserviceai.security.JwtAuthenticationFilter;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void registerReturnsAuthPayload() throws Exception {
        when(authService.register(new RegisterRequest("user@example.com", "dev.user", "strong-pass", "BACKEND")))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "username": "dev.user",
                                  "password": "strong-pass",
                                  "role": "BACKEND"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("BACKEND"));
    }

    @Test
    void loginReturnsAuthPayload() throws Exception {
        when(authService.login(new LoginRequest("dev.user", "strong-pass"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "login": "dev.user",
                                  "password": "strong-pass"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("dev.user"));
    }

    @Test
    void refreshReturnsRotatedTokens() throws Exception {
        when(authService.refresh(new RefreshTokenRequest("refresh-token"))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(authService).logout(new RefreshTokenRequest("refresh-token"));
    }

    @Test
    void meReturnsCurrentUserPayload() throws Exception {
        when(accountService.me("00000000-0000-0000-0000-000000000101")).thenReturn(new MeResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                "user@example.com",
                "dev.user",
                "ACTIVE",
                Set.of("BACKEND"),
                java.time.Instant.parse("2026-04-09T10:00:00Z"),
                java.time.Instant.parse("2026-04-09T10:10:00Z")));

        mockMvc.perform(get("/api/auth/me")
                        .principal(() -> "00000000-0000-0000-0000-000000000101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("BACKEND"));
    }

    private AuthResponse sampleResponse() {
        return new AuthResponse(
                "access-token",
                "refresh-token",
                new AuthResponse.UserSummary(
                        UUID.fromString("00000000-0000-0000-0000-000000000101"),
                        "user@example.com",
                        "dev.user",
                        "ACTIVE",
                        Set.of("BACKEND")));
    }
}
