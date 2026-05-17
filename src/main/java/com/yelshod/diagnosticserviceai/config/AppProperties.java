package com.yelshod.diagnosticserviceai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Docker docker,
        Gemini gemini,
        Runtime runtime,
        Demo demo
) {

    public record Docker(
            boolean enabled,
            String projectLabel,
            String projectLabelValue,
            int logsTail
    ) {
    }

    public record Gemini(
            String apiKey,
            String model,
            String promptVersion,
            boolean autoDiagnosisEnabled
    ) {
    }

    public record Runtime(
            List<LocalTarget> defaultLocalTargets
    ) {
    }

    public record Demo(
            boolean enabled,
            boolean autoStart,
            long stepDelayMs,
            String ordersLogFile,
            String restaurantLogFile,
            String deliveryLogFile
    ) {
    }

    public record LocalTarget(
            String name,
            String host,
            Integer port,
            String healthUrl,
            String logSourceType,
            String logSourceRef
    ) {
    }
}
