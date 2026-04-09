# Backend Next Wave Notes

## Stabilized In This Wave

- Log frame splitting, log parsing, redaction, and event assembly are now covered by focused automated tests.
- Cluster processing is split into smaller responsibilities: cluster key generation, cluster lifecycle updates, incident recording, and AI diagnosis triggering.
- AI diagnosis flow is decomposed into prompt construction, remote Gemini transport, persistence, and orchestration.
- WebSocket delivery no longer owns all orchestration logic directly; stream/session coordination now sits behind a dedicated session service.
- REST controller contracts for analytics and project listing are covered by tests against the actual JSON contract.

## Current Stopping Point

- Core backend behavior is now significantly more testable and internally decomposed.
- Unit and service-level refactors are in place for the most coupled parts of the backend.
- Controller contract tests are green.
- PostgreSQL-backed repository integration tests are prepared structurally, but not finishable yet because local Docker/Testcontainers are unavailable in the current environment.

## Remaining Risks

- Repository integration coverage is incomplete until Docker/Testcontainers can run.
- No real Docker end-to-end verification exists yet for log ingestion and stream lifecycle.
- No real Gemini integration verification exists yet; AI behavior is currently tested through isolated components and mocks/stubs.
- There are still deprecation warnings in the build around Spring Boot test mocking APIs and Docker Java usage that should be cleaned up in a future pass.

## What Is Left To Close The Current Plan

1. Bring local Docker daemon online.
2. Finish `Task 1` by running PostgreSQL-backed integration tests through Testcontainers.
3. Finish repository integration coverage from `Task 7` for analytics-related persistence behavior.
4. Run a full `./gradlew test` verification pass with Docker available.

## Next Wave For The Dissertation Direction

This project is evolving toward a lightweight local observability platform with AI assistance, effectively an `Elastic-like` system on a smaller academic scope.

The next product-facing wave should focus on:

1. Local service and port discovery so the platform can automatically detect locally running projects.
2. A unified service inventory model so containers, ports, logs, incidents, clusters, and analytics can be shown in one dashboard.
3. AI chat over operational data so the user can ask questions across logs, incidents, clusters, and analytics from the UI.
4. Richer analytics and timeline views so the system moves from raw incident visibility toward actual diagnostic workflows.
5. Explicit project/session boundaries for the website so all local runtime data can be presented as one coherent monitoring surface.

## Practical Recommendation

When Docker becomes available again, the first priority is not adding new features. The first priority is to complete the repository integration layer and run a full green verification pass. After that, the codebase is in a better position to safely grow into service discovery, AI chat, and full dashboard capabilities.
