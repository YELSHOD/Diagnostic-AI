package com.yelshod.diagnosticserviceai.ai;

import java.util.List;

public record AiDiagnosisResponse(
        String provider,
        String model,
        String promptVersion,
        String summary,
        List<String> bullets,
        String rawText
) {
}
