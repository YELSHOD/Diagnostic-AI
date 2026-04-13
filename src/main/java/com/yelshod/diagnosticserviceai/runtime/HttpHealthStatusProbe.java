package com.yelshod.diagnosticserviceai.runtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class HttpHealthStatusProbe implements RuntimeStatusProbe {

    private final RestClient restClient = RestClient.create();

    @Override
    public RuntimeTargetStatus probe(String healthUrl) {
        if (healthUrl == null || healthUrl.isBlank()) {
            log.debug("Runtime target health probe skipped reason=missing-health-url");
            return RuntimeTargetStatus.UNKNOWN;
        }

        try {
            HttpStatusCode statusCode = restClient.get()
                    .uri(healthUrl)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();
            log.debug("Runtime target health probe completed healthUrl={} status={}", healthUrl, statusCode.value());
            return statusCode.is2xxSuccessful() ? RuntimeTargetStatus.UP : RuntimeTargetStatus.DEGRADED;
        } catch (Exception ex) {
            log.warn("Runtime target health probe failed healthUrl={}", healthUrl, ex);
            return RuntimeTargetStatus.DOWN;
        }
    }
}
