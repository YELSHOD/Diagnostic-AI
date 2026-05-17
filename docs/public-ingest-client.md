# Public log ingest demo client

Local Spring Boot services can send demo error logs to DiagnosticServiceAI through the public ingest endpoint.

```yaml
diagnostic:
  project-key: ${DIAGNOSTIC_PROJECT_KEY}
  server-url: ${DIAGNOSTIC_SERVER_URL:http://localhost:8080/api/public/logs}
  service-name: orders-local
  environment: local
```

```java
@Bean
DiagnosticLogClient diagnosticLogClient(
        @Value("${diagnostic.project-key}") String projectKey,
        @Value("${diagnostic.server-url}") String serverUrl,
        @Value("${diagnostic.service-name}") String serviceName,
        @Value("${diagnostic.environment:local}") String environment) {
    return new DiagnosticLogClient(projectKey, serverUrl, serviceName, environment);
}
```

```java
try {
    orderService.createOrder(command);
} catch (Exception ex) {
    diagnosticLogClient.sendError("Order creation failed orderId=" + command.orderId(), ex);
    throw ex;
}
```
