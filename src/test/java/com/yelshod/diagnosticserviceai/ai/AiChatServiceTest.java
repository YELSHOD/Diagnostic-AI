package com.yelshod.diagnosticserviceai.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiChatServiceTest {

    private final ChatPromptFactory promptFactory =
            new ChatPromptFactory(new RedactionService(), JsonMapper.builder().findAndAddModules().build());
    private final GeminiClient geminiClient = mock(GeminiClient.class);

    @Test
    void throwsWhenAiAssistantIsNotConfigured() {
        AiChatService service = new AiChatService(
                promptFactory,
                geminiClient,
                new AppProperties(
                        new AppProperties.Docker(true, "label", "value", 100),
                        new AppProperties.Gemini("", "gemini-2.5-flash", "v1"),
                        new AppProperties.Runtime(List.of()),
                        new AppProperties.Demo(false, false, 0, "", "", "")
                ),
                JsonMapper.builder().findAndAddModules().build()
        );

        assertThatThrownBy(() -> service.chat(new AiChatRequest("Привет", List.of(), null)))
                .isInstanceOf(AiDiagnosisDisabledException.class)
                .hasMessage("AI assistant is not configured");
    }

    @Test
    void mapsChatJsonIntoConversationalResponse() {
        when(geminiClient.generateChatJson(anyString(), anyString()))
                .thenReturn("""
                        {
                          "answer":"Поменять пароль можно на странице Account.",
                          "suggestions":["Как открыть Account?"],
                          "relatedPages":["Account","Settings"]
                        }
                        """);

        AiChatService service = new AiChatService(
                promptFactory,
                geminiClient,
                new AppProperties(
                        new AppProperties.Docker(true, "label", "value", 100),
                        new AppProperties.Gemini("secret", "gemini-2.5-flash", "v1"),
                        new AppProperties.Runtime(List.of()),
                        new AppProperties.Demo(false, false, 0, "", "", "")
                ),
                JsonMapper.builder().findAndAddModules().build()
        );

        AiChatResponse response = service.chat(new AiChatRequest(
                "Где поменять пароль?",
                List.of(new AiChatRequest.Message("user", "Привет")),
                null
        ));

        assertThat(response.answer()).isEqualTo("Поменять пароль можно на странице Account.");
        assertThat(response.suggestions()).containsExactly("Как открыть Account?");
        assertThat(response.relatedPages()).containsExactly("Account", "Settings");
    }

    @Test
    void fallsBackToRawTextWhenModelReturnsPlainText() {
        when(geminiClient.generateChatJson(anyString(), anyString()))
                .thenReturn("Привет. Я могу помочь с приложением.");

        AiChatService service = new AiChatService(
                promptFactory,
                geminiClient,
                new AppProperties(
                        new AppProperties.Docker(true, "label", "value", 100),
                        new AppProperties.Gemini("secret", "gemini-2.5-flash", "v1"),
                        new AppProperties.Runtime(List.of()),
                        new AppProperties.Demo(false, false, 0, "", "", "")
                ),
                JsonMapper.builder().findAndAddModules().build()
        );

        AiChatResponse response = service.chat(new AiChatRequest("Привет", List.of(), null));

        assertThat(response.answer()).isEqualTo("Привет. Я могу помочь с приложением.");
        assertThat(response.suggestions()).isEmpty();
        assertThat(response.relatedPages()).isEmpty();
    }
}
