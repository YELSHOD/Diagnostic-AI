package com.yelshod.diagnosticserviceai.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatPromptFactory chatPromptFactory;
    private final GeminiClient geminiClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public AiChatResponse chat(AiChatRequest request) {
        AppProperties.Gemini gemini = appProperties.gemini();
        if (gemini.apiKey() == null || gemini.apiKey().isBlank()) {
            log.warn("AI chat rejected because Gemini is not configured");
            throw new AiDiagnosisDisabledException("AI assistant is not configured");
        }

        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message must be provided");
        }

        log.info("AI chat requested model={}", gemini.model());
        try {
            String prompt = chatPromptFactory.buildInputJson(request);
            String rawText = geminiClient.generateChatJson(gemini.model(), prompt);
            JsonNode parsed = parseChatJson(rawText);
            AiChatResponse response = new AiChatResponse(
                    "gemini",
                    gemini.model(),
                    gemini.promptVersion(),
                    parsed.path("answer").isTextual() ? parsed.path("answer").asText() : rawText,
                    readStringList(parsed.path("suggestions")),
                    readStringList(parsed.path("relatedPages")),
                    rawText
            );
            log.info("AI chat completed model={}", gemini.model());
            return response;
        } catch (AiDiagnosisProviderException ex) {
            log.error("AI chat provider call failed model={}", gemini.model(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("AI chat provider call failed model={}", gemini.model(), ex);
            throw new AiDiagnosisProviderException("AI assistant request failed", ex);
        }
    }

    private List<String> readStringList(JsonNode node) {
        List<String> items = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> items.add(item.asText()));
        }
        return List.copyOf(items);
    }

    private JsonNode parseChatJson(String rawText) throws JsonProcessingException {
        String normalized = normalizeModelResponse(rawText);
        try {
            return objectMapper.readTree(normalized);
        } catch (JsonProcessingException ex) {
            return objectMapper.valueToTree(new FallbackChat(rawText));
        }
    }

    private String normalizeModelResponse(String rawText) {
        String normalized = rawText == null ? "" : rawText.trim();
        if (normalized.startsWith("```")) {
            int firstNewline = normalized.indexOf('\n');
            if (firstNewline >= 0) {
                normalized = normalized.substring(firstNewline + 1).trim();
            }
            if (normalized.endsWith("```")) {
                normalized = normalized.substring(0, normalized.length() - 3).trim();
            }
        }
        return normalized;
    }

    private record FallbackChat(
            String answer,
            List<String> suggestions,
            List<String> relatedPages
    ) {
        private FallbackChat(String rawText) {
            this(rawText, List.of(), List.of());
        }
    }
}
