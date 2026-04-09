# Backend Testability And Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the backend into smaller testable units, add broad automated coverage across almost the entire backend, and preserve current transport behavior wherever possible.

**Architecture:** Keep a single Spring Boot service, but split large orchestration classes into focused components for log analysis, incident management, AI diagnosis, and WebSocket session handling. Cover pure logic with unit tests, persistence with PostgreSQL-backed integration tests, and API/transport with focused slice tests.

**Tech Stack:** Java 21, Spring Boot 3.5, JUnit 5, Spring Boot Test, Mockito, Flyway, PostgreSQL, Testcontainers, WebSocket test doubles

---

### Task 1: Establish Reliable Test Infrastructure

**Files:**
- Modify: `build.gradle`
- Create: `src/test/resources/application-test.yml`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/support/PostgresIntegrationTest.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/IncidentRepositoryIntegrationTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/IncidentRepositoryIntegrationTest.java`

- [ ] **Step 1: Write the failing repository characterization test**

```java
package com.yelshod.diagnosticserviceai.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.yelshod.diagnosticserviceai.persistence.entity.ClusterEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.IncidentEntity;
import com.yelshod.diagnosticserviceai.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IncidentRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Test
    void returnsGroupedAnalyticsForSelectedService() {
        ClusterEntity cluster = ClusterEntity.builder()
                .clusterKey("cluster-1")
                .service("orders")
                .title("IllegalStateException")
                .severity("high")
                .firstSeen(Instant.parse("2026-04-09T10:00:00Z"))
                .lastSeen(Instant.parse("2026-04-09T10:00:00Z"))
                .count(1)
                .build();
        clusterRepository.save(cluster);

        incidentRepository.save(IncidentEntity.builder()
                .id(UUID.randomUUID())
                .clusterKey("cluster-1")
                .service("orders")
                .eventTime(Instant.parse("2026-04-09T10:15:20Z"))
                .exceptionType("IllegalStateException")
                .message("boom")
                .topFrame("OrdersService.placeOrder")
                .stacktrace("stack")
                .context("ctx")
                .build());

        var rows = incidentRepository.errorsPerMinute(
                Instant.parse("2026-04-09T10:00:00Z"),
                Instant.parse("2026-04-09T11:00:00Z"),
                "orders");

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getCount()).isEqualTo(1L);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.persistence.repository.IncidentRepositoryIntegrationTest"`

Expected: FAIL because PostgreSQL-backed integration test support and Testcontainers dependencies are not set up yet.

- [ ] **Step 3: Write minimal implementation**

`build.gradle`

```gradle
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.boot:spring-boot-testcontainers'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

`src/test/resources/application-test.yml`

```yaml
spring:
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
```

`src/test/java/com/yelshod/diagnosticserviceai/support/PostgresIntegrationTest.java`

```java
package com.yelshod.diagnosticserviceai.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.persistence.repository.IncidentRepositoryIntegrationTest"`

Expected: PASS, proving Flyway schema and native PostgreSQL analytics query execution work in tests.

- [ ] **Step 5: Commit**

```bash
git add build.gradle src/test/resources/application-test.yml src/test/java/com/yelshod/diagnosticserviceai/support/PostgresIntegrationTest.java src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/IncidentRepositoryIntegrationTest.java
git commit -m "test: add postgres integration test foundation"
```

### Task 2: Extract Docker Frame Splitting And Characterize Core Parsing Logic

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/docker/DockerFrameLogSplitter.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/docker/DockerFrameLogSplitterTest.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/logs/LogParserTest.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/common/RedactionServiceTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/docker/DockerFrameLogSplitterTest.java`

- [ ] **Step 1: Write the failing tests**

`src/test/java/com/yelshod/diagnosticserviceai/docker/DockerFrameLogSplitterTest.java`

```java
package com.yelshod.diagnosticserviceai.docker;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class DockerFrameLogSplitterTest {

    private final DockerFrameLogSplitter splitter = new DockerFrameLogSplitter();

    @Test
    void splitsFramePayloadIntoNonBlankLogLines() {
        byte[] payload = "first line\n\nsecond line\r\n".getBytes(StandardCharsets.UTF_8);

        List<String> lines = splitter.split(payload);

        assertThat(lines).containsExactly("first line", "second line");
    }
}
```

`src/test/java/com/yelshod/diagnosticserviceai/logs/LogParserTest.java`

```java
package com.yelshod.diagnosticserviceai.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class LogParserTest {

    private final LogParser parser = new LogParser();

    @Test
    void parsesTimestampLevelAndTraceId() {
        ParsedLogLine parsed = parser.parse(
                "orders",
                "2026-04-09T10:15:30Z ERROR traceId=abc-123 failed to place order");

        assertThat(parsed.timestamp()).isEqualTo(Instant.parse("2026-04-09T10:15:30Z"));
        assertThat(parsed.level()).isEqualTo("ERROR");
        assertThat(parsed.traceId()).isEqualTo("abc-123");
        assertThat(parsed.service()).isEqualTo("orders");
    }
}
```

`src/test/java/com/yelshod/diagnosticserviceai/common/RedactionServiceTest.java`

```java
package com.yelshod.diagnosticserviceai.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedactionServiceTest {

    private final RedactionService redactionService = new RedactionService();

    @Test
    void redactsAuthorizationAndPasswordValues() {
        String redacted = redactionService.redact("Authorization: secret password=topsecret");

        assertThat(redacted).isEqualTo("Authorization: [REDACTED] password=[REDACTED]");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.docker.DockerFrameLogSplitterTest" --tests "com.yelshod.diagnosticserviceai.logs.LogParserTest" --tests "com.yelshod.diagnosticserviceai.common.RedactionServiceTest"`

Expected: FAIL because `DockerFrameLogSplitter` does not exist yet.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/yelshod/diagnosticserviceai/docker/DockerFrameLogSplitter.java`

```java
package com.yelshod.diagnosticserviceai.docker;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class DockerFrameLogSplitter {

    public List<String> split(byte[] payload) {
        String text = new String(payload, StandardCharsets.UTF_8);
        return Arrays.stream(text.split("\\R"))
                .filter(line -> !line.isBlank())
                .toList();
    }
}
```

`src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java`

```java
private final DockerFrameLogSplitter dockerFrameLogSplitter;

@Override
public void onNext(Frame frame) {
    if (frame.getStreamType() == StreamType.STDOUT || frame.getStreamType() == StreamType.STDERR) {
        for (String line : dockerFrameLogSplitter.split(frame.getPayload())) {
            consumer.accept(new DockerLogLine(service, line));
        }
    }
    super.onNext(frame);
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.docker.DockerFrameLogSplitterTest" --tests "com.yelshod.diagnosticserviceai.logs.LogParserTest" --tests "com.yelshod.diagnosticserviceai.common.RedactionServiceTest"`

Expected: PASS, confirming line splitting, parser behavior, and redaction rules are stable.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/docker/DockerFrameLogSplitter.java src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java src/test/java/com/yelshod/diagnosticserviceai/docker/DockerFrameLogSplitterTest.java src/test/java/com/yelshod/diagnosticserviceai/logs/LogParserTest.java src/test/java/com/yelshod/diagnosticserviceai/common/RedactionServiceTest.java
git commit -m "refactor: extract docker frame splitting and characterize parsing"
```

### Task 3: Make Event Assembly Deterministic And Covered

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/logs/EventAssemblyTimeoutScheduler.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/logs/EventAssembler.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/logs/EventAssemblerTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/logs/EventAssemblerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.yelshod.diagnosticserviceai.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EventAssemblerTest {

    @Test
    void completesBufferedEventWhenNextTimestampedLineStartsANewLogEntry() {
        EventAssemblyTimeoutScheduler scheduler = (service, action) -> () -> {};
        EventAssembler assembler = new EventAssembler(scheduler);
        AtomicReference<ErrorEvent> timedOut = new AtomicReference<>();

        assembler.process(new ParsedLogLine(
                Instant.parse("2026-04-09T10:00:00Z"),
                "ERROR",
                "2026-04-09T10:00:00Z ERROR java.lang.IllegalStateException: boom",
                "trace-1",
                "orders",
                "2026-04-09T10:00:00Z ERROR java.lang.IllegalStateException: boom"), timedOut::set);

        assembler.process(new ParsedLogLine(
                Instant.parse("2026-04-09T10:00:01Z"),
                null,
                "at com.example.OrdersService.placeOrder(OrdersService.java:42)",
                "trace-1",
                "orders",
                "at com.example.OrdersService.placeOrder(OrdersService.java:42)"), timedOut::set);

        Optional<ErrorEvent> completed = assembler.process(new ParsedLogLine(
                Instant.parse("2026-04-09T10:00:02Z"),
                "INFO",
                "2026-04-09T10:00:02Z INFO next message",
                "trace-2",
                "orders",
                "2026-04-09T10:00:02Z INFO next message"), timedOut::set);

        assertThat(completed).isPresent();
        assertThat(completed.get().exceptionType()).isEqualTo("IllegalStateException");
        assertThat(completed.get().stackFrames())
                .contains("at com.example.OrdersService.placeOrder(OrdersService.java:42)");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.logs.EventAssemblerTest"`

Expected: FAIL because `EventAssembler` cannot be constructed with an injectable timeout strategy yet.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/yelshod/diagnosticserviceai/logs/EventAssemblyTimeoutScheduler.java`

```java
package com.yelshod.diagnosticserviceai.logs;

public interface EventAssemblyTimeoutScheduler {

    Cancellable schedule(String service, Runnable action);

    interface Cancellable {
        void cancel();
    }
}
```

`src/main/java/com/yelshod/diagnosticserviceai/logs/EventAssembler.java`

```java
private final EventAssemblyTimeoutScheduler timeoutScheduler;

public EventAssembler(EventAssemblyTimeoutScheduler timeoutScheduler) {
    this.timeoutScheduler = timeoutScheduler;
}

private void rescheduleTimeout(State state, String service, Consumer<ErrorEvent> timeoutConsumer) {
    cancelTimeout(state);
    state.future = timeoutScheduler.schedule(service, () ->
            flushServiceInternal(service).ifPresent(timeoutConsumer));
}

private static final class State {
    private EventAssemblyTimeoutScheduler.Cancellable future;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.logs.EventAssemblerTest"`

Expected: PASS, with deterministic event assembly behavior and no scheduler-driven flakiness.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/logs/EventAssemblyTimeoutScheduler.java src/main/java/com/yelshod/diagnosticserviceai/logs/EventAssembler.java src/test/java/com/yelshod/diagnosticserviceai/logs/EventAssemblerTest.java
git commit -m "refactor: make event assembly timeout behavior testable"
```

### Task 4: Split Cluster Processing Into Focused Incident Services

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterKeyFactory.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/cluster/IncidentRecorder.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterLifecycleService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterLifecycleResult.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/cluster/DiagnosisTrigger.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterService.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/cluster/ClusterKeyFactoryTest.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/cluster/ClusterServiceTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/cluster/ClusterServiceTest.java`

- [ ] **Step 1: Write the failing tests**

`src/test/java/com/yelshod/diagnosticserviceai/cluster/ClusterKeyFactoryTest.java`

```java
package com.yelshod.diagnosticserviceai.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ClusterKeyFactoryTest {

    private final ClusterKeyFactory clusterKeyFactory = new ClusterKeyFactory();

    @Test
    void normalizesVolatileValuesBeforeHashingMessage() {
        String keyA = clusterKeyFactory.build(
                "IllegalStateException",
                "OrdersService.placeOrder",
                "Order 123 failed for request 550e8400-e29b-41d4-a716-446655440000");
        String keyB = clusterKeyFactory.build(
                "IllegalStateException",
                "OrdersService.placeOrder",
                "Order 456 failed for request 123e4567-e89b-12d3-a456-426614174000");

        assertThat(keyA).isEqualTo(keyB);
    }
}
```

`src/test/java/com/yelshod/diagnosticserviceai/cluster/ClusterServiceTest.java`

```java
package com.yelshod.diagnosticserviceai.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

    @Mock
    private ClusterKeyFactory clusterKeyFactory;

    @Mock
    private ClusterLifecycleService clusterLifecycleService;

    @Mock
    private IncidentRecorder incidentRecorder;

    @Mock
    private DiagnosisTrigger diagnosisTrigger;

    @InjectMocks
    private ClusterService clusterService;

    @Test
    void recordsIncidentAndSkipsDiagnosisWhenClusterAlreadyExists() {
        ErrorEvent event = new ErrorEvent(
                "orders",
                Instant.parse("2026-04-09T10:00:00Z"),
                "trace-1",
                "IllegalStateException",
                "boom",
                List.of("OrdersService.placeOrder"),
                "stack",
                List.of("ctx"));

        when(clusterKeyFactory.build("IllegalStateException", "OrdersService.placeOrder", "boom"))
                .thenReturn("cluster-1");
        when(clusterLifecycleService.upsert("cluster-1", event)).thenReturn(new ClusterLifecycleResult("cluster-1", "orders", 2L, false));

        clusterService.processEvent(event);

        verify(incidentRecorder).record("cluster-1", event);
        verify(diagnosisTrigger, never()).diagnoseNewCluster(any(), any());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.cluster.ClusterKeyFactoryTest" --tests "com.yelshod.diagnosticserviceai.cluster.ClusterServiceTest"`

Expected: FAIL because the extracted components and constructor-injected `ClusterService` do not exist yet.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterKeyFactory.java`

```java
package com.yelshod.diagnosticserviceai.cluster;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class ClusterKeyFactory {

    public String build(String exceptionType, String topFrame, String message) {
        return exceptionType + "|" + topFrame + "|" + sha256(normalize(message));
    }

    private String normalize(String message) {
        if (message == null) {
            return "";
        }
        return message.toLowerCase()
                .replaceAll("[0-9]+", "#")
                .replaceAll("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "{uuid}")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
```

`src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterService.java`

```java
@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterKeyFactory clusterKeyFactory;
    private final ClusterLifecycleService clusterLifecycleService;
    private final IncidentRecorder incidentRecorder;
    private final DiagnosisTrigger diagnosisTrigger;

    @Transactional
    public ClusterResult processEvent(ErrorEvent event) {
        String topFrame = event.stackFrames().isEmpty() ? "no-frame" : event.stackFrames().getFirst();
        String clusterKey = clusterKeyFactory.build(event.exceptionType(), topFrame, event.message());
        ClusterLifecycleResult lifecycle = clusterLifecycleService.upsert(clusterKey, event);
        incidentRecorder.record(clusterKey, event);
        if (lifecycle.newCluster()) {
            diagnosisTrigger.diagnoseNewCluster(clusterKey, event);
        }
        return new ClusterResult(lifecycle.clusterKey(), lifecycle.service(), lifecycle.count(), lifecycle.newCluster());
    }
}
```

`src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterLifecycleResult.java`

```java
package com.yelshod.diagnosticserviceai.cluster;

public record ClusterLifecycleResult(
        String clusterKey,
        String service,
        long count,
        boolean newCluster
) {
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.cluster.ClusterKeyFactoryTest" --tests "com.yelshod.diagnosticserviceai.cluster.ClusterServiceTest"`

Expected: PASS, proving cluster-key normalization and cluster orchestration are independently testable.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterKeyFactory.java src/main/java/com/yelshod/diagnosticserviceai/cluster/IncidentRecorder.java src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterLifecycleService.java src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterLifecycleResult.java src/main/java/com/yelshod/diagnosticserviceai/cluster/DiagnosisTrigger.java src/main/java/com/yelshod/diagnosticserviceai/cluster/ClusterService.java src/test/java/com/yelshod/diagnosticserviceai/cluster/ClusterKeyFactoryTest.java src/test/java/com/yelshod/diagnosticserviceai/cluster/ClusterServiceTest.java
git commit -m "refactor: split cluster processing responsibilities"
```

### Task 5: Separate AI Prompting, Transport, And Persistence

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactory.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/GeminiClient.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/HttpGeminiClient.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisPersistenceService.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisService.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactoryTest.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisServiceTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisServiceTest.java`

- [ ] **Step 1: Write the failing tests**

`src/test/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactoryTest.java`

```java
package com.yelshod.diagnosticserviceai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.yelshod.diagnosticserviceai.common.RedactionService;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosisPromptFactoryTest {

    @Test
    void buildsPromptPayloadWithRedactedSensitiveFields() {
        DiagnosisPromptFactory factory = new DiagnosisPromptFactory(new RedactionService());
        ErrorEvent event = new ErrorEvent(
                "orders",
                Instant.parse("2026-04-09T10:00:00Z"),
                "trace-1",
                "IllegalStateException",
                "Authorization: secret",
                List.of("OrdersService.placeOrder"),
                "stack",
                List.of("password=topsecret"));

        String prompt = factory.buildInputJson(event);

        assertThat(prompt).contains("[REDACTED]");
        assertThat(prompt).doesNotContain("secret");
        assertThat(prompt).doesNotContain("topsecret");
    }
}
```

`src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisServiceTest.java`

```java
package com.yelshod.diagnosticserviceai.ai;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiDiagnosisServiceTest {

    @Mock
    private DiagnosisPromptFactory diagnosisPromptFactory;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private AiDiagnosisPersistenceService aiDiagnosisPersistenceService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AiDiagnosisService aiDiagnosisService;

    @Test
    void skipsRemoteCallWhenApiKeyIsBlank() {
        when(appProperties.gemini()).thenReturn(new AppProperties.Gemini("", "gemini-2.5-flash", "v1"));

        aiDiagnosisService.diagnoseNewCluster("cluster-1", new ErrorEvent(
                "orders", Instant.now(), "trace", "IllegalStateException", "boom", List.of(), "stack", List.of()));

        verify(geminiClient, never()).generateDiagnosisJson("gemini-2.5-flash", "");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.ai.DiagnosisPromptFactoryTest" --tests "com.yelshod.diagnosticserviceai.ai.AiDiagnosisServiceTest"`

Expected: FAIL because the extracted prompt/client/persistence components do not exist yet.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/yelshod/diagnosticserviceai/ai/GeminiClient.java`

```java
package com.yelshod.diagnosticserviceai.ai;

public interface GeminiClient {

    String generateDiagnosisJson(String model, String prompt);
}
```

`src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisService.java`

```java
@Service
@RequiredArgsConstructor
public class AiDiagnosisService {

    private final DiagnosisPromptFactory diagnosisPromptFactory;
    private final GeminiClient geminiClient;
    private final AiDiagnosisPersistenceService aiDiagnosisPersistenceService;
    private final AppProperties appProperties;

    public void diagnoseNewCluster(String clusterKey, ErrorEvent event) {
        AppProperties.Gemini gemini = appProperties.gemini();
        if (gemini.apiKey() == null || gemini.apiKey().isBlank()) {
            return;
        }

        String prompt = diagnosisPromptFactory.buildInputJson(event);
        String diagnosisJson = geminiClient.generateDiagnosisJson(gemini.model(), prompt);
        aiDiagnosisPersistenceService.save(clusterKey, gemini.model(), gemini.promptVersion(), diagnosisJson);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.ai.DiagnosisPromptFactoryTest" --tests "com.yelshod.diagnosticserviceai.ai.AiDiagnosisServiceTest"`

Expected: PASS, proving AI workflow decisions are testable without live network access.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactory.java src/main/java/com/yelshod/diagnosticserviceai/ai/GeminiClient.java src/main/java/com/yelshod/diagnosticserviceai/ai/HttpGeminiClient.java src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisPersistenceService.java src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisService.java src/test/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactoryTest.java src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisServiceTest.java
git commit -m "refactor: isolate ai prompt transport and persistence"
```

### Task 6: Move WebSocket Flow Coordination Into A Session Service

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ws/WsMessageSender.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandler.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionServiceTest.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandlerTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandlerTest.java`

- [ ] **Step 1: Write the failing tests**

`src/test/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandlerTest.java`

```java
package com.yelshod.diagnosticserviceai.ws;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class LogsWebSocketHandlerTest {

    @Mock
    private LogStreamSessionService logStreamSessionService;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private LogsWebSocketHandler logsWebSocketHandler;

    @Test
    void delegatesValidSubscriptionToSessionService() {
        org.mockito.Mockito.when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/logs?containerId=abc123"));
        org.mockito.Mockito.when(session.getId()).thenReturn("session-1");

        logsWebSocketHandler.afterConnectionEstablished(session);

        verify(logStreamSessionService).open(eq("session-1"), eq("abc123"), eq(session));
    }
}
```

`src/test/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionServiceTest.java`

```java
package com.yelshod.diagnosticserviceai.ws;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.docker.DockerLogLine;
import com.yelshod.diagnosticserviceai.docker.DockerLogSession;
import com.yelshod.diagnosticserviceai.docker.DockerLogsService;
import com.yelshod.diagnosticserviceai.logs.LogProcessingService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class LogStreamSessionServiceTest {

    @Mock
    private DockerLogsService dockerLogsService;

    @Mock
    private LogProcessingService logProcessingService;

    @Mock
    private WsMessageSender wsMessageSender;

    @Mock
    private WebSocketSession session;

    @Test
    void sendsTransformedLogMessageForIncomingDockerLine() {
        DockerLogSession dockerLogSession = () -> {};
        when(dockerLogsService.streamLogs(org.mockito.ArgumentMatchers.eq("container-1"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(dockerLogSession);
        when(logProcessingService.toLogMessage(new DockerLogLine("orders", "hello")))
                .thenReturn(new WsMessage("LOG_LINE", Instant.parse("2026-04-09T10:00:00Z"), "orders", Map.of("message", "hello")));

        LogStreamSessionService service = new LogStreamSessionService(dockerLogsService, logProcessingService, wsMessageSender);
        service.open("session-1", "container-1", session);

        ArgumentCaptor<java.util.function.Consumer<DockerLogLine>> captor = ArgumentCaptor.forClass(java.util.function.Consumer.class);
        verify(dockerLogsService).streamLogs(org.mockito.ArgumentMatchers.eq("container-1"), captor.capture());
        captor.getValue().accept(new DockerLogLine("orders", "hello"));

        verify(wsMessageSender).send(session, new WsMessage("LOG_LINE", Instant.parse("2026-04-09T10:00:00Z"), "orders", Map.of("message", "hello")));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.ws.LogsWebSocketHandlerTest" --tests "com.yelshod.diagnosticserviceai.ws.LogStreamSessionServiceTest"`

Expected: FAIL because `LogStreamSessionService` and `WsMessageSender` do not exist yet and the handler still owns orchestration.

- [ ] **Step 3: Write minimal implementation**

`src/main/java/com/yelshod/diagnosticserviceai/ws/WsMessageSender.java`

```java
package com.yelshod.diagnosticserviceai.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsMessageSender {

    private final ObjectMapper objectMapper;

    public void send(WebSocketSession session, WsMessage message) {
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            }
        } catch (IOException ex) {
            log.warn("Unable to send ws message", ex);
        }
    }
}
```

`src/main/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandler.java`

```java
@Component
@RequiredArgsConstructor
public class LogsWebSocketHandler extends TextWebSocketHandler {

    private final LogStreamSessionService logStreamSessionService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String containerId = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("containerId");
        if (containerId == null || containerId.isBlank()) {
            closeQuietly(session, CloseStatus.BAD_DATA);
            return;
        }
        logStreamSessionService.open(session.getId(), containerId, session);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.ws.LogsWebSocketHandlerTest" --tests "com.yelshod.diagnosticserviceai.ws.LogStreamSessionServiceTest"`

Expected: PASS, proving transport code is now thin and flow orchestration is isolated.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java src/main/java/com/yelshod/diagnosticserviceai/ws/WsMessageSender.java src/main/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandler.java src/test/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionServiceTest.java src/test/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandlerTest.java
git commit -m "refactor: extract websocket session orchestration"
```

### Task 7: Lock Down Controllers And Repository Aggregates

**Files:**
- Create: `src/test/java/com/yelshod/diagnosticserviceai/api/AnalyticsControllerTest.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/api/ProjectControllerTest.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/ClusterRepositoryIntegrationTest.java`
- Create: `src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/AiDiagnosisRepositoryIntegrationTest.java`
- Modify: `src/test/java/com/yelshod/diagnosticserviceai/DiagnosticServiceAiApplicationTests.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/api/AnalyticsControllerTest.java`

- [ ] **Step 1: Write the failing tests**

`src/test/java/com/yelshod/diagnosticserviceai/api/AnalyticsControllerTest.java`

```java
package com.yelshod.diagnosticserviceai.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yelshod.diagnosticserviceai.analytics.AnalyticsResponse;
import com.yelshod.diagnosticserviceai.analytics.AnalyticsService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void returnsAnalyticsPayloadForExplicitWindow() throws Exception {
        when(analyticsService.getAnalytics(
                Instant.parse("2026-04-09T10:00:00Z"),
                Instant.parse("2026-04-09T11:00:00Z"),
                "orders"))
                .thenReturn(new AnalyticsResponse(List.of(), List.of(), List.of()));

        mockMvc.perform(get("/api/analytics")
                        .param("from", "2026-04-09T10:00:00Z")
                        .param("to", "2026-04-09T11:00:00Z")
                        .param("service", "orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorsPerMinute").isArray());

        verify(analyticsService).getAnalytics(
                eq(Instant.parse("2026-04-09T10:00:00Z")),
                eq(Instant.parse("2026-04-09T11:00:00Z")),
                eq("orders"));
    }
}
```

`src/test/java/com/yelshod/diagnosticserviceai/api/ProjectControllerTest.java`

```java
package com.yelshod.diagnosticserviceai.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yelshod.diagnosticserviceai.docker.DockerContainerService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DockerContainerService dockerContainerService;

    @Test
    void returnsVisibleProjectContainers() throws Exception {
        when(dockerContainerService.listDemoProjectContainers()).thenReturn(List.of(
                new ProjectContainerDto("id-1", "orders", "orders:latest", "Up", Instant.parse("2026-04-09T10:00:00Z"), Map.of())));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("orders"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.api.AnalyticsControllerTest" --tests "com.yelshod.diagnosticserviceai.api.ProjectControllerTest"`

Expected: FAIL if controller serialization or Spring MVC test setup is not yet aligned with current API contract.

- [ ] **Step 3: Write minimal implementation**

`src/test/java/com/yelshod/diagnosticserviceai/DiagnosticServiceAiApplicationTests.java`

```java
package com.yelshod.diagnosticserviceai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DiagnosticServiceAiApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

`src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/AiDiagnosisRepositoryIntegrationTest.java`

```java
package com.yelshod.diagnosticserviceai.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.yelshod.diagnosticserviceai.persistence.entity.AiDiagnosisEntity;
import com.yelshod.diagnosticserviceai.support.PostgresIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AiDiagnosisRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private AiDiagnosisRepository aiDiagnosisRepository;

    @Test
    void persistsDiagnosisJsonByClusterKey() {
        aiDiagnosisRepository.save(AiDiagnosisEntity.builder()
                .clusterKey("cluster-1")
                .model("gemini-2.5-flash")
                .promptVersion("v1")
                .diagnosisJson("{\"severity\":\"high\"}")
                .createdAt(Instant.parse("2026-04-09T10:00:00Z"))
                .build());

        assertThat(aiDiagnosisRepository.findById("cluster-1")).isPresent();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.yelshod.diagnosticserviceai.api.AnalyticsControllerTest" --tests "com.yelshod.diagnosticserviceai.api.ProjectControllerTest" --tests "com.yelshod.diagnosticserviceai.persistence.repository.ClusterRepositoryIntegrationTest" --tests "com.yelshod.diagnosticserviceai.persistence.repository.AiDiagnosisRepositoryIntegrationTest"`

Expected: PASS, locking down controller contracts and repository behavior.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/yelshod/diagnosticserviceai/api/AnalyticsControllerTest.java src/test/java/com/yelshod/diagnosticserviceai/api/ProjectControllerTest.java src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/ClusterRepositoryIntegrationTest.java src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/AiDiagnosisRepositoryIntegrationTest.java src/test/java/com/yelshod/diagnosticserviceai/DiagnosticServiceAiApplicationTests.java
git commit -m "test: lock down controllers and repository aggregates"
```

### Task 8: Final Verification And Forward Plan

**Files:**
- Create: `docs/superpowers/specs/2026-04-09-backend-next-wave-notes.md`
- Test: `build/reports/tests/test/index.html`

- [ ] **Step 1: Write the failing documentation expectation**

Create a short checklist of what the final note must contain:

```markdown
# Backend Next Wave Notes

- What is now covered by tests
- Which classes were split and why
- Which risks remain
- Where implementation stopped
- What the next wave should implement next
```

- [ ] **Step 2: Run full verification to surface remaining failures**

Run: `./gradlew test`

Expected: FAIL until all prior tasks are complete and all new tests are green.

- [ ] **Step 3: Write minimal implementation**

`docs/superpowers/specs/2026-04-09-backend-next-wave-notes.md`

```markdown
# Backend Next Wave Notes

## Stabilized In This Wave

- Log parsing, redaction, frame splitting, event assembly, cluster orchestration, AI workflow, controllers, and repositories are covered by automated tests.
- WebSocket delivery now delegates orchestration to a dedicated session service.
- AI and Docker concerns are isolated behind narrower interfaces.

## Current Stopping Point

- Core backend behavior is covered and internally decomposed.
- External REST and WebSocket contracts are preserved.
- Remaining work is mostly second-wave hardening rather than first-wave stabilization.

## Remaining Risks

- No real Docker end-to-end test coverage yet.
- No real Gemini integration test coverage yet.
- Timeout behavior still needs operational observation after deployment.

## Next Wave

1. Add metrics around log throughput, cluster creation, and AI failures.
2. Add operational limits for noisy streams and large stack traces.
3. Harden validation and explicit error responses on public endpoints.
4. Consider contract snapshots for WebSocket payload schemas.
```

- [ ] **Step 4: Run full verification to verify it passes**

Run: `./gradlew test`

Expected: PASS with all tests green and no newly introduced failures.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-04-09-backend-next-wave-notes.md
git commit -m "docs: add backend next wave notes"
```

## Self-Review

### Spec coverage

- Architecture decomposition from the design is covered by Tasks 2 through 6.
- Broad test coverage is covered by Tasks 1 through 7.
- Forward visibility on where implementation stopped and what comes next is covered by Task 8.
- Preserving external REST/WebSocket behavior is covered by Tasks 6 and 7.

### Placeholder scan

- No unfinished placeholders remain.
- Each task includes exact files, test commands, expected failures, minimal implementation sketches, and commit steps.

### Type consistency

- `ClusterKeyFactory`, `ClusterLifecycleService`, `IncidentRecorder`, and `DiagnosisTrigger` are referenced consistently in both tests and implementation steps.
- `GeminiClient` and `AiDiagnosisPersistenceService` are referenced consistently in Task 5.
- `LogStreamSessionService` and `WsMessageSender` are referenced consistently in Task 6.
