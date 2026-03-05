package com.yelshod.diagnosticserviceai.api;

import java.time.Instant;
import java.util.Map;

public record ProjectContainerDto(
        String containerId,
        String name,
        String image,
        String status,
        Instant created,
        Map<String, String> labels
) {
}
