package com.yelshod.diagnosticserviceai.auth;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yelshod.diagnosticserviceai.security.JwtAuthenticationFilter;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void returnsCurrentAccount() throws Exception {
        when(accountService.me("00000000-0000-0000-0000-000000000101")).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/account")
                        .principal(() -> "00000000-0000-0000-0000-000000000101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("BACKEND"));
    }

    @Test
    void updatesCurrentAccount() throws Exception {
        when(accountService.update(
                "00000000-0000-0000-0000-000000000101",
                new UpdateAccountRequest("user@example.com", "dev.user")))
                .thenReturn(sampleResponse());

        mockMvc.perform(patch("/api/account")
                        .principal(() -> "00000000-0000-0000-0000-000000000101")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "username": "dev.user"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dev.user"));
    }

    @Test
    void changesPasswordForCurrentAccount() throws Exception {
        mockMvc.perform(patch("/api/account/password")
                        .principal(() -> "00000000-0000-0000-0000-000000000101")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "current-pass",
                                  "newPassword": "new-password"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(accountService).changePassword(
                "00000000-0000-0000-0000-000000000101",
                new ChangePasswordRequest("current-pass", "new-password"));
    }

    private MeResponse sampleResponse() {
        return new MeResponse(
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                "user@example.com",
                "dev.user",
                "ACTIVE",
                Set.of("BACKEND"),
                Instant.parse("2026-04-09T10:00:00Z"),
                Instant.parse("2026-04-09T10:10:00Z"));
    }
}
