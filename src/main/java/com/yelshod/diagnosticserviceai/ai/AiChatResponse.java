package com.yelshod.diagnosticserviceai.ai;

import java.util.List;

public record AiChatResponse(
        String provider,
        String model,
        String promptVersion,
        String answer,
        List<String> suggestions,
        List<String> relatedPages,
        String rawText
) {
}
