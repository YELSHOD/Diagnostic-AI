package com.yelshod.diagnosticserviceai.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record AiDiagnosisRequest(
        @Size(max = 120) String service,
        @Size(max = 2_000) String question,
        List<@Size(max = 2_000) String> logLines,
        @Valid TimeRange timeRange,
        @Size(max = 32) String levelFilter,
        @Size(max = 200) String textFilter
) {
        public record TimeRange(
                @Pattern(regexp = "all|relative|custom") String mode,
                @Size(max = 120) String label,
                Instant from,
                Instant to
        ) {
        }
}
