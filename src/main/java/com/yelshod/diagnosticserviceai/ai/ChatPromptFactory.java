package com.yelshod.diagnosticserviceai.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatPromptFactory {

    private static final String PRODUCT_KNOWLEDGE = """
            Product knowledge:
            - Overview: shows current system activity, exception patterns, and cluster summaries.
            - Runtime targets: lists local services and demo targets, and lets the operator open live logs.
            - Live Logs: streams logs for a selected target, supports text, level, and time-range filters, and can run AI diagnosis.
            - Analysis: summarizes realtime cluster updates and current incident patterns.
            - Settings: changes frontend API/WS connection settings, reconnect behavior, theme, and language. It does not change runtime target ports.
            - Account: shows user account details and supports password updates inside the account flow.
            - AI Chat: answers general product questions and can use current live context if it is supplied.
            - Demo scenarios: backend can generate demo business logs such as orders-demo and restaurant-demo.
            Constraints:
            - Do not invent features, pages, buttons, or endpoints that are not in this product.
            - If something is not available, say that clearly.
            - Give direct navigation guidance when asked where to do something.
            """;

    private static final String SYSTEM_PROMPT = """
            You are an AI assistant for an observability and diagnostics product.
            Return ONLY valid JSON, no markdown, no extra text.
            Answer in the same language as the user's latest message.
            Keep the tone natural and helpful, like a normal assistant.
            Do not use diagnosis sections such as root cause, evidence, or next checks unless the user explicitly asks for a structured analysis.
            Keep JSON field names in English.
            """;

    private static final String USER_PROMPT_TEMPLATE = """
            Answer the user's message as a product-aware assistant.

            Constraints:
            - Output must be valid JSON with the exact schema below.
            - The main answer must be plain conversational text.
            - Suggestions are optional short follow-up prompts.
            - Related pages are optional page names only.
            - Use any provided live context only when it is relevant to the user's question.

            Schema:
            {
              "answer": string,
              "suggestions": [string],
              "relatedPages": [string]
            }

            Input (JSON):
            %s
            """;

    private final RedactionService redactionService;
    private final ObjectMapper objectMapper;

    public String buildInputJson(AiChatRequest request) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("message", redactionService.redact(request.message() == null ? "" : request.message()));
            payload.put("history", request.history() == null
                    ? List.of()
                    : request.history().stream()
                            .limit(12)
                            .map(message -> Map.of(
                                    "role", message.role() == null ? "" : message.role(),
                                    "content", redactionService.redact(message.content() == null ? "" : message.content())
                            ))
                            .toList());
            payload.put("context", buildContextPayload(request.context()));
            payload.put("productKnowledge", PRODUCT_KNOWLEDGE);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to build AI chat prompt", e);
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
                "generationConfig", Map.of("temperature", 0.4)
        );
    }

    private Map<String, Object> buildContextPayload(AiChatRequest.Context context) {
        if (context == null) {
            return Map.of();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service", context.service() == null ? "" : context.service());
        payload.put("logLines", context.logLines() == null
                ? List.of()
                : context.logLines().stream()
                        .limit(20)
                        .map(redactionService::redact)
                        .toList());
        return payload;
    }
}
