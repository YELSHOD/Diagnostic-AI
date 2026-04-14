package com.yelshod.diagnosticserviceai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosisPromptFactoryTest {

    @Test
    void buildsPromptPayloadWithRedactedSensitiveFields() {
        DiagnosisPromptFactory factory = new DiagnosisPromptFactory(
                new RedactionService(),
                JsonMapper.builder().findAndAddModules().build());
        ErrorEvent event = new ErrorEvent(
                "orders",
                Instant.parse("2026-04-09T10:00:00Z"),
                "trace-1",
                "IllegalStateException",
                "Authorization: secret",
                List.of("OrdersService.placeOrder"),
                "stack",
                List.of("password=topsecret"));

        String prompt = factory.buildInputJson(event);

        assertThat(prompt).contains("[REDACTED]");
        assertThat(prompt).doesNotContain("secret");
        assertThat(prompt).doesNotContain("topsecret");
    }

    @Test
    void buildsPromptForAdHocDiagnosisRequest() {
        DiagnosisPromptFactory factory = new DiagnosisPromptFactory(
                new RedactionService(),
                JsonMapper.builder().findAndAddModules().build());
        AiDiagnosisRequest request = new AiDiagnosisRequest(
                "diagnosticserviceai",
                "Why is this service unstable?",
                List.of(
                        "2026-04-13T11:21:40Z WARN Docker discovery skipped",
                        "2026-04-13T11:21:41Z ERROR Authorization: secret"
                ),
                new AiDiagnosisRequest.TimeRange(
                        "relative",
                        "Showing: 15m",
                        Instant.parse("2026-04-13T11:06:00Z"),
                        Instant.parse("2026-04-13T11:21:00Z")
                ),
                "ERROR",
                "socket"
        );

        String prompt = factory.buildInputJson(request);

        assertThat(prompt).contains("diagnosticserviceai");
        assertThat(prompt).contains("Why is this service unstable?");
        assertThat(prompt).contains("Showing: 15m");
        assertThat(prompt).contains("\"from\":1776078360.000000000");
        assertThat(prompt).contains("\"to\":1776079260.000000000");
        assertThat(prompt).contains("\"levelFilter\":\"ERROR\"");
        assertThat(prompt).contains("\"textFilter\":\"socket\"");
        assertThat(prompt).contains("Docker discovery skipped");
        assertThat(prompt).contains("[REDACTED]");
        assertThat(prompt).doesNotContain("Authorization: secret");
    }

    @Test
    void instructsModelToAnswerUsingTheQuestionLanguage() {
        DiagnosisPromptFactory factory = new DiagnosisPromptFactory(
                new RedactionService(),
                JsonMapper.builder().findAndAddModules().build());
        String inputJson = """
                {"question":"Проанализируй логи ресторана и ответь на русском"}
                """;

        JsonNode payload = JsonMapper.builder().findAndAddModules().build()
                .valueToTree(factory.buildRequestPayload(inputJson));

        String systemPrompt = payload.path("systemInstruction")
                .path("parts").path(0).path("text").asText();
        String userPrompt = payload.path("contents")
                .path(0).path("parts").path(0).path("text").asText();

        assertThat(systemPrompt).contains("Use the same natural language as the user's question");
        assertThat(userPrompt).contains("Russian");
    }
}
