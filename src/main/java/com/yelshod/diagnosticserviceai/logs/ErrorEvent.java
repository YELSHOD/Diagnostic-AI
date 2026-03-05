package com.yelshod.diagnosticserviceai.logs;

import java.time.Instant;
import java.util.List;

public record ErrorEvent(
        String service,
        Instant eventTime,
        String traceId,
        String exceptionType,
        String message,
        List<String> stackFrames,
        String stacktraceFull,
        List<String> contextLines
) {
}
