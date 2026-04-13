package com.yelshod.diagnosticserviceai.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.util.List;
import java.util.LinkedHashMap;
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
            Analyze the supplied log interval and produce a structured diagnosis.

            Constraints:
            - Output must be valid JSON with the exact schema below.
            - Analyze only the supplied time window and supplied filtered log lines.
            - Treat timestamps as the primary ordering signal.
            - Separate evidence from inference.
            - Answer the user's question directly and concisely.

            Schema:
            {
              \"summary\": string,
              \"timeline\": [string],
              \"probableRootCause\": string,
              \"evidence\": [string],
              \"nextChecks\": [string]
            }

            Input (JSON):
            %s
            """;

    private final RedactionService redactionService;
    private final ObjectMapper objectMapper;

    public String buildInputJson(AiDiagnosisRequest request) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("service", request.service() == null ? "" : request.service());
            payload.put("question", redactionService.redact(request.question() == null ? "" : request.question()));
            payload.put("timeRange", buildTimeRangePayload(request.timeRange()));
            payload.put("levelFilter", request.levelFilter() == null ? "" : request.levelFilter());
            payload.put("textFilter", redactionService.redact(request.textFilter() == null ? "" : request.textFilter()));
            payload.put("logLines", request.logLines() == null
                    ? List.of()
                    : request.logLines().stream()
                            .limit(50)
                            .map(redactionService::redact)
                            .toList());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to build AI diagnosis prompt", e);
        }
    }

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

    private Map<String, Object> buildTimeRangePayload(AiDiagnosisRequest.TimeRange timeRange) {
        if (timeRange == null) {
            return Map.of();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("mode", timeRange.mode() == null ? "" : timeRange.mode());
        payload.put("label", timeRange.label() == null ? "" : timeRange.label());
        payload.put("from", timeRange.from());
        payload.put("to", timeRange.to());
        return payload;
    }
}
