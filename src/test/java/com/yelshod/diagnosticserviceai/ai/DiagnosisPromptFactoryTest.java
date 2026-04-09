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
}
