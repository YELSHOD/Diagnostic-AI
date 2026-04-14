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

@WebMvcTest({AiChatController.class, AiDiagnosisController.class})
@AutoConfigureMockMvc(addFilters = false)
class AiChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AiChatService aiChatService;

    @MockBean
    private AiDiagnosisService aiDiagnosisService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void returnsConversationalAiResponse() throws Exception {
        when(aiChatService.chat(any())).thenReturn(
                new AiChatResponse(
                        "gemini",
                        "gemini-2.5-flash",
                        "v1",
                        "Поменять пароль можно на странице Account.",
                        List.of("Как открыть Account?"),
                        List.of("Account"),
                        "{\"answer\":\"ok\"}"
                ));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AiChatRequest(
                                        "Где поменять пароль?",
                                        List.of(new AiChatRequest.Message("user", "Привет")),
                                        null
                                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Поменять пароль можно на странице Account."))
                .andExpect(jsonPath("$.suggestions[0]").value("Как открыть Account?"))
                .andExpect(jsonPath("$.relatedPages[0]").value("Account"));
    }
}
