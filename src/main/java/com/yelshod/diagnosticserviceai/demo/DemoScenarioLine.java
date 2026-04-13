package com.yelshod.diagnosticserviceai.demo;

import java.time.Instant;

public record DemoScenarioLine(
        Instant timestamp,
        String level,
        String service,
        String message
) {
}
