# Backend Testability And Architecture Design

## Goal

Stabilize and simplify the backend by introducing clearer internal boundaries, broad automated test coverage across almost the entire backend, and a practical architecture that preserves external behavior where possible while allowing targeted internal improvements.

## Current Project Snapshot

The project is a Spring Boot 3.5 / Java 21 service with these active concerns:

- REST APIs for project and analytics access
- WebSocket delivery for live log and error updates
- Docker-based log ingestion
- Log parsing and multi-line error assembly
- Cluster and incident persistence through JPA/Flyway/PostgreSQL
- AI diagnosis for newly discovered clusters

Current strengths:

- The project already has a usable vertical slice from log ingestion to persistence and analytics.
- Core responsibilities are visible in package structure.
- External dependencies are conventional and workable for testing.

Current weaknesses:

- Test coverage is nearly absent.
- Several services combine orchestration, domain logic, infrastructure calls, and serialization concerns in one class.
- Some behavior is difficult to test deterministically, especially log assembly timeouts and AI invocation flow.
- WebSocket handling currently owns too much flow coordination.

## Constraints

- Preserve current REST and WebSocket behavior where reasonably possible.
- Allow targeted contract changes only if they materially improve architecture or testability.
- Favor focused refactoring over full redesign.
- First wave should cover almost the entire backend, not just happy-path smoke tests.
- Real Docker and real Gemini calls must not be required in automated tests for the first wave.

## Recommended Approach

Use a balanced internal refactor:

1. Keep the application as a single Spring Boot service.
2. Preserve external entry points where possible.
3. Split large mixed-responsibility classes into smaller units with explicit boundaries.
4. Cover domain logic with unit tests, persistence with repository integration tests, and transport contracts with controller/WebSocket-focused tests.
5. Use TDD for implementation work: every production change starts with a failing test.

This approach gives most of the architectural benefit without overengineering the codebase into a full platform rewrite.

## Target Architecture

The backend should be organized into five internal responsibility zones.

### 1. Ingestion

Purpose:
Read external log data from Docker and expose normalized stream input to the rest of the system.

Responsibilities:

- Discover project containers
- Open and close log streams
- Split Docker frames into individual log lines
- Resolve service names from container metadata

Non-responsibilities:

- Parsing log semantics
- Error-event assembly
- Cluster persistence
- AI diagnosis
- WebSocket message formatting

### 2. Log Analysis

Purpose:
Turn raw log lines into structured domain signals.

Responsibilities:

- Parse level, trace ID, timestamp, and raw message fields
- Assemble multi-line exception events
- Redact sensitive information before downstream exposure
- Create outbound message payloads for ordinary log lines and error events

Non-responsibilities:

- Database writes
- Transport/session lifecycle
- External HTTP calls

### 3. Incident Management

Purpose:
Turn assembled error events into durable operational records.

Responsibilities:

- Build deterministic cluster keys
- Create or update cluster records
- Persist incident records
- Decide whether AI diagnosis should run for a new cluster

Non-responsibilities:

- Parsing raw logs
- Building WebSocket session behavior
- Constructing AI HTTP requests directly

### 4. Delivery

Purpose:
Expose transport contracts for REST and WebSocket clients.

Responsibilities:

- Thin REST controllers
- Thin WebSocket handler
- Delegate orchestration to application services
- Serialize outbound messages

Non-responsibilities:

- Owning business rules
- Owning infrastructure-specific processing logic

### 5. Analytics

Purpose:
Serve read-model style aggregated insights from persisted incident data.

Responsibilities:

- Query repository aggregates
- Map query projections into API response models
- Enforce API contract for analytics endpoints

Non-responsibilities:

- Reprocessing logs
- Mutating incident pipeline state

## Planned Component Decomposition

### Docker Layer

Keep:

- `DockerContainerService`
- `DockerLogsService`

Add:

- `DockerFrameLogSplitter`

Reason:
The current Docker log streaming code mixes frame handling with line splitting. Extracting line splitting enables direct, low-cost tests for payload handling without Docker client setup.

### Log Analysis Layer

Keep and cover:

- `LogParser`
- `RedactionService`

Refactor:

- `EventAssembler` should separate stateful assembly rules from scheduling mechanics so timeout behavior becomes deterministic in tests.
- `LogProcessingService` should stop being a catch-all transformation service.

Add:

- `LogMessageFactory`
- `ErrorEventMessageFactory`
- One orchestration service that coordinates parser, assembler, persistence trigger, and outbound messages

Reason:
Message creation is pure transformation logic and should be tested independently from stream orchestration.

### Incident Management Layer

Refactor `ClusterService` into smaller units:

- `ClusterKeyFactory`
- `ClusterLifecycleService`
- `IncidentRecorder`
- `DiagnosisTrigger`

Reason:
The current service computes keys, persists two entity types, and invokes AI. These concerns change for different reasons and should not be tested through one large method.

### AI Layer

Refactor `AiDiagnosisService` into:

- `DiagnosisPromptFactory`
- `GeminiClient` interface
- `HttpGeminiClient` implementation
- `AiDiagnosisPersistenceService`
- Small orchestration service for diagnosis workflow

Reason:
Prompt construction, remote invocation, response extraction, and persistence should be isolated so failures are easier to test and reason about.

### Delivery Layer

Keep controllers thin.

Refactor WebSocket flow:

- Keep `LogsWebSocketHandler` as transport adapter
- Introduce `LogStreamSessionService` to manage stream subscription lifecycle and per-line processing

Reason:
The handler currently owns URI parsing, subscription tracking, stream opening, downstream processing, and message sending coordination. That is too much behavior for a transport adapter.

### Analytics Layer

Keep:

- `AnalyticsService`
- Existing repositories and DTOs where suitable

Focus:

- Add repository integration tests for aggregate queries
- Add controller contract tests

Reason:
Analytics code is already relatively focused compared with the rest of the backend; the main gap is verification.

## Data Flow

Primary runtime flow:

1. Docker adapter reads frames from a container log stream.
2. Frame splitter emits individual non-blank log lines.
3. Parser converts each line into `ParsedLogLine`.
4. Event assembler decides whether the line is ordinary context or part of an `ErrorEvent`.
5. Outbound log-line messages are produced for live viewing.
6. When a full `ErrorEvent` is assembled:
   - cluster key is calculated
   - cluster record is created or updated
   - incident record is persisted
   - AI diagnosis is triggered only for a newly created cluster
   - `ERROR_EVENT` and `CLUSTER_UPDATE` messages are emitted
7. Analytics endpoints query persisted data only.

Boundary rule:
No transport layer component should directly own domain processing decisions if an application service can own them instead.

## Error Handling And Reliability

The backend should handle failures with isolation rather than cascade:

- Docker stream failure must affect only the relevant stream/session, not global application state.
- AI diagnosis failure must never block cluster persistence or incident persistence.
- WebSocket serialization and send failures should be logged and isolated to the relevant session.
- Timeout-based event flushing must remain deterministic and testable.
- Sensitive values must be redacted before being exposed to WebSocket clients or AI payloads.

## Testing Strategy

Testing is a first-class deliverable in this initiative.

### Unit Tests

Target pure or near-pure logic:

- `LogParser`
- `RedactionService`
- `DockerFrameLogSplitter`
- `ClusterKeyFactory`
- `DiagnosisPromptFactory`
- message factories
- event assembly rules once timeout control is testable

Goal:
Fast, behavior-focused, low setup cost.

### Service Tests

Target orchestration behavior with test doubles:

- log processing orchestration
- cluster lifecycle flow
- diagnosis trigger behavior
- AI workflow behavior
- WebSocket session orchestration service

Goal:
Verify cross-component behavior without requiring real Docker or network calls.

### Repository Integration Tests

Target JPA mappings and aggregate queries:

- `IncidentRepository`
- `ClusterRepository`
- `AiDiagnosisRepository`

Goal:
Verify Flyway schema compatibility, projections, filters, and aggregation behavior.

### Web/Transport Tests

Target externally visible contracts:

- `AnalyticsController`
- `ProjectController`
- WebSocket connection/session behavior through test doubles and focused transport tests

Goal:
Preserve public behavior while internal refactoring proceeds.

### Out Of Scope For First Wave

- Real Docker end-to-end tests
- Real Gemini integration tests
- Full browser or frontend end-to-end tests

## Delivery Plan Shape

Implementation should proceed in this order:

1. Establish characterization tests around existing behavior.
2. Extract pure transformation logic into small units.
3. Refactor orchestration classes behind preserved external contracts.
4. Add repository and controller integration coverage.
5. Add WebSocket/session coverage with test doubles.
6. Finish with an individual forward plan that states:
   - what is stabilized
   - where work stopped
   - what the next implementation wave should cover

## Success Criteria

This initiative is successful when:

- almost the entire backend has meaningful automated coverage
- core logic can be tested without real Docker or real Gemini access
- major orchestration classes have clearer single-purpose boundaries
- REST and WebSocket behavior remains stable unless a specific change is intentionally justified
- the codebase has a clear documented point for the next wave of work

## Risks And Tradeoffs

- Over-refactoring could delay useful coverage; mitigation is to refactor only where testability or clarity materially improves.
- Under-refactoring could leave brittle high-level tests around hard-to-isolate classes; mitigation is to split obvious mixed-responsibility services early.
- Timeout-driven assembly logic can create flaky tests if scheduling remains embedded; mitigation is to isolate scheduling from assembly rules.
- Native SQL analytics tests may behave differently across databases; mitigation is to align repository tests with the chosen test database setup and verify query compatibility explicitly.

## Explicit Decisions

- Choose balanced refactor over full rewrite.
- Preserve external behavior where practical.
- Prefer small focused classes over large multi-responsibility services.
- Treat AI and Docker as replaceable infrastructure dependencies in tests.
- Use TDD for implementation tasks after this design is approved.

## Next Step

After review and approval of this design document, the next required step is to produce a detailed implementation plan in `docs/superpowers/plans/` with task-by-task TDD-oriented execution steps.
