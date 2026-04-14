package com.yelshod.diagnosticserviceai.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class HttpGeminiClient implements GeminiClient {

    private final AppProperties appProperties;
    private final DiagnosisPromptFactory diagnosisPromptFactory;
    private final ChatPromptFactory chatPromptFactory;
    private final ObjectMapper objectMapper;

    @Override
    public String generateDiagnosisJson(String model, String prompt) {
        return generateText(model, diagnosisPromptFactory.buildRequestPayload(prompt));
    }

    @Override
    public String generateChatJson(String model, String prompt) {
        return generateText(model, chatPromptFactory.buildRequestPayload(prompt));
    }

    private String generateText(String model, Map<String, Object> requestPayload) {
        RestClient client = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();

        try {
            JsonNode response = client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", appProperties.gemini().apiKey())
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(JsonNode.class);

            return extractModelText(response);
        } catch (AiDiagnosisProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiDiagnosisProviderException("Gemini request failed", ex);
        }
    }

    private String extractModelText(JsonNode response) {
        try {
            if (response == null) {
                return "{}";
            }
            JsonNode textNode = response.path("candidates").path(0).path("content").path(0).path("parts").path(0).path("text");
            if (textNode.isTextual()) {
                return textNode.asText();
            }
            textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isTextual()) {
                return textNode.asText();
            }
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to parse Gemini response", e);
        }
    }
}
