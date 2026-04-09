package com.yelshod.diagnosticserviceai.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedactionServiceTest {

    private final RedactionService redactionService = new RedactionService();

    @Test
    void redactsAuthorizationAndPasswordValues() {
        String redacted = redactionService.redact("Authorization: secret password=topsecret");

        assertThat(redacted).isEqualTo("Authorization: [REDACTED] password=[REDACTED]");
    }
}
