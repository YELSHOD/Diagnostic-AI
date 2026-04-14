package com.yelshod.diagnosticserviceai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatPromptFactoryTest {

    @Test
    void buildsPromptWithHistoryAndContext() {
        ChatPromptFactory factory = new ChatPromptFactory(
                new RedactionService(),
                JsonMapper.builder().findAndAddModules().build());

        String prompt = factory.buildInputJson(new AiChatRequest(
                "Где поменять пароль?",
                List.of(
                        new AiChatRequest.Message("user", "Привет"),
                        new AiChatRequest.Message("assistant", "Здравствуйте")
                ),
                new AiChatRequest.Context("restaurant-demo", List.of("Authorization: secret"))
        ));

        assertThat(prompt).contains("Где поменять пароль?");
        assertThat(prompt).contains("\"role\":\"user\"");
        assertThat(prompt).contains("\"service\":\"restaurant-demo\"");
        assertThat(prompt).contains("[REDACTED]");
        assertThat(prompt).contains("Product knowledge");
    }

    @Test
    void instructsModelToAnswerLikeANormalAssistant() {
        ChatPromptFactory factory = new ChatPromptFactory(
                new RedactionService(),
                JsonMapper.builder().findAndAddModules().build());

        JsonNode payload = JsonMapper.builder().findAndAddModules().build()
                .valueToTree(factory.buildRequestPayload("{\"message\":\"Привет\"}"));

        String systemPrompt = payload.path("systemInstruction")
                .path("parts").path(0).path("text").asText();
        String userPrompt = payload.path("contents")
                .path(0).path("parts").path(0).path("text").asText();

        assertThat(systemPrompt).contains("normal assistant");
        assertThat(systemPrompt).contains("same language");
        assertThat(userPrompt).contains("\"answer\": string");
        assertThat(userPrompt).contains("\"relatedPages\": [string]");
    }
}
