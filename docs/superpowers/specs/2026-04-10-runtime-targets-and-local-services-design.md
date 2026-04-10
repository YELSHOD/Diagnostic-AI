# Runtime Targets And Local Services Design

## Context

The current backend can discover Docker containers and stream Docker logs, but it cannot represent local applications started directly from IntelliJ, Gradle, Node, or other local processes. This creates a mismatch with the real product goal:

- one local observability workspace
- visibility into Docker containers and locally started services
- live logs, errors, clusters, and analytics across all supported sources

The existing `/api/projects` endpoint is Docker-only. It filters containers by labels and returns an empty list when the monitored service is running outside Docker or when Docker labels do not match.

This design expands the system from "Docker project viewer" into a local observability hub with a unified runtime model.

## Goal

Introduce a unified `runtime target` model that supports:

- Docker containers
- manually configured local services
- shared health/status reporting
- shared live-log entry points
- shared analytics/error pipeline regardless of source

This wave does not aim to auto-discover every OS process or replicate the full Elastic Stack. It focuses on a pragmatic architecture that can grow toward that direction.

## Non-Goals

This wave will not include:

- full OS process discovery
- direct stdout capture from arbitrary IDE processes
- Elasticsearch-like indexing/storage
- multi-host fleet management
- a dedicated agent installed on each machine

## Product Direction

The product should be treated as a `Local Observability Hub`, not only as a Docker viewer.

The user should be able to:

- see Docker containers that belong to the working environment
- see local services they explicitly configured
- check whether each target is reachable
- open live logs from each target through an appropriate log source
- analyze errors and clusters through the same backend pipeline

## Approaches Considered

### Approach 1: Docker plus manually configured local services

The system discovers Docker containers automatically and reads additional local services from configuration or database records.

Pros:

- practical and stable
- clear ownership over what should appear in the UI
- easy to evolve toward database-managed targets
- avoids unreliable OS-level heuristics

Cons:

- local services are not automatically discovered

### Approach 2: Docker plus automatic localhost port discovery

The backend scans localhost ports and tries to infer services from open ports.

Pros:

- feels more magical
- lower manual setup

Cons:

- open ports do not provide logs by themselves
- service identity is often unclear
- more false positives
- harder to explain and test

### Approach 3: Full local observability agent

Use a separate agent to collect local processes, ports, files, and logs.

Pros:

- closest to a production-grade observability platform

Cons:

- too large for this phase
- introduces a separate deployment model

### Recommendation

Choose `Approach 1`.

It gives a strong and explainable MVP:

- Docker support remains first-class
- local services become visible without fragile heuristics
- the system gets a clean architecture for future file-tail and ingest-based logging

## Target Architecture

The backend should stop modeling the UI around "projects" or "containers" only. The new top-level abstraction is `RuntimeTarget`.

### Core model

`RuntimeTarget` represents any observable execution target.

Suggested fields:

- `id`
- `name`
- `type`
- `status`
- `host`
- `port`
- `healthUrl`
- `logSourceType`
- `logSourceRef`
- `metadata`

### Runtime target types

- `DOCKER_CONTAINER`
- `LOCAL_SERVICE`

Future expansion can add:

- `REMOTE_SERVICE`
- `FILE_SOURCE`

### Runtime target status

- `UP`
- `DOWN`
- `UNKNOWN`
- `DEGRADED`

### Log source types

- `DOCKER`
- `FILE_TAIL`
- `HTTP_INGEST`

`HTTP_INGEST` is future-facing and not required in the first implementation wave.

## Components

### 1. Runtime target discovery layer

Introduce a common discovery interface:

- `RuntimeTargetDiscoveryService`

Implementations:

- `DockerRuntimeDiscoveryService`
- `ConfiguredLocalServiceDiscoveryService`

Responsibilities:

- enumerate targets
- normalize source-specific data into `RuntimeTargetDto`
- avoid analytics/log-processing concerns

### 2. Runtime target read service

Introduce an orchestration service:

- `RuntimeTargetService`

Responsibilities:

- combine targets from all discovery providers
- sort or group them for API output
- enrich them with runtime status if needed

### 3. Status probing layer

For local services, status should be derived through health probing.

Introduce:

- `RuntimeStatusProbe`
- `HttpHealthStatusProbe`

Behavior:

- if `healthUrl` is configured and reachable with successful HTTP status, mark target `UP`
- if probe fails, mark `DOWN` or `DEGRADED`
- if no probe is configured, mark `UNKNOWN`

Docker targets can continue using Docker container state for status.

### 4. Log source abstraction

Introduce:

- `LogSource`
- `DockerLogSource`
- `FileTailLogSource`

Responsibilities:

- stream raw log lines from the source
- normalize them into the existing log-processing pipeline input

This keeps log parsing, clustering, and analytics independent from where logs originated.

### 5. Shared log-processing pipeline

The existing pipeline stays conceptually the same:

- parse line
- assemble error event
- cluster incident
- store analytics
- emit websocket updates

The difference is that it now receives lines from multiple source adapters instead of Docker only.

## Data Model

### Phase 1 storage choice

Local services should be stored in the database rather than only in `application.yml`.

Reasoning:

- easier to keep behavior consistent across devices
- better fit for a real product UI later
- avoids config drift between laptops
- supports CRUD from the frontend in future waves

### New table

Add a `runtime_targets` table for non-Docker managed targets.

Suggested fields:

- `id`
- `name`
- `type`
- `host`
- `port`
- `health_url`
- `log_source_type`
- `log_source_ref`
- `enabled`
- `created_at`
- `updated_at`

For this phase, records in this table represent manually configured `LOCAL_SERVICE` targets.

Docker targets remain discovered dynamically and do not need persistence yet.

## API Design

### Replace or extend `/api/projects`

The long-term API should move away from Docker-specific naming.

New endpoint:

- `GET /api/runtime-targets`

Response should include both Docker and local service targets.

Example fields:

- `id`
- `name`
- `type`
- `status`
- `host`
- `port`
- `logSourceType`
- `metadata`

### Compatibility strategy

Keep `/api/projects` temporarily as a compatibility alias or Docker-only endpoint during transition, but treat `/api/runtime-targets` as the target contract for the frontend.

### Future endpoints

Planned later:

- `POST /api/runtime-targets`
- `PATCH /api/runtime-targets/{id}`
- `DELETE /api/runtime-targets/{id}`

These are not required in the first implementation wave if targets are managed through seed data or admin-side DB setup.

## Live Logs Design

### Docker targets

Continue using Docker API streaming.

### Local services

A local service cannot provide logs from `host:port` alone. The system needs an explicit log source.

Supported first-wave option:

- `FILE_TAIL`

Meaning:

- the user configures a log file path for the local service
- backend tails the file and streams new lines into the same processing pipeline

Future option:

- `HTTP_INGEST`

Meaning:

- applications actively send logs to the backend

Recommendation:

- first implement `FILE_TAIL`
- design interfaces so `HTTP_INGEST` can be added later without reworking the pipeline

## Frontend Impact

The frontend should stop assuming that every observable target is a Docker container.

### Replace "containers" mental model

The main list page should evolve from `Containers` into a broader `Services` or `Runtime Targets` page.

The UI should show:

- Docker targets
- local services
- source type badge
- status badge
- host and port where relevant

### Investigation flow

Recommended product flow:

- `Overview`
- `Runtime Targets`
- `Live Logs`
- `Analysis`

This keeps the current product flow but broadens the source model.

## Error Handling

The backend should differentiate clearly between:

- auth failures (`401`)
- validation failures (`400`)
- runtime target source unavailability (`503`)

Examples:

- Docker daemon unavailable -> `503`
- configured file path missing for file-tail -> `503` or `404` depending on endpoint semantics
- target not found -> `404`

Avoid masking runtime errors behind auth responses.

## Testing Strategy

### Unit tests

Add tests for:

- runtime target mapping
- local service status probing
- file-tail source behavior
- runtime target aggregation logic

### Integration tests

Add tests for:

- `GET /api/runtime-targets`
- persisted `runtime_targets` repository
- auth access to runtime target endpoints

### Characterization tests

Keep tests around Docker-backed discovery so the old behavior remains stable during the transition.

## Implementation Boundaries

This design is large enough to require decomposition into implementation phases.

Recommended order:

1. introduce `RuntimeTarget` read model and `/api/runtime-targets`
2. add persisted `LOCAL_SERVICE` targets
3. add status probing for local services
4. refactor frontend to use runtime targets instead of containers
5. add `FILE_TAIL` log source
6. connect file-tail logs into the existing pipeline
7. later add `HTTP_INGEST`

## Success Criteria

This design is successful when:

- the backend lists both Docker targets and configured local services
- the frontend can render them in one page
- the user can distinguish type and status clearly
- analytics remain source-agnostic
- local services can later provide logs without redesigning the whole system

## Summary

The right next step is not OS-wide magic discovery. It is a clean architectural generalization:

- from `container` to `runtime target`
- from `docker logs only` to `source-based log acquisition`
- from `Docker-only dashboard` to `local observability hub`

That gives the project a stable path toward the intended thesis outcome without prematurely overbuilding a full Elastic-like platform.
