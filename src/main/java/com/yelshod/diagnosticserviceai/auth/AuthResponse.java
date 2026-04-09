package com.yelshod.diagnosticserviceai.auth;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {

    public record UserSummary(
            UUID id,
            String email,
            String username,
            String status,
            Set<String> roles
    ) {
    }
}
