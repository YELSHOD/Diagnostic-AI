package com.yelshod.diagnosticserviceai.runtime;

import java.util.Map;

public record RuntimeTargetDto(
        String id,
        String name,
        RuntimeTargetType type,
        RuntimeTargetStatus status,
        String host,
        Integer port,
        String healthUrl,
        LogSourceType logSourceType,
        String logSourceRef,
        Map<String, String> metadata
) {
}
