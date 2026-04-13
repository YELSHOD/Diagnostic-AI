# Scenario Log Coverage Design

## Goal

Improve backend log usefulness so the existing frontend `Live Logs` page shows meaningful scenario-driven operational events across authentication, security, runtime target management, Docker integration, and websocket log streaming.

This work does not add a new log transport or frontend protocol. It increases the quality and consistency of backend log emission so the current log stream becomes useful during demos, debugging, and investigation.

## Scope

In scope:
- Add structured, human-readable `INFO`, `WARN`, `ERROR`, and selective `DEBUG` logs to key backend flows
- Cover auth and security flows
- Cover runtime target lifecycle and health probing
- Cover Docker discovery and Docker log streaming lifecycle
- Cover websocket log stream lifecycle and websocket authentication failures
- Keep messages safe for local development use without exposing secrets

Out of scope:
- New frontend pages or new websocket message types
- Persisting logs in the database
- Full audit logging semantics
- Changing the existing parsing contract beyond what current `Live Logs` already understands

## Existing System Context

The current system already supports the downstream path needed for this feature:
- backend emits plain log lines
- log sources stream lines through the websocket log pipeline
- `LogParser` extracts `INFO`, `WARN`, `ERROR`, and `DEBUG`
- frontend `Live Logs` already filters and styles by level

Because that path already exists, the feature should focus on improving emission quality rather than adding a parallel logging mechanism.

## Desired Outcome

When a user opens `Live Logs`, they should see scenario-relevant operational messages such as:
- login success or login rejection
- registration success or duplicate-user rejection
- refresh/logout results
- websocket connection accepted or rejected
- runtime target creation, update, deletion, bootstrap seeding
- Docker discovery success, zero-results cases, and daemon failures
- log stream opened, closed, or interrupted
- health probe success/failure summaries

These logs should help explain what the system is doing without forcing the user to infer behavior from stack traces alone.

## Logging Model

### Level Rules

- `INFO`: successful scenario milestones and normal lifecycle transitions
- `WARN`: rejected requests, invalid user actions, unavailable dependencies, empty/partial conditions that are important but not code-failure bugs
- `ERROR`: unexpected failures with exception attached
- `DEBUG`: high-frequency technical details that help local debugging but are noisy for normal demos

### Message Style

Each log line should:
- start with a stable scenario/action phrase
- include outcome-oriented wording
- include only the identifiers needed for diagnosis
- avoid dumping whole request objects

Preferred style examples:
- `User login succeeded for username=dev.user`
- `User login rejected for login=user@example.com`
- `Runtime target created id=... name=frontend-dev type=LOCAL_SERVICE`
- `Docker discovery failed for project label ai.project.env=demo`
- `Websocket log stream opened session=... runtimeTargetId=...`

### Sensitive Data Rules

Never log:
- passwords
- JWT access tokens
- refresh tokens
- authorization headers
- full request payloads containing secrets

Allowed identifiers:
- username
- normalized email
- user id
- role code
- runtime target id/name/type
- container id/name when relevant
- websocket session id
- health URL hostname/path if already configured and non-secret

## Component Coverage

### Auth And Account

Apply logging in auth/account services and controllers where outcomes are meaningful.

Expected coverage:
- register requested
- register rejected because email or username already exists
- register rejected because role is unknown
- register succeeded
- login rejected because credentials are invalid
- login succeeded
- refresh rejected because token is invalid or expired
- refresh succeeded
- logout processed
- current account/profile fetch and update success/failure where useful
- password change success
- password change rejection because current password is invalid

Level guidance:
- success paths: `INFO`
- invalid credentials / duplicate user / bad role / invalid refresh token: `WARN`
- unexpected service exceptions: `ERROR`
- optional low-level token persistence details: `DEBUG`

### Security And Websocket Auth

Expected coverage:
- REST unauthenticated access attempts when surfaced through explicit entry points or interceptors
- websocket handshake accepted
- websocket handshake rejected due to missing or invalid token
- token subject lookup failures that cause authentication rejection

Level guidance:
- accepted auth: `INFO` or `DEBUG` depending on volume
- rejected auth: `WARN`
- token parsing or unexpected auth failures: `ERROR` only when truly exceptional, otherwise `WARN`

### Runtime Targets

Expected coverage:
- configured local targets bootstrap started
- bootstrap skipped because repository already has persisted targets
- bootstrap inserted configured defaults
- create runtime target succeeded
- update runtime target succeeded
- delete runtime target succeeded
- list runtime targets summary at `DEBUG`
- health probe result for a target at `DEBUG` on success and `WARN` on failure/unreachable state

Level guidance:
- CRUD success and bootstrap seeding: `INFO`
- invalid target references or unhealthy targets: `WARN`
- persistence/probe exceptions: `ERROR`

### Docker Discovery And Streaming

Expected coverage:
- container discovery started at `DEBUG`
- discovery returned N containers at `INFO` or `DEBUG`
- discovery returned zero matching containers at `WARN` or `INFO` depending on current project intent
- Docker daemon unavailable for discovery or streaming at `WARN`
- Docker log stream opened for container
- Docker log stream closed
- Docker log streaming failure with exception

Recommended rule:
- normal list polling summaries default to `DEBUG` unless they represent a notable lifecycle event
- daemon unavailability is `WARN` because it is operationally important but can happen in local development
- actual thrown streaming failures are `ERROR`

### Websocket Log Stream Lifecycle

Expected coverage:
- websocket session connected
- runtime target missing from websocket request
- websocket session registered to runtime target
- websocket session closed
- transport error during websocket session

Level guidance:
- open/close/session registration: `INFO` or `DEBUG`
- invalid/missing request parameters: `WARN`
- transport exceptions: `ERROR`

## Implementation Shape

The implementation should follow existing Spring logging conventions:
- add class-level `log` fields through Lombok `@Slf4j` where the project already uses Lombok
- place logs at service boundaries and decision points, not in every helper method
- keep one meaningful log per outcome rather than multiple repetitive lines for the same action

The feature should prefer:
- logging after an outcome is known
- one summary log for a successful scenario
- one warning log for a handled rejection
- one error log with exception for an unhandled failure

## Data Flow

1. Backend component emits a standard application log line.
2. Existing file or Docker log source reads the line.
3. Existing parser extracts timestamp and log level.
4. Existing websocket pipeline sends it to the frontend.
5. Existing `Live Logs` page displays and filters it by level.

No protocol change is required for this feature.

## Error Handling

The logging feature must not introduce behavior changes in business logic.

Rules:
- logging is observational, not control flow
- avoid catching exceptions only to log and swallow them
- if an exception is already propagated, log at the correct boundary once
- avoid duplicate `ERROR` logs at multiple layers for the same failure unless each layer adds distinct diagnostic value

## Testing Strategy

Automated coverage should focus on the highest-signal cases:
- tests for auth/security flows continue verifying HTTP status behavior
- add targeted tests where current codebase already captures logs feasibly
- if logger-capture tests are too heavy, verify by focused manual runtime checks after implementation

Manual verification should include:
- register a user and observe registration logs
- log in with valid credentials and invalid credentials
- open live logs and confirm `INFO/WARN/ERROR/DEBUG` lines appear
- exercise runtime target CRUD
- open and close websocket log stream
- simulate Docker unavailable path if feasible

## Risks

- Over-logging can make `Live Logs` noisy and reduce value
- Duplicated logs across controller and service layers can create confusion
- Logging too much auth detail can create privacy or security issues
- High-frequency debug logs can obscure true error signals

## Mitigations

- keep `INFO` for milestones only
- reserve `DEBUG` for noisy technical detail
- use one primary log per scenario outcome
- explicitly exclude secrets and tokens from logged fields

## Acceptance Criteria

- Existing `Live Logs` page shows useful scenario-driven entries during normal app use
- Successful flows emit visible `INFO` logs
- Expected rejections emit visible `WARN` logs
- Real failures emit `ERROR` logs with exception context
- Existing log parsing still recognizes levels correctly
- No passwords or tokens are written to logs
