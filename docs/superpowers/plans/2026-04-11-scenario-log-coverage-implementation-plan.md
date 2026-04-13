# Scenario Log Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add disciplined scenario-driven backend logging so the existing `Live Logs` page shows useful `INFO`, `WARN`, `ERROR`, and `DEBUG` events across auth, security, runtime targets, Docker integration, and websocket streaming.

**Architecture:** Extend the existing Spring Boot services and websocket components with stable, outcome-based log lines rather than introducing a new logging pipeline. Keep logging at service and boundary layers, avoid duplicate errors, and preserve the current frontend/websocket contract because the parser and `Live Logs` page already support log levels.

**Tech Stack:** Java 21, Spring Boot, Spring Security, Lombok, SLF4J, JUnit 5, Mockito, existing websocket log pipeline

---

## File Map

- Modify: `src/main/java/com/yelshod/diagnosticserviceai/auth/AuthService.java`
  - Add scenario logs for register, login, refresh, and logout outcomes.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/auth/AccountService.java`
  - Add logs for account/profile/password scenarios where outcomes matter.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/WebSocketAuthHandshakeInterceptor.java`
  - Add accepted/rejected websocket auth logs.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetService.java`
  - Add CRUD and lookup outcome logs.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrap.java`
  - Add startup bootstrap logs for seeded local services.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java`
  - Add probe result logs with disciplined level usage.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java`
  - Add Docker discovery summary/failure logs.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java`
  - Add stream open/close/failure logs.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandler.java`
  - Add websocket session lifecycle logs.
- Modify: relevant tests under `src/test/java/...`
  - Add targeted logger-capture tests where practical, otherwise preserve existing behavior tests and rely on manual verification for emitted lines.

### Task 1: Baseline Log Coverage Inventory

**Files:**
- Modify: `docs/superpowers/specs/2026-04-11-scenario-log-coverage-design.md`
- Create: no new production files
- Test: no automated test in this task

- [ ] **Step 1: Inspect the current production classes before changing code**

Read:
```text
src/main/java/com/yelshod/diagnosticserviceai/auth/AuthService.java
src/main/java/com/yelshod/diagnosticserviceai/auth/AccountService.java
src/main/java/com/yelshod/diagnosticserviceai/ws/WebSocketAuthHandshakeInterceptor.java
src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetService.java
src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrap.java
src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java
src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java
src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java
src/main/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandler.java
```

Expected: you can point to one clear outcome log to add in each class without changing business behavior.

- [ ] **Step 2: Reconfirm no secret-bearing fields will be logged**

Check that planned messages never include:
```text
password
passwordHash
Authorization header
accessToken
refreshToken
raw request JSON
```

Expected: only identifiers such as `username`, `email`, `userId`, `runtimeTargetId`, `containerId`, and `sessionId` remain in scope.

- [ ] **Step 3: Commit the planning-only checkpoint if working on an isolated branch**

```bash
git add docs/superpowers/specs/2026-04-11-scenario-log-coverage-design.md docs/superpowers/plans/2026-04-11-scenario-log-coverage-implementation-plan.md
git commit -m "docs: plan scenario log coverage"
```

Expected: a documentation checkpoint commit exists, or this step is intentionally skipped if docs are not being committed separately.

### Task 2: Add AuthService Scenario Logs

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/auth/AuthService.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/auth/AuthServiceTest.java`

- [ ] **Step 1: Add or extend tests for auth behavior hot paths**

Add or adjust tests to preserve these behaviors:
```java
@Test
void loginRejectsInvalidCredentials() {
    LoginRequest request = new LoginRequest("dev.user", "bad-pass");

    when(userRepository.findByUsername("dev.user")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("bad-pass", user.getPasswordHash())).thenReturn(false);

    assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("401 UNAUTHORIZED");
}
```

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.auth.AuthServiceTest
```

Expected: PASS before implementation changes so logging additions can be verified as behavior-preserving.

- [ ] **Step 2: Add class-level logger and outcome-based log lines**

Implement logging patterns like:
```java
log.info("User registration succeeded email={} username={} role={}",
        normalizedEmail, request.username(), request.role());

log.warn("User registration rejected duplicate email={}", normalizedEmail);

log.warn("User login rejected login={}", request.login());

log.info("User login succeeded userId={} username={}", user.getId(), user.getUsername());

log.warn("Refresh token rejected userId={} reason=expired", storedToken.getUser().getId());

log.info("User logout processed userId={}", token.getUser().getId());
```

Expected: `AuthService` emits one log per meaningful outcome, without logging tokens or passwords.

- [ ] **Step 3: Run focused auth tests**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.auth.AuthServiceTest --tests com.yelshod.diagnosticserviceai.auth.AuthControllerTest
```

Expected: PASS

- [ ] **Step 4: Commit auth logging**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/auth/AuthService.java src/test/java/com/yelshod/diagnosticserviceai/auth/AuthServiceTest.java
git commit -m "feat: add auth scenario logs"
```

### Task 3: Add AccountService Scenario Logs

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/auth/AccountService.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/auth/AccountServiceTest.java`

- [ ] **Step 1: Preserve existing account behavior with focused tests**

Confirm tests cover:
```java
@Test
void meReturnsCurrentUser() {
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    MeResponse response = accountService.me(user.getId().toString());

    assertThat(response.email()).isEqualTo("user@example.com");
}
```

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.auth.AccountServiceTest
```

Expected: PASS

- [ ] **Step 2: Add logs for account/profile/password outcomes**

Implement messages such as:
```java
log.info("Account profile fetched userId={}", user.getId());
log.info("Account profile updated userId={} username={}", user.getId(), updatedUser.getUsername());
log.warn("Password change rejected userId={} reason=invalid-current-password", user.getId());
log.info("Password changed userId={}", user.getId());
```

Expected: account-related user actions are visible in `Live Logs` without noisy duplicates.

- [ ] **Step 3: Re-run focused account tests**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.auth.AccountServiceTest --tests com.yelshod.diagnosticserviceai.auth.AccountControllerTest
```

Expected: PASS

- [ ] **Step 4: Commit account logging**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/auth/AccountService.java src/test/java/com/yelshod/diagnosticserviceai/auth/AccountServiceTest.java
git commit -m "feat: add account scenario logs"
```

### Task 4: Add Security And Websocket Auth Logs

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/WebSocketAuthHandshakeInterceptor.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/security/JwtAuthenticationFilter.java` only if a non-duplicative warning point is clearly needed
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ws/WebSocketAuthHandshakeInterceptorTest.java`

- [ ] **Step 1: Confirm handshake tests cover accepted and rejected cases**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.ws.WebSocketAuthHandshakeInterceptorTest
```

Expected: PASS and existing cases show where accepted/rejected paths occur.

- [ ] **Step 2: Add websocket auth logs at the handshake boundary**

Implement messages such as:
```java
log.info("Websocket auth accepted session={} userId={}", request.getId(), user.getId());
log.warn("Websocket auth rejected reason=missing-token");
log.warn("Websocket auth rejected reason=invalid-token");
log.warn("Websocket auth rejected reason=user-not-found subject={}", subject);
```

Constraint:
```text
Do not log the raw token or authorization header.
```

Expected: websocket authentication outcomes are visible without polluting REST request logs.

- [ ] **Step 3: Re-run handshake tests**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.ws.WebSocketAuthHandshakeInterceptorTest
```

Expected: PASS

- [ ] **Step 4: Commit websocket auth logging**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/ws/WebSocketAuthHandshakeInterceptor.java src/test/java/com/yelshod/diagnosticserviceai/ws/WebSocketAuthHandshakeInterceptorTest.java
git commit -m "feat: add websocket auth logs"
```

### Task 5: Add Runtime Target And Bootstrap Logs

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetService.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrap.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrapTest.java`
- Test: existing runtime target service/controller tests

- [ ] **Step 1: Run current runtime target tests**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetBootstrapTest --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetControllerTest
```

Expected: PASS

- [ ] **Step 2: Add logs for bootstrap and CRUD outcomes**

Implement messages such as:
```java
log.info("Runtime target bootstrap started configuredTargets={}", appProperties.runtime().defaultLocalTargets().size());
log.info("Runtime target bootstrap inserted targets count={}", seeds.size());
log.debug("Runtime target bootstrap skipped because repository already contains targets");

log.info("Runtime target created id={} name={} type={}", entity.getId(), entity.getName(), entity.getType());
log.info("Runtime target updated id={} name={} enabled={}", entity.getId(), entity.getName(), entity.isEnabled());
log.info("Runtime target deleted id={}", id);
log.warn("Runtime target lookup failed id={}", id);
log.debug("Runtime targets listed count={}", targets.size());
```

Expected: lifecycle operations become visible in `Live Logs` while keeping list summaries at `DEBUG`.

- [ ] **Step 3: Re-run focused runtime tests**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetBootstrapTest --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetControllerTest
```

Expected: PASS

- [ ] **Step 4: Commit runtime target logging**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetService.java src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrap.java src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrapTest.java
git commit -m "feat: log runtime target lifecycle"
```

### Task 6: Add Health Probe And Docker Discovery Logs

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java`
- Test: relevant runtime target and discovery tests if present

- [ ] **Step 1: Identify probe and discovery call sites before editing**

Read:
```text
src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java
src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java
```

Expected: you know which success paths should be `DEBUG` and which operational problems should be `WARN`.

- [ ] **Step 2: Add disciplined health probe and Docker discovery logs**

Implement patterns like:
```java
log.debug("Runtime target health probe succeeded target={} status={}", target.name(), response.statusCode());
log.warn("Runtime target health probe failed target={} healthUrl={}", target.name(), target.healthUrl(), ex);

log.debug("Docker discovery started labelKey={} labelValue={}", labelKey, labelValue);
log.info("Docker discovery completed matches={}", containers.size());
log.warn("Docker discovery returned no matching containers labelKey={} labelValue={}", labelKey, labelValue);
log.warn("Docker discovery unavailable because Docker daemon could not be reached", ex);
```

Constraint:
```text
If a path polls frequently, prefer DEBUG for the normal success case.
```

Expected: routine polling remains low-noise while operationally important failures stand out.

- [ ] **Step 3: Run relevant tests**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetControllerTest --tests com.yelshod.diagnosticserviceai.api.ProjectControllerTest
```

Expected: PASS

- [ ] **Step 4: Commit probe/discovery logging**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java
git commit -m "feat: add runtime probe and docker discovery logs"
```

### Task 7: Add Docker Log Stream And Websocket Session Logs

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandler.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java` only if session registration is the right single boundary
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandlerTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionServiceTest.java`

- [ ] **Step 1: Run websocket session tests**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.ws.LogsWebSocketHandlerTest --tests com.yelshod.diagnosticserviceai.ws.LogStreamSessionServiceTest
```

Expected: PASS

- [ ] **Step 2: Add websocket session and Docker streaming logs**

Implement messages such as:
```java
log.info("Websocket log stream opened session={} runtimeTargetId={}", session.getId(), runtimeTargetId);
log.warn("Websocket log stream rejected session={} reason=missing-runtime-target-id", session.getId());
log.info("Websocket log stream closed session={} status={}", session.getId(), status);
log.error("Websocket transport error session={}", session.getId(), exception);

log.info("Docker log stream opened containerId={}", containerId);
log.info("Docker log stream closed containerId={}", containerId);
log.error("Docker log streaming failed containerId={}", containerId, ex);
```

Expected: operators can see stream lifecycle transitions directly in the existing log view.

- [ ] **Step 3: Re-run websocket/log stream tests**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.ws.LogsWebSocketHandlerTest --tests com.yelshod.diagnosticserviceai.ws.LogStreamSessionServiceTest
```

Expected: PASS

- [ ] **Step 4: Commit websocket and log stream logging**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java src/main/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandler.java src/main/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionService.java src/test/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandlerTest.java src/test/java/com/yelshod/diagnosticserviceai/ws/LogStreamSessionServiceTest.java
git commit -m "feat: add websocket and docker stream logs"
```

### Task 8: Verification In Running App

**Files:**
- Modify: no files required unless verification reveals a bug
- Test: manual runtime verification against the local app

- [ ] **Step 1: Start the backend with the dev profile**

Run:
```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Expected: app starts successfully and writes logs to the configured local file in `dev`.

- [ ] **Step 2: Exercise auth scenarios**

Run or trigger:
```text
POST /api/auth/register
POST /api/auth/login with valid credentials
POST /api/auth/login with invalid password
POST /api/auth/refresh
POST /api/auth/logout
```

Expected: `INFO` for success paths, `WARN` for expected rejections, no secrets in logs.

- [ ] **Step 3: Exercise runtime, websocket, and Docker paths**

Trigger:
```text
Open Runtime Targets page
Create/update/delete a local runtime target
Open Live Logs for a target
Disconnect/reconnect websocket
If possible, stop Docker or target an unavailable daemon once
```

Expected: runtime target lifecycle logs, websocket open/close logs, and Docker/probe warnings appear with the right levels.

- [ ] **Step 4: Run the high-signal automated suite after manual verification**

Run:
```bash
./gradlew test --tests com.yelshod.diagnosticserviceai.auth.AuthServiceTest --tests com.yelshod.diagnosticserviceai.auth.AccountServiceTest --tests com.yelshod.diagnosticserviceai.ws.WebSocketAuthHandshakeInterceptorTest --tests com.yelshod.diagnosticserviceai.ws.LogsWebSocketHandlerTest --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetBootstrapTest --tests com.yelshod.diagnosticserviceai.runtime.RuntimeTargetControllerTest
```

Expected: PASS

- [ ] **Step 5: Commit final verification-clean state**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/auth/AuthService.java src/main/java/com/yelshod/diagnosticserviceai/auth/AccountService.java src/main/java/com/yelshod/diagnosticserviceai/ws/WebSocketAuthHandshakeInterceptor.java src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetService.java src/main/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrap.java src/main/java/com/yelshod/diagnosticserviceai/runtime/HttpHealthStatusProbe.java src/main/java/com/yelshod/diagnosticserviceai/runtime/DockerRuntimeDiscoveryService.java src/main/java/com/yelshod/diagnosticserviceai/docker/DockerLogsService.java src/main/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandler.java src/test/java/com/yelshod/diagnosticserviceai/auth/AuthServiceTest.java src/test/java/com/yelshod/diagnosticserviceai/auth/AccountServiceTest.java src/test/java/com/yelshod/diagnosticserviceai/ws/WebSocketAuthHandshakeInterceptorTest.java src/test/java/com/yelshod/diagnosticserviceai/ws/LogsWebSocketHandlerTest.java src/test/java/com/yelshod/diagnosticserviceai/runtime/RuntimeTargetBootstrapTest.java
git commit -m "feat: add scenario-driven backend log coverage"
```

## Self-Review

- Spec coverage:
  - auth/account logging: covered by Tasks 2 and 3
  - websocket/security logging: covered by Tasks 4 and 7
  - runtime/bootstrap/probe logging: covered by Tasks 5 and 6
  - Docker discovery/streaming logging: covered by Tasks 6 and 7
  - verification in existing `Live Logs`: covered by Task 8
- Placeholder scan:
  - no `TODO`/`TBD` placeholders remain
  - every task has exact file targets and verification commands
- Type consistency:
  - uses existing class names and endpoint paths from the current codebase
  - logging examples stay observational and do not introduce new contracts
