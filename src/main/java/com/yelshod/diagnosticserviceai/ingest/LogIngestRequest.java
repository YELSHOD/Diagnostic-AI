package com.yelshod.diagnosticserviceai.ingest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record LogIngestRequest(
        @NotBlank
        @Size(max = 128)
        String projectKey,

        @NotBlank
        @Size(max = 120)
        String serviceName,

        @NotBlank
        @Pattern(regexp = "TRACE|DEBUG|INFO|WARN|ERROR")
        String level,

        @NotBlank
        @Size(max = 8_000)
        String message,

        @Size(max = 32_000)
        String stackTrace,

        Instant timestamp,

        @Size(max = 80)
        String environment
) {
}
