package com.yelshod.diagnosticserviceai.auth;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String username,
        String status,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
