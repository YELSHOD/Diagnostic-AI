package com.yelshod.diagnosticserviceai.project;

import java.time.Instant;
import java.util.UUID;

public record ProjectDto(
        UUID id,
        String name,
        String projectKey,
        Instant createdAt
) {
}
