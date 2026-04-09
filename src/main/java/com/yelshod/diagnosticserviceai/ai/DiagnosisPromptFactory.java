package com.yelshod.diagnosticserviceai.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiagnosisPromptFactory {

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

    private final RedactionService redactionService;
    private final ObjectMapper objectMapper;

    public String buildInputJson(ErrorEvent event) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "systemPrompt", SYSTEM_PROMPT,
                    "userPrompt", USER_PROMPT_TEMPLATE.formatted(objectMapper.writeValueAsString(Map.of(
                            "service", event.service(),
                            "eventTime", event.eventTime(),
                            "exceptionType", event.exceptionType(),
                            "message", redactionService.redact(event.message()),
                            "topFrames", event.stackFrames(),
                            "stacktrace", redactionService.redact(event.stacktraceFull()),
                            "context", event.contextLines().stream().map(redactionService::redact).toList()
                    )))
            ));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to build AI diagnosis prompt", e);
        }
    }

    public Map<String, Object> buildRequestPayload(String inputJson) {
        return Map.of(
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
    }
}
