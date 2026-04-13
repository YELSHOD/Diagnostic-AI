package com.yelshod.diagnosticserviceai.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.security.JwtAuthenticationFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiDiagnosisController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiDiagnosisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiDiagnosisService aiDiagnosisService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void returnsStructuredDiagnosis() throws Exception {
        when(aiDiagnosisService.diagnose(any())).thenReturn(
                new AiDiagnosisResponse(
                        "gemini",
                        "gemini-2.5-flash",
                        "v1",
                        "Likely root cause",
                        List.of("Observation A"),
                        "{\"summary\":\"Likely root cause\"}"));

        mockMvc.perform(post("/api/ai/diagnose")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AiDiagnosisRequest("svc", "why?", List.of("line")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("gemini"))
                .andExpect(jsonPath("$.summary").value("Likely root cause"))
                .andExpect(jsonPath("$.bullets[0]").value("Observation A"));
    }

    @Test
    void returnsServiceUnavailableWhenGeminiIsNotConfigured() throws Exception {
        when(aiDiagnosisService.diagnose(any()))
                .thenThrow(new AiDiagnosisDisabledException("Gemini integration is not configured"));

        mockMvc.perform(post("/api/ai/diagnose")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AiDiagnosisRequest("svc", "why?", List.of("line")))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Gemini integration is not configured"));
    }

    @Test
    void returnsBadRequestForInvalidInput() throws Exception {
        when(aiDiagnosisService.diagnose(any()))
                .thenThrow(new IllegalArgumentException("Question or logLines must be provided"));

        mockMvc.perform(post("/api/ai/diagnose")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AiDiagnosisRequest("svc", "", List.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Question or logLines must be provided"));
    }
}
