package com.yelshod.diagnosticserviceai.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import com.yelshod.diagnosticserviceai.persistence.entity.AiDiagnosisEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.AiDiagnosisRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDiagnosisService {

    private static final String SYSTEM_PROMPT = """
            You are a senior backend/SRE engineer. Your task is to diagnose software errors from log events.
            Return ONLY valid JSON, no markdown, no extra text.
            Be concise, technical, and actionable.
            If information is insufficient, say so and list what additional data is needed.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Analyze this error event and produce a structured diagnosis.

            Constraints:
            - Output must be valid JSON with the exact schema below.
            - Do not invent stack frames or files that are not present.
            - Use the provided stacktrace to infer whereToLook.
            - Provide ranked hypotheses (max 3).

            Schema:
            {
              \"title\": string,
              \"severity\": \"low\" | \"medium\" | \"high\" | \"critical\",
              \"mostLikelyRootCause\": string,
              \"rootCauseHypotheses\": [
                { \"cause\": string, \"why\": string, \"confidence\": number }
              ],
              \"whereToLook\": [
                { \"class\": string, \"method\": string, \"file\": string, \"line\": number }
              ],
              \"immediateActions\": [string],
              \"preventiveActions\": [string],
              \"missingInfo\": [string],
              \"confidence\": number
            }

            Input (JSON):
            %s
            """;

    private final AiDiagnosisRepository aiDiagnosisRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final RedactionService redactionService;

    public void diagnoseNewCluster(String clusterKey, ErrorEvent event) {
        if (appProperties.gemini().apiKey() == null || appProperties.gemini().apiKey().isBlank()) {
            return;
        }
        try {
            String inputJson = objectMapper.writeValueAsString(Map.of(
                    "service", event.service(),
                    "eventTime", event.eventTime(),
                    "exceptionType", event.exceptionType(),
                    "message", redactionService.redact(event.message()),
                    "topFrames", event.stackFrames(),
                    "stacktrace", redactionService.redact(event.stacktraceFull()),
                    "context", event.contextLines().stream().map(redactionService::redact).toList()
            ));

            Map<String, Object> payload = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                    ),
                    "contents", List.of(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of(
                                    "text", USER_PROMPT_TEMPLATE.formatted(inputJson)
                            ))
                    )),
                    "generationConfig", Map.of("temperature", 0.2)
            );

            RestClient client = RestClient.builder()
                    .baseUrl("https://generativelanguage.googleapis.com")
                    .build();

            JsonNode response = client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", appProperties.gemini().apiKey())
                            .build(appProperties.gemini().model()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            String diagnosisJson = extractModelText(response);
            aiDiagnosisRepository.save(AiDiagnosisEntity.builder()
                    .clusterKey(clusterKey)
                    .model(appProperties.gemini().model())
                    .promptVersion(appProperties.gemini().promptVersion())
                    .diagnosisJson(diagnosisJson)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception ex) {
            log.warn("Failed to get Gemini diagnosis for cluster {}", clusterKey, ex);
        }
    }

    private String extractModelText(JsonNode response) throws JsonProcessingException {
        if (response == null) {
            return "{}";
        }
        JsonNode textNode = response.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isTextual()) {
            return textNode.asText();
        }
        return objectMapper.writeValueAsString(response);
    }
}
