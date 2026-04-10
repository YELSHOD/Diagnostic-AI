package com.yelshod.diagnosticserviceai.runtime;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpHealthStatusProbe implements RuntimeStatusProbe {

    private final RestClient restClient = RestClient.create();

    @Override
    public RuntimeTargetStatus probe(String healthUrl) {
        if (healthUrl == null || healthUrl.isBlank()) {
            return RuntimeTargetStatus.UNKNOWN;
        }

        try {
            HttpStatusCode statusCode = restClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            return statusCode.is2xxSuccessful() ? RuntimeTargetStatus.UP : RuntimeTargetStatus.DEGRADED;
        } catch (Exception ex) {
            return RuntimeTargetStatus.DOWN;
        }
    }
}
