package com.yelshod.diagnosticserviceai.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LogParserTest {

    private final LogParser parser = new LogParser();

    @Test
    void parsesTimestampLevelAndTraceId() {
        ParsedLogLine parsed = parser.parse(
                "orders",
                "2026-04-09T10:15:30Z ERROR traceId=abc-123 failed to place order");

        assertThat(parsed.timestamp()).isEqualTo(Instant.parse("2026-04-09T10:15:30Z"));
        assertThat(parsed.level()).isEqualTo("ERROR");
        assertThat(parsed.traceId()).isEqualTo("abc-123");
        assertThat(parsed.service()).isEqualTo("orders");
    }
}
