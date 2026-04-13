# Demo Business Log Scenarios Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add demo `orders-demo` and `restaurant-demo` runtime targets that stream believable business log scenarios through the existing `FILE_TAIL` pipeline, while suppressing repeated Docker socket noise in local `dev`.

**Architecture:** Extend runtime config with dedicated demo properties and seedable local targets, then add a small demo scenario subsystem that appends timed log lines to separate files and exposes a trigger endpoint. Keep Docker-noise suppression isolated to logging configuration and a small guard in Docker discovery so the observability demo stays focused on business flows.

**Tech Stack:** Spring Boot 3, Java 21, configuration properties, existing runtime target/file-tail stack, JUnit 5, Mockito, MockMvc

---

## File Map

**Create:**
- `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioProperties.java` - typed config for enablement, auto-start, delays, file paths, and variants.
- `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioType.java` - enum for supported demo scenarios.
- `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioRequest.java` - request DTO for trigger endpoint variant selection.
- `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioLine.java` - immutable description of one generated log line.
- `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioWriter.java` - appends lines to demo log files.
- `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioService.java` - orchestrates scenario start, sequencing, and concurrency guard.
- `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioController.java` - manual trigger API.
- `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioAutoStarter.java` - optional `dev` auto-start hook.
- `src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioWriterTest.java`
- `src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioServiceTest.java`
- `src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioControllerTest.java`

**Modify:**
- `src/main/java/com/yelshod/diagnosticserviceai/config/AppProperties.java` - add nested demo properties if reusing the existing `app` config root.
- `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrap.java` - ensure demo default targets seed cleanly in `dev`.
- `src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java` - reduce repeated missing-socket noise.
- `src/main/resources/application.yml` - base demo config defaults that are safe when disabled.
- `src/main/resources/application-dev.yml` - enable demo targets/paths and suppress third-party Docker retry logging.
- `src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrapTest.java` - cover demo targets in runtime defaults if needed.

---

### Task 1: Add Demo Configuration And Seeded Runtime Targets

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioProperties.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/config/AppProperties.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrapTest.java`

- [ ] **Step 1: Write the failing bootstrap test for demo targets**

```java
@Test
void seedsConfiguredDemoTargetsWhenRepositoryIsEmpty() {
    AppProperties properties = new AppProperties(
            new AppProperties.Docker("ai.project.env", "demo", 300),
            new AppProperties.Gemini("", "gemini-2.5-flash", "v1"),
            new AppProperties.Runtime(List.of(
                    new AppProperties.LocalTarget(
                            "orders-demo",
                            "localhost",
                            8080,
                            "http://localhost:8080/actuator/health",
                            "FILE_TAIL",
                            "./logs/orders-demo.log"
                    ),
                    new AppProperties.LocalTarget(
                            "restaurant-demo",
                            "localhost",
                            8080,
                            "http://localhost:8080/actuator/health",
                            "FILE_TAIL",
                            "./logs/restaurant-demo.log"
                    )
            ))
    );
    RuntimeTargetRepository repository = mock(RuntimeTargetRepository.class);
    when(repository.count()).thenReturn(0L);

    RuntimeTargetBootstrap bootstrap = new RuntimeTargetBootstrap(repository, properties);

    bootstrap.run(new DefaultApplicationArguments(new String[0]));

    ArgumentCaptor<List<RuntimeTargetEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(repository).saveAll(captor.capture());
    assertThat(captor.getValue()).extracting(RuntimeTargetEntity::getName)
            .containsExactly("orders-demo", "restaurant-demo");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetBootstrapTest`

Expected: FAIL because the bootstrap test or config shape does not yet include demo targets.

- [ ] **Step 3: Add minimal configuration support**

```java
public record AppProperties(
        Docker docker,
        Gemini gemini,
        Runtime runtime,
        Demo demo
) {
    public record Demo(
            boolean enabled,
            boolean autoStart,
            long stepDelayMs,
            String ordersLogFile,
            String restaurantLogFile
    ) {
    }
}
```

```yaml
app:
  demo:
    enabled: false
    auto-start: false
    step-delay-ms: 900
    orders-log-file: ./logs/orders-demo.log
    restaurant-log-file: ./logs/restaurant-demo.log
```

```yaml
app:
  runtime:
    default-local-targets:
      - name: diagnosticserviceai
        host: localhost
        port: 8080
        health-url: http://localhost:8080/actuator/health
        log-source-type: FILE_TAIL
        log-source-ref: ${APP_LOG_FILE:./logs/diagnosticserviceai.log}
      - name: orders-demo
        host: localhost
        port: 8080
        health-url: http://localhost:8080/actuator/health
        log-source-type: FILE_TAIL
        log-source-ref: ${ORDERS_DEMO_LOG_FILE:./logs/orders-demo.log}
      - name: restaurant-demo
        host: localhost
        port: 8080
        health-url: http://localhost:8080/actuator/health
        log-source-type: FILE_TAIL
        log-source-ref: ${RESTAURANT_DEMO_LOG_FILE:./logs/restaurant-demo.log}
  demo:
    enabled: true
    auto-start: false
    step-delay-ms: 900
    orders-log-file: ${ORDERS_DEMO_LOG_FILE:./logs/orders-demo.log}
    restaurant-log-file: ${RESTAURANT_DEMO_LOG_FILE:./logs/restaurant-demo.log}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetBootstrapTest`

Expected: PASS with seeded `orders-demo` and `restaurant-demo`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/config/AppProperties.java src/main/resources/application.yml src/main/resources/application-dev.yml src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrapTest.java
git commit -m "Add demo runtime target configuration"
```

### Task 2: Build Demo Log Writer

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioWriter.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioLine.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioWriterTest.java`

- [ ] **Step 1: Write the failing writer test**

```java
@Test
void appendsBusinessLogLinesToTheConfiguredDemoFile() throws Exception {
    Path file = Files.createTempFile("orders-demo", ".log");
    DemoScenarioWriter writer = new DemoScenarioWriter();

    writer.append(file, new DemoScenarioLine(
            Instant.parse("2026-04-13T06:30:00Z"),
            "INFO",
            "orders-demo",
            "Order created orderId=ORD-20260413-1001 customer=\"Aruzhan S.\" phone=\"+7 700 *** 12 34\""
    ));

    assertThat(Files.readString(file)).contains("Order created orderId=ORD-20260413-1001");
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioWriterTest`

Expected: FAIL because `DemoScenarioWriter` does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
public record DemoScenarioLine(
        Instant timestamp,
        String level,
        String service,
        String message
) {
}
```

```java
@Component
public class DemoScenarioWriter {

    public void append(Path path, DemoScenarioLine line) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        String formatted = "%s %-5s [%s] %s%n".formatted(
                line.timestamp(),
                line.level(),
                line.service(),
                line.message()
        );
        Files.writeString(path, formatted, StandardOpenOption.APPEND);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioWriterTest`

Expected: PASS and log file contains the business line.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioLine.java src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioWriter.java src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioWriterTest.java
git commit -m "Add demo scenario log writer"
```

### Task 3: Add Scenario Orchestration With Safe Demo Data

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioType.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioService.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioServiceTest.java`

- [ ] **Step 1: Write the failing service tests**

```java
@Test
void startsOrdersScenarioAndWritesExpectedOrderedSteps() {
    DemoScenarioWriter writer = mock(DemoScenarioWriter.class);
    DemoScenarioProperties properties = new DemoScenarioProperties(
            true,
            false,
            1L,
            "./logs/orders-demo.log",
            "./logs/restaurant-demo.log"
    );
    DemoScenarioService service = new DemoScenarioService(properties, writer, Runnable::run);

    service.start(DemoScenarioType.ORDERS_HAPPY_PATH);

    InOrder inOrder = inOrder(writer);
    inOrder.verify(writer).append(any(), argThat(line -> line.message().contains("Order created")));
    inOrder.verify(writer).append(any(), argThat(line -> line.message().contains("Payment authorized")));
    inOrder.verify(writer).append(any(), argThat(line -> line.message().contains("Restaurant accepted")));
    inOrder.verify(writer).append(any(), argThat(line -> line.message().contains("Order delivered")));
}

@Test
void rejectsConcurrentStartForTheSameScenarioRun() {
    DemoScenarioWriter writer = mock(DemoScenarioWriter.class);
    DemoScenarioProperties properties = new DemoScenarioProperties(true, false, 1L, "./logs/orders-demo.log", "./logs/restaurant-demo.log");
    Executor sameThread = command -> { };
    DemoScenarioService service = new DemoScenarioService(properties, writer, sameThread);

    service.markRunningForTest();

    assertThatThrownBy(() -> service.start(DemoScenarioType.ORDERS_HAPPY_PATH))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already running");
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioServiceTest`

Expected: FAIL because the service does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
public enum DemoScenarioType {
    ORDERS_HAPPY_PATH,
    ORDERS_PAYMENT_DELAY,
    RESTAURANT_PREP_DELAY
}
```

```java
@Service
public class DemoScenarioService {

    private final DemoScenarioProperties properties;
    private final DemoScenarioWriter writer;
    private final Executor executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void start(DemoScenarioType type) {
        if (!properties.enabled()) {
            throw new IllegalStateException("Demo scenarios are disabled");
        }
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Demo scenario is already running");
        }
        executor.execute(() -> {
            try {
                for (DemoScenarioLine line : linesFor(type)) {
                    writer.append(resolvePath(line.service()), line);
                    Thread.sleep(properties.stepDelayMs());
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to write demo scenario", ex);
            } finally {
                running.set(false);
            }
        });
    }
}
```

Use synthetic messages only, for example:

```java
List.of(
    line("orders-demo", "INFO", "Order created orderId=ORD-20260413-1001 customer=\"Aruzhan S.\" phone=\"+7 700 *** 12 34\" restaurant=\"Tokyo Bowl\" amount=8450"),
    line("orders-demo", "INFO", "Payment authorized orderId=ORD-20260413-1001 paymentId=PAY-88421 method=CARD"),
    line("restaurant-demo", "INFO", "Restaurant accepted orderId=ORD-20260413-1001 etaMinutes=24"),
    line("restaurant-demo", "INFO", "Kitchen started preparation orderId=ORD-20260413-1001 station=HOT"),
    line("orders-demo", "INFO", "Order delivered orderId=ORD-20260413-1001 courier=\"Nurlan T.\"")
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioServiceTest`

Expected: PASS with ordered writes and concurrency rejection.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioType.java src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioService.java src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioServiceTest.java
git commit -m "Add demo business scenario generator"
```

### Task 4: Expose Manual Trigger Endpoint

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioRequest.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioController.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioControllerTest.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/security/SecurityConfig.java`

- [ ] **Step 1: Write the failing controller test**

```java
@AutoConfigureMockMvc(addFilters = false)
class DemoScenarioControllerTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoScenarioService demoScenarioService;

    @Test
    void startsRequestedDemoScenario() throws Exception {
        mockMvc.perform(post("/api/demo/scenarios/orders/start")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "variant": "ORDERS_PAYMENT_DELAY"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("started"));

        verify(demoScenarioService).start(DemoScenarioType.ORDERS_PAYMENT_DELAY);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioControllerTest`

Expected: FAIL because the controller endpoint does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
public record DemoScenarioRequest(String variant) {
}
```

```java
@RestController
@RequestMapping("/api/demo/scenarios")
@RequiredArgsConstructor
public class DemoScenarioController {

    private final DemoScenarioService demoScenarioService;

    @PostMapping("/orders/start")
    ResponseEntity<Map<String, String>> startOrdersScenario(@RequestBody(required = false) DemoScenarioRequest request) {
        DemoScenarioType type = request == null || request.variant() == null
                ? DemoScenarioType.ORDERS_HAPPY_PATH
                : DemoScenarioType.valueOf(request.variant());
        demoScenarioService.start(type);
        return ResponseEntity.accepted().body(Map.of("status", "started"));
    }
}
```

If the endpoint should be usable during demos without auth friction, add it to `permitAll()` only in the same explicit block where public demo endpoints are listed.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioControllerTest`

Expected: PASS and service start invoked with the selected variant.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioRequest.java src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioController.java src/main/java/com/yelshod/diagnosticserviceai/security/SecurityConfig.java src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioControllerTest.java
git commit -m "Add demo scenario trigger endpoint"
```

### Task 5: Add Optional Auto-Start For Dev Demos

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioAutoStarter.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioServiceTest.java`

- [ ] **Step 1: Write the failing auto-start test**

```java
@Test
void autoStartsHappyPathWhenEnabledInDev() throws Exception {
    DemoScenarioService service = mock(DemoScenarioService.class);
    DemoScenarioProperties properties = new DemoScenarioProperties(true, true, 1L, "./logs/orders-demo.log", "./logs/restaurant-demo.log");

    new DemoScenarioAutoStarter(properties, service).run(new DefaultApplicationArguments(new String[0]));

    verify(service).start(DemoScenarioType.ORDERS_HAPPY_PATH);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioServiceTest`

Expected: FAIL because auto-starter does not exist.

- [ ] **Step 3: Write minimal implementation**

```java
@Component
@RequiredArgsConstructor
public class DemoScenarioAutoStarter implements ApplicationRunner {

    private final DemoScenarioProperties properties;
    private final DemoScenarioService service;

    @Override
    public void run(ApplicationArguments args) {
        if (properties.enabled() && properties.autoStart()) {
            service.start(DemoScenarioType.ORDERS_HAPPY_PATH);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioServiceTest`

Expected: PASS and auto-start only fires when enabled.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioAutoStarter.java src/test/java/com/yelshod/diagnosticserviceai/demo/DemoScenarioServiceTest.java
git commit -m "Add optional demo scenario autostart"
```

### Task 6: Suppress Docker Socket Noise For Local Demos

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java`
- Modify: `src/main/resources/application-dev.yml`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryServiceTest.java`

- [ ] **Step 1: Write the failing behavior test for repeated missing-socket handling**

```java
@Test
void returnsEmptyListOnRepeatedMissingSocketWithoutThrowing() throws Exception {
    DockerClient dockerClient = mock(DockerClient.class);
    ListContainersCmd listContainersCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
    when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
    when(listContainersCmd.exec()).thenThrow(new RuntimeException(new SocketException("No such file or directory")));

    DockerRuntimeDiscoveryService service = new DockerRuntimeDiscoveryService(dockerClient, appProperties());

    assertThat(service.discover()).isEmpty();
    assertThat(service.discover()).isEmpty();
}
```

- [ ] **Step 2: Run test to verify it fails or lacks the desired behavior**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.DockerRuntimeDiscoveryServiceTest`

Expected: either FAIL from missing repeated-case coverage or confirm current behavior still logs too loudly.

- [ ] **Step 3: Write minimal implementation**

```java
private final AtomicBoolean missingSocketLogged = new AtomicBoolean(false);

if (isMissingDockerSocket(ex)) {
    if (missingSocketLogged.compareAndSet(false, true)) {
        log.info("Docker discovery skipped because Docker socket is unavailable");
    } else {
        log.debug("Docker discovery still skipped because Docker socket is unavailable");
    }
    return List.of();
}
```

```yaml
logging:
  level:
    org.apache.hc.client5.http.impl.classic.HttpRequestRetryExec: ERROR
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.DockerRuntimeDiscoveryServiceTest`

Expected: PASS and discovery still returns an empty list on repeated missing-socket calls.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java src/main/resources/application-dev.yml src/test/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryServiceTest.java
git commit -m "Reduce docker socket noise in dev"
```

### Task 7: End-To-End Verification

**Files:**
- Modify: no code changes required unless verification uncovers defects

- [ ] **Step 1: Run targeted backend tests**

Run:

```bash
./gradlew test \
  --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetBootstrapTest \
  --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioWriterTest \
  --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioServiceTest \
  --tests com.yelshod.diagnosticserviceai.demo.DemoScenarioControllerTest \
  --tests com.yelshod.diagnosticserviceai.runtime.DockerRuntimeDiscoveryServiceTest
```

Expected: PASS.

- [ ] **Step 2: Run backend locally in dev**

Run:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Expected:
- app starts cleanly
- `orders-demo` and `restaurant-demo` appear in `/api/runtime-targets`
- no repeated Apache Docker retry spam in the app log

- [ ] **Step 3: Trigger the demo scenario**

Run:

```bash
curl -X POST http://localhost:8080/api/demo/scenarios/orders/start \
  -H 'Content-Type: application/json' \
  -d '{"variant":"ORDERS_HAPPY_PATH"}'
```

Expected: `202 Accepted` with `{"status":"started"}`.

- [ ] **Step 4: Verify log files and UI flow**

Run:

```bash
tail -n 20 logs/orders-demo.log
tail -n 20 logs/restaurant-demo.log
```

Expected:
- `orders-demo.log` contains order creation, payment, and delivery lines
- `restaurant-demo.log` contains acceptance and kitchen lines
- `Live Logs` shows these lines when either target is selected

- [ ] **Step 5: Commit verification-only fixes if needed**

```bash
git add <files changed during verification>
git commit -m "Fix demo scenario verification issues"
```

---

## Self-Review

- Spec coverage:
  - demo runtime targets: Task 1
  - real log-file writing: Task 2
  - believable business flow and masked data: Task 3
  - manual trigger endpoint: Task 4
  - optional auto-start: Task 5
  - Docker-noise suppression: Task 6
  - manual local verification: Task 7
- Placeholder scan: no `TODO`/`TBD` placeholders remain.
- Type consistency: the plan consistently uses `DemoScenarioProperties`, `DemoScenarioService`, `DemoScenarioWriter`, and `DemoScenarioType`.
