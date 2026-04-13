package com.yelshod.diagnosticserviceai.ai;

import java.util.List;

public record AiDiagnosisResponse(
        String provider,
        String model,
        String promptVersion,
        String summary,
        List<String> timeline,
        String probableRootCause,
        List<String> evidence,
        List<String> nextChecks,
        String rawText
) {
}
