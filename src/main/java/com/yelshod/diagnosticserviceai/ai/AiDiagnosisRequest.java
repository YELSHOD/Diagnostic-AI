package com.yelshod.diagnosticserviceai.ai;

import jakarta.validation.constraints.Size;
import java.util.List;

public record AiDiagnosisRequest(
        @Size(max = 120) String service,
        @Size(max = 2_000) String question,
        List<@Size(max = 2_000) String> logLines
) {
}
