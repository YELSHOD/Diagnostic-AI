package com.yelshod.diagnosticserviceai.ai;

import static org.assertj.core.api.Assertions.assertThat;

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
                ));

        String prompt = factory.buildInputJson(request);

        assertThat(prompt).contains("diagnosticserviceai");
        assertThat(prompt).contains("Why is this service unstable?");
        assertThat(prompt).contains("Docker discovery skipped");
        assertThat(prompt).contains("[REDACTED]");
        assertThat(prompt).doesNotContain("Authorization: secret");
    }
}
