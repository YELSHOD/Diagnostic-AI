package com.yelshod.diagnosticserviceai.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AiChatController.class, AiApiExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AiApiExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatService aiChatService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void mapsChatProviderFailureToBadGateway() throws Exception {
        when(aiChatService.chat(any()))
                .thenThrow(new AiDiagnosisProviderException("AI assistant request failed", new RuntimeException("quota")));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AiChatRequest("Привет", java.util.List.of(), null))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI assistant request failed"));
    }
}
