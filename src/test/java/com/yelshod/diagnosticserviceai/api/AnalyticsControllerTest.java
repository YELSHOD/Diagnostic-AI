package com.yelshod.diagnosticserviceai.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yelshod.diagnosticserviceai.analytics.AnalyticsResponse;
import com.yelshod.diagnosticserviceai.analytics.AnalyticsService;
import com.yelshod.diagnosticserviceai.security.JwtAuthenticationFilter;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void returnsAnalyticsPayloadForExplicitWindow() throws Exception {
        when(analyticsService.getAnalytics(
                Instant.parse("2026-04-09T10:00:00Z"),
                Instant.parse("2026-04-09T11:00:00Z"),
                "orders"))
                .thenReturn(new AnalyticsResponse(List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/analytics")
                        .param("from", "2026-04-09T10:00:00Z")
                        .param("to", "2026-04-09T11:00:00Z")
                        .param("service", "orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorsPerMinute").isArray())
                .andExpect(jsonPath("$.topExceptionTypes").isArray())
                .andExpect(jsonPath("$.topClusters").isArray());

        verify(analyticsService).getAnalytics(
                eq(Instant.parse("2026-04-09T10:00:00Z")),
                eq(Instant.parse("2026-04-09T11:00:00Z")),
                eq("orders"));
    }
}
