package com.yelshod.diagnosticserviceai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Docker docker,
        Gemini gemini
) {

    public record Docker(
            String projectLabel,
            String projectLabelValue,
            int logsTail
    ) {
    }

    public record Gemini(
            String apiKey,
            String model,
            String promptVersion
    ) {
    }
}
