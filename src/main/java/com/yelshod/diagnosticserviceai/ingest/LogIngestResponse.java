package com.yelshod.diagnosticserviceai.ingest;

public record LogIngestResponse(
        String status,
        String runtimeTargetId,
        String clusterKey,
        boolean errorRecorded
) {
}
