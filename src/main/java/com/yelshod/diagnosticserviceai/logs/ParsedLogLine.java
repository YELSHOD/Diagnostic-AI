package com.yelshod.diagnosticserviceai.logs;

import java.time.Instant;

public record ParsedLogLine(
        Instant timestamp,
        String level,
        String message,
        String traceId,
        String service,
        String raw
) {
}
