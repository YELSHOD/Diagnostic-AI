package com.yelshod.diagnosticserviceai.client;

import java.time.Instant;
import org.springframework.web.client.RestClient;

/**
 * Demo helper for local Spring Boot services that want to push ERROR logs to DiagnosticServiceAI.
 *
 * Example configuration in the local service:
 * DIAGNOSTIC_PROJECT_KEY=prj_xxx
 * DIAGNOSTIC_SERVER_URL=http://localhost:8080/api/public/logs
 */
public class DiagnosticLogClient {

    private final RestClient restClient;
    private final String projectKey;
    private final String serverUrl;
    private final String serviceName;
    private final String environment;

    public DiagnosticLogClient(String projectKey, String serverUrl, String serviceName, String environment) {
        this.restClient = RestClient.create();
        this.projectKey = projectKey;
        this.serverUrl = serverUrl;
        this.serviceName = serviceName;
        this.environment = environment;
    }

    public void sendError(String message, Throwable throwable) {
        if (projectKey == null || projectKey.isBlank() || serverUrl == null || serverUrl.isBlank()) {
            return;
        }
        String stackTrace = throwable == null ? "" : stackTraceToString(throwable);
        restClient.post()
                .uri(serverUrl)
                .body(new LogIngestPayload(
                        projectKey,
                        serviceName,
                        "ERROR",
                        message,
                        stackTrace,
                        Instant.now(),
                        environment))
                .retrieve()
                .toBodilessEntity();
    }

    private String stackTraceToString(Throwable throwable) {
        StringBuilder builder = new StringBuilder(throwable.toString());
        for (StackTraceElement element : throwable.getStackTrace()) {
            builder.append('\n').append("    at ").append(element);
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            builder.append('\n').append("Caused by: ").append(cause);
            for (StackTraceElement element : cause.getStackTrace()) {
                builder.append('\n').append("    at ").append(element);
            }
        }
        return builder.toString();
    }

    private record LogIngestPayload(
            String projectKey,
            String serviceName,
            String level,
            String message,
            String stackTrace,
            Instant timestamp,
            String environment
    ) {
    }
}
