# Runtime Targets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Docker-only "projects" model with a unified runtime-target system that supports Docker containers, configured local services, shared status reporting, and future source-agnostic live logs.

**Architecture:** The backend introduces a new `RuntimeTarget` read model, a discovery layer for Docker and configured local services, and a source abstraction for logs. The frontend transitions from `containers` to `runtime targets` while preserving the current operational flow: overview, targets, live logs, analysis.

**Tech Stack:** Spring Boot, JPA, Flyway, PostgreSQL, Docker Java, React, React Query, Zustand, WebSocket

---

## File Structure

### Backend files

- Create: `src/main/resources/db/migration/V3__runtime_targets.sql`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetType.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetStatus.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/LogSourceType.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetDto.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetEntity.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetRepository.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetDiscoveryService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeStatusProbe.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/api/RuntimeTargetController.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/logsource/LogSource.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/logsource/DockerLogSource.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/logsource/FileTailLogSource.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/logsource/LogSourceRouter.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/api/ProjectController.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/config/AppProperties.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`
- Modify: `src/main/resources/application-docker.yml`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetServiceTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbeTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/api/RuntimeTargetControllerTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetRepositoryIntegrationTest.java`

### Frontend files

- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/entities/runtime-target/api.ts`
- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/entities/runtime-target/types.ts`
- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/entities/runtime-target/api.test.ts`
- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/RuntimeTargetsPage.tsx`
- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/RuntimeTargetsPage.test.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/app/router.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/ContainersPage.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/OverviewPage.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/shared/ui/ShellLayout.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/features/realtime/useLogsSocket.ts`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/shared/types/api.ts`

## Task 1: Add Backend Runtime Target Persistence

**Files:**
- Create: `src/main/resources/db/migration/V3__runtime_targets.sql`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetEntity.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetRepository.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetType.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/LogSourceType.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetRepositoryIntegrationTest.java`

- [ ] **Step 1: Write the failing repository integration test**

```java
@Test
void savesLocalServiceRuntimeTarget() {
    RuntimeTargetEntity entity = RuntimeTargetEntity.builder()
            .name("diagnostic-ai-front")
            .type(RuntimeTargetType.LOCAL_SERVICE)
            .host("localhost")
            .port(5173)
            .healthUrl("http://localhost:5173")
            .logSourceType(LogSourceType.FILE_TAIL)
            .logSourceRef("/tmp/diagnostic-ai-front.log")
            .enabled(true)
            .build();

    RuntimeTargetEntity saved = runtimeTargetRepository.save(entity);

    assertThat(saved.getId()).isNotNull();
    assertThat(runtimeTargetRepository.findAll()).hasSize(1);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetRepositoryIntegrationTest`
Expected: FAIL because migration, entity, and repository do not exist yet.

- [ ] **Step 3: Add migration and persistence model**

```sql
create table runtime_targets (
    id uuid primary key,
    name varchar(120) not null,
    type varchar(40) not null,
    host varchar(255),
    port integer,
    health_url varchar(500),
    log_source_type varchar(40) not null,
    log_source_ref varchar(1000),
    enabled boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
```

```java
public enum RuntimeTargetType {
    DOCKER_CONTAINER,
    LOCAL_SERVICE
}
```

```java
public enum LogSourceType {
    DOCKER,
    FILE_TAIL,
    HTTP_INGEST
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetRepositoryIntegrationTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V3__runtime_targets.sql src/main/java/com/yelshod/diagnosticserviceai/runtime src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetRepositoryIntegrationTest.java
git commit -m "feat: add runtime target persistence model"
```

## Task 2: Add Runtime Target Discovery And Status Probing

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetDto.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetStatus.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetDiscoveryService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/ConfiguredLocalServiceDiscoveryService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeStatusProbe.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetService.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetServiceTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbeTest.java`

- [ ] **Step 1: Write the failing service test**

```java
@Test
void combinesDockerAndLocalTargetsIntoSingleList() {
    when(dockerDiscoveryService.discover()).thenReturn(List.of(
            new RuntimeTargetDto("docker:abc", "api", RuntimeTargetType.DOCKER_CONTAINER, RuntimeTargetStatus.UP, null, null, null, LogSourceType.DOCKER, "abc", Map.of())
    ));
    when(localDiscoveryService.discover()).thenReturn(List.of(
            new RuntimeTargetDto("local:front", "front", RuntimeTargetType.LOCAL_SERVICE, RuntimeTargetStatus.UP, "localhost", 5173, "http://localhost:5173", LogSourceType.FILE_TAIL, "/tmp/front.log", Map.of())
    ));

    List<RuntimeTargetDto> result = runtimeTargetService.listTargets();

    assertThat(result).hasSize(2);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetServiceTest`
Expected: FAIL because DTOs and service do not exist.

- [ ] **Step 3: Implement DTOs, discovery interfaces, and target service**

```java
public interface RuntimeTargetDiscoveryService {
    List<RuntimeTargetDto> discover();
}
```

```java
public List<RuntimeTargetDto> listTargets() {
    return Stream.concat(dockerRuntimeDiscoveryService.discover().stream(),
                    configuredLocalServiceDiscoveryService.discover().stream())
            .sorted(Comparator.comparing(RuntimeTargetDto::name))
            .toList();
}
```

- [ ] **Step 4: Add health probe test and implementation**

```java
@Test
void marksTargetUpWhenHealthEndpointReturns200() {
    RuntimeTargetStatus status = probe.probe("http://localhost:8080/actuator/health");
    assertThat(status).isEqualTo(RuntimeTargetStatus.UP);
}
```

```java
public RuntimeTargetStatus probe(String healthUrl) {
    try {
        HttpStatusCode status = restClient.get().uri(healthUrl).retrieve().toBodilessEntity().getStatusCode();
        return status.is2xxSuccessful() ? RuntimeTargetStatus.UP : RuntimeTargetStatus.DEGRADED;
    } catch (Exception ex) {
        return RuntimeTargetStatus.DOWN;
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetServiceTest --tests com.yelshod.diagnosticserviceai.runtime.HttpHealthStatusProbeTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/runtime src/test/java/com/yelshod/diagnosticserviceai/runtime
git commit -m "feat: add runtime target discovery services"
```

## Task 3: Add Runtime Target API And Compatibility Layer

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/api/RuntimeTargetController.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/api/ProjectController.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/api/RuntimeTargetControllerTest.java`
- Modify: `src/test/java/com/yelshod/diagnosticserviceai/auth/AuthSecurityIntegrationTest.java`

- [ ] **Step 1: Write the failing controller contract test**

```java
@Test
void returnsRuntimeTargetsForAuthenticatedUser() throws Exception {
    given(runtimeTargetService.listTargets()).willReturn(List.of(
            new RuntimeTargetDto("local:front", "front", RuntimeTargetType.LOCAL_SERVICE, RuntimeTargetStatus.UP, "localhost", 5173, "http://localhost:5173", LogSourceType.FILE_TAIL, "/tmp/front.log", Map.of())
    ));

    mockMvc.perform(get("/api/runtime-targets").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("LOCAL_SERVICE"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.api.RuntimeTargetControllerTest`
Expected: FAIL because controller does not exist.

- [ ] **Step 3: Implement new controller and keep compatibility**

```java
@RestController
@RequestMapping("/api/runtime-targets")
@RequiredArgsConstructor
public class RuntimeTargetController {
    private final RuntimeTargetService runtimeTargetService;

    @GetMapping
    public List<RuntimeTargetDto> runtimeTargets() {
        return runtimeTargetService.listTargets();
    }
}
```

```java
@GetMapping
public List<RuntimeTargetDto> projects() {
    return runtimeTargetService.listTargets().stream()
            .filter(target -> target.type() == RuntimeTargetType.DOCKER_CONTAINER)
            .toList();
}
```

- [ ] **Step 4: Extend security integration coverage**

```java
mockMvc.perform(get("/api/runtime-targets")
        .header("Authorization", "Bearer " + accessToken))
    .andExpect(status().isOk());
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.api.RuntimeTargetControllerTest --tests com.yelshod.diagnosticserviceai.auth.AuthSecurityIntegrationTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/api src/test/java/com/yelshod/diagnosticserviceai/api src/test/java/com/yelshod/diagnosticserviceai/auth/AuthSecurityIntegrationTest.java
git commit -m "feat: add runtime targets api"
```

## Task 4: Add Log Source Abstraction And File Tail Support

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/logsource/LogSource.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/logsource/DockerLogSource.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/logsource/FileTailLogSource.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/logsource/LogSourceRouter.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/logsource/FileTailLogSourceTest.java`

- [ ] **Step 1: Write the failing file-tail test**

```java
@Test
void streamsNewLinesFromFileTailSource() throws Exception {
    Path file = Files.createTempFile("runtime-target", ".log");
    Files.writeString(file, "first\n");
    List<String> seen = new ArrayList<>();

    try (LogSourceSession session = fileTailLogSource.stream(file.toString(), seen::add)) {
        Files.writeString(file, "second\n", StandardOpenOption.APPEND);
        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> assertThat(seen).contains("second"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.logsource.FileTailLogSourceTest`
Expected: FAIL because file-tail log source does not exist.

- [ ] **Step 3: Implement log source interfaces and router**

```java
public interface LogSource {
    boolean supports(RuntimeTargetDto target);
    LogSourceSession stream(RuntimeTargetDto target, Consumer<DockerLogLine> consumer);
}
```

```java
public LogSourceSession open(RuntimeTargetDto target, Consumer<DockerLogLine> consumer) {
    return logSources.stream()
            .filter(source -> source.supports(target))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported log source"))
            .stream(target, consumer);
}
```

- [ ] **Step 4: Wire file-tail into live log orchestration**

```java
LogSourceSession session = logSourceRouter.open(target, line -> processLine(...));
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.logsource.FileTailLogSourceTest --tests com.yelshod.diagnosticserviceai.ws.LogStreamSessionServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/logsource src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java src/test/java/com/yelshod/diagnosticserviceai/logsource/FileTailLogSourceTest.java
git commit -m "feat: add file tail log sources for runtime targets"
```

## Task 5: Add Seeded Local Services And Config Support

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`
- Modify: `src/main/resources/application-docker.yml`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/config/AppProperties.java`
- Modify: `README.md`

- [ ] **Step 1: Add failing configuration test**

```java
assertThat(appProperties.runtime().defaultLocalTargets()).isNotEmpty();
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.config.AppPropertiesTest`
Expected: FAIL because runtime target config does not exist.

- [ ] **Step 3: Add bootstrap config for local services**

```yaml
app:
  runtime:
    default-local-targets:
      - name: diagnosticserviceai
        host: localhost
        port: 8080
        health-url: http://localhost:8080/actuator/health
        log-source-type: FILE_TAIL
        log-source-ref: ./logs/diagnosticserviceai.log
```

- [ ] **Step 4: Update local discovery to seed missing database targets**

```java
if (runtimeTargetRepository.count() == 0) {
    seedDefaults(appProperties.runtime().defaultLocalTargets());
}
```

- [ ] **Step 5: Run verification**

Run: `./gradlew test`
Expected: PASS or only known environment-sensitive integration blockers.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/application*.yml src/main/java/com/yelshod/diagnosticserviceai/config/AppProperties.java README.md
git commit -m "feat: add local runtime target configuration bootstrap"
```

## Task 6: Add Frontend Runtime Target Data Layer

**Files:**
- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/entities/runtime-target/types.ts`
- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/entities/runtime-target/api.ts`
- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/entities/runtime-target/api.test.ts`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/shared/types/api.ts`

- [ ] **Step 1: Write the failing API test**

```ts
it('maps runtime target responses', async () => {
  server.use(http.get('http://localhost:8080/api/runtime-targets', () => HttpResponse.json([
    { id: 'local:front', name: 'front', type: 'LOCAL_SERVICE', status: 'UP', host: 'localhost', port: 5173, logSourceType: 'FILE_TAIL', metadata: {} }
  ])))

  const result = await listRuntimeTargets()
  expect(result[0].type).toBe('LOCAL_SERVICE')
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/admin/IdeaProjects/diagnostic-ai-front && npm test -- --run src/entities/runtime-target/api.test.ts`
Expected: FAIL because entity API does not exist.

- [ ] **Step 3: Implement runtime target types and API**

```ts
export type RuntimeTarget = {
  id: string
  name: string
  type: 'DOCKER_CONTAINER' | 'LOCAL_SERVICE'
  status: 'UP' | 'DOWN' | 'UNKNOWN' | 'DEGRADED'
  host: string | null
  port: number | null
  healthUrl: string | null
  logSourceType: 'DOCKER' | 'FILE_TAIL' | 'HTTP_INGEST'
  metadata: Record<string, string>
}
```

```ts
export async function listRuntimeTargets() {
  return request<RuntimeTarget[]>('/api/runtime-targets')
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd /Users/admin/IdeaProjects/diagnostic-ai-front && npm test -- --run src/entities/runtime-target/api.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd /Users/admin/IdeaProjects/diagnostic-ai-front
git add src/entities/runtime-target src/shared/types/api.ts
git commit -m "feat: add frontend runtime target api"
```

## Task 7: Replace Containers Page With Runtime Targets Page

**Files:**
- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/RuntimeTargetsPage.tsx`
- Create: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/RuntimeTargetsPage.test.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/ContainersPage.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/app/router.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/shared/ui/ShellLayout.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/shared/i18n/messages.ts`

- [ ] **Step 1: Write the failing page test**

```tsx
it('renders docker and local runtime targets together', async () => {
  renderWithRouter(<RuntimeTargetsPage />)
  expect(await screen.findByText('diagnosticserviceai')).toBeInTheDocument()
  expect(await screen.findByText('LOCAL_SERVICE')).toBeInTheDocument()
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/admin/IdeaProjects/diagnostic-ai-front && npm test -- --run src/pages/RuntimeTargetsPage.test.tsx`
Expected: FAIL because page does not exist.

- [ ] **Step 3: Implement new page and route migration**

```tsx
export function RuntimeTargetsPage() {
  const query = useQuery({ queryKey: ['runtime-targets'], queryFn: listRuntimeTargets })
  // render cards/table with source and status badges
}
```

```tsx
<Route path="/runtime-targets" element={<RuntimeTargetsPage />} />
```

- [ ] **Step 4: Keep compatibility navigation**

```tsx
<Route path="/containers" element={<Navigate to="/runtime-targets" replace />} />
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd /Users/admin/IdeaProjects/diagnostic-ai-front && npm test -- --run src/pages/RuntimeTargetsPage.test.tsx src/shared/ui/ShellLayout.test.tsx`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
cd /Users/admin/IdeaProjects/diagnostic-ai-front
git add src/pages/RuntimeTargetsPage.tsx src/pages/RuntimeTargetsPage.test.tsx src/app/router.tsx src/shared/ui/ShellLayout.tsx src/shared/i18n/messages.ts src/pages/ContainersPage.tsx
git commit -m "feat: replace containers ui with runtime targets"
```

## Task 8: Connect Live Logs And Overview To Runtime Targets

**Files:**
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/features/realtime/useLogsSocket.ts`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/OverviewPage.tsx`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/src/pages/AnalysisPage.tsx`

- [ ] **Step 1: Write the failing socket test**

```ts
it('opens websocket with runtimeTargetId param', () => {
  useLogsSocket({ runtimeTargetId: 'local:front' })
  expect(connectSpy).toHaveBeenCalledWith(expect.stringContaining('runtimeTargetId=local%3Afront'))
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd /Users/admin/IdeaProjects/diagnostic-ai-front && npm test -- --run src/features/realtime/useLogsSocket.test.ts`
Expected: FAIL because the hook still expects container-oriented parameters.

- [ ] **Step 3: Update realtime flow**

```ts
const socketUrl = buildWsUrl(`/ws/logs?runtimeTargetId=${encodeURIComponent(runtimeTargetId)}&token=${encodeURIComponent(token)}`)
```

```tsx
<Link to={`/live-logs?runtimeTargetId=${target.id}`}>Open live logs</Link>
```

- [ ] **Step 4: Update overview messaging**

```tsx
<KpiCard title={t('overview.runtimeTargets')} value={targets.length} />
```

- [ ] **Step 5: Run verification**

Run: `cd /Users/admin/IdeaProjects/diagnostic-ai-front && npm test -- --run src/features/realtime/useLogsSocket.test.ts src/app/router.test.tsx && npm run build`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
cd /Users/admin/IdeaProjects/diagnostic-ai-front
git add src/features/realtime/useLogsSocket.ts src/pages/LiveLogsPage.tsx src/pages/OverviewPage.tsx src/pages/AnalysisPage.tsx
git commit -m "feat: wire live logs and overview to runtime targets"
```

## Task 9: Final Verification And Docs

**Files:**
- Modify: `docs/superpowers/plans/2026-04-10-runtime-targets-implementation-plan.md`
- Modify: `docs/superpowers/specs/2026-04-09-backend-next-wave-notes.md`
- Modify: `/Users/admin/IdeaProjects/diagnostic-ai-front/docs/superpowers/specs/2026-04-09-frontend-next-wave-notes.md`

- [ ] **Step 1: Run backend verification**

Run: `./gradlew test`
Expected: PASS or only explicitly documented environment-sensitive failures.

- [ ] **Step 2: Run frontend verification**

Run: `cd /Users/admin/IdeaProjects/diagnostic-ai-front && npm test -- --run && npm run build`
Expected: PASS

- [ ] **Step 3: Run live smoke checks**

Run:

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/api/runtime-targets -H "Authorization: Bearer <token>"
```

Expected:
- `health` returns `200`
- `runtime-targets` returns Docker and/or local targets

- [ ] **Step 4: Update docs with actual progress**

```md
- runtime targets API is live
- runtime targets page replaced containers page
- remaining next wave: HTTP ingest and CRUD for local services
```

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers
git commit -m "docs: record runtime targets implementation progress"
```

## Self-Review

- Spec coverage:
  - unified runtime target model: covered by Tasks 1-3
  - database-backed local services: covered by Tasks 1 and 5
  - status probing: covered by Task 2
  - file-tail local logs: covered by Task 4
  - frontend migration from containers to runtime targets: covered by Tasks 6-8
  - compatibility with current product flow: covered by Tasks 3, 7, and 8
- Placeholder scan:
  - no `TBD`, `TODO`, or implicit "handle later" items remain inside implementation tasks
- Type consistency:
  - backend consistently uses `RuntimeTargetDto`, `RuntimeTargetType`, `RuntimeTargetStatus`, `LogSourceType`
  - frontend consistently uses `RuntimeTarget` and `runtimeTargetId`
