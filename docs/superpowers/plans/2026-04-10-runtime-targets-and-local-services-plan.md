# Runtime Targets And Local Services Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce a unified runtime-target read path that lists Docker containers and persisted local services through `GET /api/runtime-targets`, while keeping the current Docker endpoint working during transition.

**Architecture:** Add a source-agnostic `runtime` package with DTOs, discovery providers, status probing, and an orchestration service. Persist `LOCAL_SERVICE` targets in PostgreSQL via Flyway/JPA, then have the API compose Docker-discovered and database-backed targets into one response without pushing source-specific logic into controllers.

**Tech Stack:** Spring Boot 3.5, Spring MVC, Spring Data JPA, Flyway, PostgreSQL, JUnit 5, Mockito, MockMvc

---

### Task 1: Runtime Target Read Model And API

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetDto.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetType.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetStatus.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/LogSourceType.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetController.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetDiscoveryService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/api/ProjectController.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetControllerTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetServiceTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryServiceTest.java`
- Modify: `src/test/java/com/yelshod/diagnosticserviceai/api/ProjectControllerTest.java`

- [ ] **Step 1: Write the failing controller test for the new endpoint**

```java
@WebMvcTest(RuntimeTargetController.class)
@AutoConfigureMockMvc(addFilters = false)
class RuntimeTargetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RuntimeTargetService runtimeTargetService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void returnsUnifiedRuntimeTargets() throws Exception {
        when(runtimeTargetService.listRuntimeTargets()).thenReturn(List.of(
                new RuntimeTargetDto(
                        "docker-orders",
                        "orders",
                        RuntimeTargetType.DOCKER_CONTAINER,
                        RuntimeTargetStatus.UP,
                        "localhost",
                        8081,
                        "http://localhost:8081/actuator/health",
                        LogSourceType.DOCKER,
                        "docker-orders",
                        Map.of("image", "orders:latest"))));

        mockMvc.perform(get("/api/runtime-targets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("DOCKER_CONTAINER"))
                .andExpect(jsonPath("$[0].status").value("UP"))
                .andExpect(jsonPath("$[0].logSourceType").value("DOCKER"));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails because the controller and DTOs do not exist**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetControllerTest`
Expected: FAIL with missing `RuntimeTargetController`, `RuntimeTargetService`, or runtime target types.

- [ ] **Step 3: Write the failing orchestration test**

```java
class RuntimeTargetServiceTest {

    @Test
    void mergesTargetsFromAllDiscoveryProvidersInStableOrder() {
        RuntimeTargetDiscoveryService dockerDiscovery = () -> List.of(
                new RuntimeTargetDto("docker-orders", "orders", RuntimeTargetType.DOCKER_CONTAINER,
                        RuntimeTargetStatus.UP, "localhost", 8081, null, LogSourceType.DOCKER, "docker-orders",
                        Map.of("source", "docker")));
        RuntimeTargetDiscoveryService localDiscovery = () -> List.of(
                new RuntimeTargetDto("local-api", "api", RuntimeTargetType.LOCAL_SERVICE,
                        RuntimeTargetStatus.UNKNOWN, "127.0.0.1", 8080, null, LogSourceType.FILE_TAIL,
                        "/tmp/api.log", Map.of("source", "db")));

        RuntimeTargetService service = new RuntimeTargetService(List.of(localDiscovery, dockerDiscovery));

        assertThat(service.listRuntimeTargets())
                .extracting(RuntimeTargetDto::id)
                .containsExactly("local-api", "docker-orders");
    }
}
```

- [ ] **Step 4: Run the service test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetServiceTest`
Expected: FAIL with missing `RuntimeTargetService` or `RuntimeTargetDiscoveryService`.

- [ ] **Step 5: Write the failing Docker discovery mapping test**

```java
@Test
void mapsDockerContainersIntoRuntimeTargets() {
    DockerClient dockerClient = mock(DockerClient.class);
    ListContainersCmd listContainersCmd = mock(ListContainersCmd.class);
    when(dockerClient.listContainersCmd()).thenReturn(listContainersCmd);
    when(listContainersCmd.withShowAll(true)).thenReturn(listContainersCmd);
    when(listContainersCmd.exec()).thenReturn(List.of(container()));

    var discovery = new DockerRuntimeDiscoveryService(dockerClient, appProperties());

    assertThat(discovery.discover())
            .singleElement()
            .satisfies(target -> {
                assertThat(target.type()).isEqualTo(RuntimeTargetType.DOCKER_CONTAINER);
                assertThat(target.status()).isEqualTo(RuntimeTargetStatus.UP);
                assertThat(target.logSourceType()).isEqualTo(LogSourceType.DOCKER);
            });
}
```

- [ ] **Step 6: Run the Docker discovery test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.DockerRuntimeDiscoveryServiceTest`
Expected: FAIL with missing `DockerRuntimeDiscoveryService` or runtime target DTO types.

- [ ] **Step 7: Implement the minimal runtime-target DTOs, controller, service, and Docker discovery adapter**

```java
public record RuntimeTargetDto(
        String id,
        String name,
        RuntimeTargetType type,
        RuntimeTargetStatus status,
        String host,
        Integer port,
        String healthUrl,
        LogSourceType logSourceType,
        String logSourceRef,
        Map<String, String> metadata
) {
}
```

```java
public interface RuntimeTargetDiscoveryService {
    List<RuntimeTargetDto> discover();
}
```

```java
@Service
@RequiredArgsConstructor
public class RuntimeTargetService {

    private final List<RuntimeTargetDiscoveryService> discoveryServices;

    public List<RuntimeTargetDto> listRuntimeTargets() {
        return discoveryServices.stream()
                .flatMap(service -> service.discover().stream())
                .sorted(Comparator.comparing(RuntimeTargetDto::type).thenComparing(RuntimeTargetDto::name))
                .toList();
    }
}
```

```java
@RestController
@RequestMapping("/api/runtime-targets")
@RequiredArgsConstructor
public class RuntimeTargetController {

    private final RuntimeTargetService runtimeTargetService;

    @GetMapping
    public List<RuntimeTargetDto> runtimeTargets() {
        return runtimeTargetService.listRuntimeTargets();
    }
}
```

- [ ] **Step 8: Keep `/api/projects` working by mapping runtime targets back to the legacy Docker-only shape**

```java
@GetMapping
public List<ProjectContainerDto> projects() {
    return runtimeTargetService.listRuntimeTargets().stream()
            .filter(target -> target.type() == RuntimeTargetType.DOCKER_CONTAINER)
            .map(target -> new ProjectContainerDto(
                    target.id(),
                    target.name(),
                    target.metadata().getOrDefault("image", "unknown"),
                    target.metadata().getOrDefault("dockerStatus", target.status().name()),
                    Instant.parse(target.metadata().get("createdAt")),
                    target.metadata()))
            .toList();
}
```

- [ ] **Step 9: Run the targeted API and runtime tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetControllerTest --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetServiceTest --tests com.yelshod.diagnosticserviceai.runtime.DockerRuntimeDiscoveryServiceTest --tests com.yelshod.diagnosticserviceai.api.ProjectControllerTest`
Expected: PASS

- [ ] **Step 10: Commit the phase 1 API foundation**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/runtime src/main/java/com/yelshod/diagnosticserviceai/api/ProjectController.java src/test/java/com/yelshod/diagnosticserviceai/runtime src/test/java/com/yelshod/diagnosticserviceai/api/ProjectControllerTest.java
git commit -m "feat: add runtime target read api"
```

### Task 2: Persisted Local Service Targets

**Files:**
- Create: `src/main/resources/db/migration/V3__runtime_targets.sql`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/persistence/entity/RuntimeTargetEntity.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/persistence/repository/RuntimeTargetRepository.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryService.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/RuntimeTargetRepositoryIntegrationTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryServiceTest.java`

- [ ] **Step 1: Write the failing repository integration test**

```java
class RuntimeTargetRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private RuntimeTargetRepository runtimeTargetRepository;

    @Test
    void persistsLocalServiceTargets() {
        RuntimeTargetEntity entity = RuntimeTargetEntity.builder()
                .id(UUID.fromString("20000000-0000-0000-0000-000000000001"))
                .name("orders-api")
                .type(RuntimeTargetType.LOCAL_SERVICE)
                .host("127.0.0.1")
                .port(8081)
                .healthUrl("http://127.0.0.1:8081/actuator/health")
                .logSourceType(LogSourceType.FILE_TAIL)
                .logSourceRef("/tmp/orders.log")
                .enabled(true)
                .build();

        RuntimeTargetEntity saved = runtimeTargetRepository.save(entity);

        assertThat(runtimeTargetRepository.findById(saved.getId())).isPresent();
    }
}
```

- [ ] **Step 2: Run the integration test to verify Flyway/schema support is missing**

Run: `TEST_DB_URL=jdbc:postgresql://localhost:5433/postgres TEST_DB_USER=postgres TEST_DB_PASSWORD=postgres ./gradlew test --tests com.yelshod.diagnosticserviceai.persistence.repository.RuntimeTargetRepositoryIntegrationTest`
Expected: FAIL with missing `runtime_targets` table or missing repository/entity types.

- [ ] **Step 3: Write the failing local discovery unit test**

```java
@Test
void returnsOnlyEnabledLocalServiceTargets() {
    RuntimeTargetRepository repository = mock(RuntimeTargetRepository.class);
    when(repository.findByEnabledTrueOrderByNameAsc()).thenReturn(List.of(enabledEntity(), disabledEntity()));

    var discovery = new ConfiguredLocalServiceDiscoveryService(repository, statusProbe);

    assertThat(discovery.discover())
            .extracting(RuntimeTargetDto::name)
            .containsExactly("orders-api");
}
```

- [ ] **Step 4: Run the unit test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.ConfiguredLocalServiceDiscoveryServiceTest`
Expected: FAIL with missing repository/discovery types.

- [ ] **Step 5: Implement the migration, entity, repository, and database-backed discovery**

```sql
CREATE TABLE runtime_targets (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(64) NOT NULL,
    host VARCHAR(255),
    port INTEGER,
    health_url VARCHAR(1024),
    log_source_type VARCHAR(64) NOT NULL,
    log_source_ref VARCHAR(2048),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_runtime_targets_enabled ON runtime_targets(enabled);
CREATE INDEX idx_runtime_targets_type ON runtime_targets(type);
```

```java
public interface RuntimeTargetRepository extends JpaRepository<RuntimeTargetEntity, UUID> {
    List<RuntimeTargetEntity> findByEnabledTrueOrderByNameAsc();
}
```

- [ ] **Step 6: Run repository and discovery tests to verify they pass**

Run: `TEST_DB_URL=jdbc:postgresql://localhost:5433/postgres TEST_DB_USER=postgres TEST_DB_PASSWORD=postgres ./gradlew test --tests com.yelshod.diagnosticserviceai.persistence.repository.RuntimeTargetRepositoryIntegrationTest --tests com.yelshod.diagnosticserviceai.runtime.ConfiguredLocalServiceDiscoveryServiceTest`
Expected: PASS

- [ ] **Step 7: Commit database-backed local service targets**

```bash
git add src/main/resources/db/migration/V3__runtime_targets.sql src/main/java/com/yelshod/diagnosticserviceai/persistence/entity/RuntimeTargetEntity.java src/main/java/com/yelshod/diagnosticserviceai/persistence/repository/RuntimeTargetRepository.java src/main/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryService.java src/test/java/com/yelshod/diagnosticserviceai/persistence/repository/RuntimeTargetRepositoryIntegrationTest.java src/test/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryServiceTest.java
git commit -m "feat: persist local runtime targets"
```

### Task 3: Health-Based Runtime Status Probing

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeStatusProbe.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryService.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbeTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryServiceTest.java`

- [ ] **Step 1: Write the failing probe tests**

```java
@Test
void returnsUpWhenHealthEndpointRespondsWith2xx() {
    MockRestServiceServer server = MockRestServiceServer.bindTo(restClient).build();
    server.expect(requestTo("http://127.0.0.1:8081/actuator/health"))
            .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

    assertThat(probe.probe("http://127.0.0.1:8081/actuator/health"))
            .isEqualTo(RuntimeTargetStatus.UP);
}
```

- [ ] **Step 2: Run the probe test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.HttpHealthStatusProbeTest`
Expected: FAIL with missing `HttpHealthStatusProbe` or `RuntimeStatusProbe`.

- [ ] **Step 3: Extend the local discovery test to expect probed statuses**

```java
when(statusProbe.probe("http://127.0.0.1:8081/actuator/health")).thenReturn(RuntimeTargetStatus.UP);

assertThat(discovery.discover())
        .singleElement()
        .extracting(RuntimeTargetDto::status)
        .isEqualTo(RuntimeTargetStatus.UP);
```

- [ ] **Step 4: Run the updated discovery test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.ConfiguredLocalServiceDiscoveryServiceTest`
Expected: FAIL because discovery still returns `UNKNOWN` or does not call the probe.

- [ ] **Step 5: Implement the probe and wire it into local discovery**

```java
public interface RuntimeStatusProbe {
    RuntimeTargetStatus probe(String healthUrl);
}
```

```java
@Component
@RequiredArgsConstructor
public class HttpHealthStatusProbe implements RuntimeStatusProbe {

    private final RestClient restClient;

    @Override
    public RuntimeTargetStatus probe(String healthUrl) {
        if (healthUrl == null || healthUrl.isBlank()) {
            return RuntimeTargetStatus.UNKNOWN;
        }
        try {
            restClient.get().uri(healthUrl).retrieve().toBodilessEntity();
            return RuntimeTargetStatus.UP;
        } catch (RestClientResponseException ex) {
            return ex.getStatusCode().is5xxServerError() ? RuntimeTargetStatus.DEGRADED : RuntimeTargetStatus.DOWN;
        } catch (RestClientException ex) {
            return RuntimeTargetStatus.DOWN;
        }
    }
}
```

- [ ] **Step 6: Run the probe and local discovery tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.HttpHealthStatusProbeTest --tests com.yelshod.diagnosticserviceai.runtime.ConfiguredLocalServiceDiscoveryServiceTest`
Expected: PASS

- [ ] **Step 7: Commit health probing**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeStatusProbe.java src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java src/main/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryService.java src/test/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbeTest.java src/test/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryServiceTest.java
git commit -m "feat: add runtime target health probing"
```

### Task 4: Log Source Abstraction For Docker And File Tail

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/LogSource.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerLogSource.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/FileTailLogSource.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/FileTailLogSourceTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionServiceTest.java`

- [ ] **Step 1: Write a failing file-tail test that emits appended lines**

```java
@Test
void streamsNewLinesAppendedToTheConfiguredFile() throws Exception {
    Path logFile = Files.createTempFile("runtime-target", ".log");
    Files.writeString(logFile, "boot\n");

    FileTailLogSource source = new FileTailLogSource();
    List<String> emitted = new CopyOnWriteArrayList<>();

    AutoCloseable session = source.stream(logFile.toString(), emitted::add);
    Files.writeString(logFile, "boom\n", StandardOpenOption.APPEND);

    await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(emitted).contains("boom"));
    session.close();
}
```

- [ ] **Step 2: Run the file-tail test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.FileTailLogSourceTest`
Expected: FAIL with missing `FileTailLogSource`.

- [ ] **Step 3: Write the failing session-service test for source dispatch**

```java
@Test
void opensFileTailSessionsForLocalServiceTargets() {
    when(runtimeTargetService.getRequiredTarget("local-api")).thenReturn(localFileTailTarget());

    logStreamSessionService.open("session-1", "local-api");

    verify(fileTailLogSource).stream(eq("/tmp/orders.log"), any());
}
```

- [ ] **Step 4: Run the session-service test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.ws.LogStreamSessionServiceTest`
Expected: FAIL because session handling still assumes Docker-only sources.

- [ ] **Step 5: Implement the log source abstraction and route runtime targets by `logSourceType`**

```java
public interface LogSource {
    LogSourceType type();
    AutoCloseable stream(String reference, Consumer<String> consumer);
}
```

- [ ] **Step 6: Run the source and websocket tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.FileTailLogSourceTest --tests com.yelshod.diagnosticserviceai.ws.LogStreamSessionServiceTest`
Expected: PASS

- [ ] **Step 7: Commit log source abstraction**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/runtime/LogSource.java src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerLogSource.java src/main/java/com/yelshod/diagnosticserviceai/runtime/FileTailLogSource.java src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java src/test/java/com/yelshod/diagnosticserviceai/runtime/FileTailLogSourceTest.java src/test/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionServiceTest.java
git commit -m "feat: add runtime log sources"
```

### Task 5: Pipe File-Tail Logs Through Existing Analytics

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/logs/LogProcessingService.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/logs/LogProcessingServiceTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionServiceTest.java`

- [ ] **Step 1: Write the failing processing test for non-Docker runtime target input**

```java
@Test
void processesFileTailLinesWithTheSamePipelineAsDockerLines() {
    logProcessingService.process("local-api", "java.lang.IllegalStateException: boom");

    verify(clusterService).cluster(any(ErrorEvent.class));
}
```

- [ ] **Step 2: Run the processing test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.logs.LogProcessingServiceTest`
Expected: FAIL because the processing entry point still requires Docker-specific assumptions.

- [ ] **Step 3: Implement the minimal source-agnostic processing path**

```java
public void process(String runtimeTargetId, String rawLine) {
    ParsedLogLine parsedLine = logParser.parse(rawLine);
    eventAssembler.accept(runtimeTargetId, parsedLine).ifPresent(this::handleErrorEvent);
}
```

- [ ] **Step 4: Run processing and websocket tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.logs.LogProcessingServiceTest --tests com.yelshod.diagnosticserviceai.ws.LogStreamSessionServiceTest`
Expected: PASS

- [ ] **Step 5: Commit source-agnostic log processing**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/logs/LogProcessingService.java src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java src/test/java/com/yelshod/diagnosticserviceai/logs/LogProcessingServiceTest.java src/test/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionServiceTest.java
git commit -m "feat: process logs from runtime targets"
```

### Task 6: Final Verification And Transition Cleanup

**Files:**
- Modify: `README.md`
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/resources/application-test.yml`

- [ ] **Step 1: Document new runtime target concepts and required DB seed fields**

```markdown
## Runtime targets

`GET /api/runtime-targets` returns Docker containers and configured local services.

Local services require:
- `type=LOCAL_SERVICE`
- `log_source_type=FILE_TAIL`
- `log_source_ref=/absolute/path/to/log.file`
- optional `health_url`
```

- [ ] **Step 2: Run the focused backend verification suite**

Run: `TEST_DB_URL=jdbc:postgresql://localhost:5433/postgres TEST_DB_USER=postgres TEST_DB_PASSWORD=postgres ./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetControllerTest --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetServiceTest --tests com.yelshod.diagnosticserviceai.runtime.DockerRuntimeDiscoveryServiceTest --tests com.yelshod.diagnosticserviceai.runtime.ConfiguredLocalServiceDiscoveryServiceTest --tests com.yelshod.diagnosticserviceai.runtime.HttpHealthStatusProbeTest --tests com.yelshod.diagnosticserviceai.api.ProjectControllerTest --tests com.yelshod.diagnosticserviceai.ws.LogStreamSessionServiceTest`
Expected: PASS

- [ ] **Step 3: Run the full test suite**

Run: `TEST_DB_URL=jdbc:postgresql://localhost:5433/postgres TEST_DB_USER=postgres TEST_DB_PASSWORD=postgres ./gradlew test`
Expected: PASS

- [ ] **Step 4: Commit docs and final cleanup**

```bash
git add README.md src/main/resources/application.yml src/test/resources/application-test.yml
git commit -m "docs: document runtime target setup"
```
