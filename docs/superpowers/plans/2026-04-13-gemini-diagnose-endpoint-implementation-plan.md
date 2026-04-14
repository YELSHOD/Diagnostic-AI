# Gemini Diagnose Endpoint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a real backend `POST /api/ai/diagnose` endpoint backed by Gemini, with safe config handling, request validation, structured response mapping, and no committed API secrets.

**Architecture:** Reuse the existing `ai` package, but reshape it into a clean request-response vertical slice. `HttpGeminiClient` remains the provider boundary, `DiagnosisPromptFactory` becomes responsible for prompt building for both cluster-triggered and ad hoc diagnose requests, and a new controller/service pair exposes a frontend-ready endpoint with explicit validation and configuration failure behavior.

**Tech Stack:** Spring Boot, RestClient, Jackson, JUnit 5, MockMvc, Mockito, Lombok

---

## File Structure

- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/GeminiClient.java`
  Extend the client boundary so it supports the new request-response use case cleanly.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/HttpGeminiClient.java`
  Keep direct HTTP integration, but add safer error handling and provider response extraction for synchronous diagnose requests.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactory.java`
  Add a public prompt builder for ad hoc diagnosis requests while keeping the existing cluster-trigger prompt path intact.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisService.java`
  Add a synchronous `diagnose` flow returning structured data, while preserving the existing cluster-triggered background path.
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisRequest.java`
  Request DTO for `/api/ai/diagnose`.
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisResponse.java`
  Structured response DTO returned to callers.
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisController.java`
  REST endpoint for ad hoc diagnosis.
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisDisabledException.java`
  Explicit error for missing Gemini config.
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisProviderException.java`
  Explicit error for outbound Gemini failures.
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/security/SecurityConfig.java`
  Confirm `/api/ai/diagnose` stays authenticated.
- Modify: `.env.example`
  Replace the committed real Gemini token with a placeholder value.
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisServiceTest.java`
  Cover missing config, success mapping, and provider failure.
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisControllerTest.java`
  Cover endpoint validation, success path, and missing-config response.
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactoryTest.java`
  Verify bounded prompt construction for the new request DTO.
- Optional test update: `src/test/java/com/yelshod/diagnosticserviceai/auth/AuthSecurityIntegrationTest.java`
  Only if existing security coverage needs an assertion that `/api/ai/diagnose` is protected.

### Task 1: Remove Real Secret And Define The Public AI Contract

**Files:**
- Modify: `.env.example`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisRequest.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisResponse.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactoryTest.java`

- [ ] **Step 1: Write the failing prompt-factory test for ad hoc diagnosis input**

```java
package com.yelshod.diagnosticserviceai.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import java.util.List;
import org.junit.jupiter.api.Test;

class DiagnosisPromptFactoryTest {

    private final DiagnosisPromptFactory factory =
            new DiagnosisPromptFactory(new RedactionService(), new ObjectMapper());

    @Test
    void buildsPromptForAdHocDiagnosisRequest() {
        AiDiagnosisRequest request = new AiDiagnosisRequest(
                "diagnosticserviceai",
                "Why is this service unstable?",
                List.of(
                        "2026-04-13T11:21:40Z WARN Docker discovery skipped",
                        "2026-04-13T11:21:41Z ERROR Something failed"
                )
        );

        String prompt = factory.buildInputJson(request);

        assertThat(prompt).contains("diagnosticserviceai");
        assertThat(prompt).contains("Why is this service unstable?");
        assertThat(prompt).contains("Docker discovery skipped");
        assertThat(prompt).contains("Something failed");
    }
}
```

- [ ] **Step 2: Run the prompt-factory test to verify it fails**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.ai.DiagnosisPromptFactoryTest`  
Expected: FAIL because `AiDiagnosisRequest` and `buildInputJson(AiDiagnosisRequest)` do not exist yet.

- [ ] **Step 3: Replace the real token in `.env.example` with a placeholder**

```dotenv
# Gemini settings.
# Create the key in Google AI Studio: https://ai.google.dev/aistudio
# Then paste it into GEMINI_API_KEY in your local `.env` or IntelliJ run config.
GEMINI_API_KEY=your-gemini-api-key-here
GEMINI_MODEL=gemini-2.5-flash
GEMINI_PROMPT_VERSION=v1
```

- [ ] **Step 4: Add the public request/response DTOs**

```java
package com.yelshod.diagnosticserviceai.ai;

import jakarta.validation.constraints.Size;
import java.util.List;

public record AiDiagnosisRequest(
        @Size(max = 120) String service,
        @Size(max = 2_000) String question,
        List<@Size(max = 2_000) String> logLines
) {
}
```

```java
package com.yelshod.diagnosticserviceai.ai;

import java.util.List;

public record AiDiagnosisResponse(
        String provider,
        String model,
        String promptVersion,
        String summary,
        List<String> bullets,
        String rawText
) {
}
```

- [ ] **Step 5: Add the ad hoc prompt builder in `DiagnosisPromptFactory`**

```java
public String buildInputJson(AiDiagnosisRequest request) {
    try {
        return objectMapper.writeValueAsString(Map.of(
                "service", request.service(),
                "question", redactionService.redact(request.question() == null ? "" : request.question()),
                "logLines", request.logLines() == null
                        ? List.of()
                        : request.logLines().stream()
                                .limit(50)
                                .map(redactionService::redact)
                                .toList()
        ));
    } catch (JsonProcessingException e) {
        throw new IllegalStateException("Unable to build AI diagnosis prompt", e);
    }
}
```

- [ ] **Step 6: Run the prompt-factory test to verify it passes**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.ai.DiagnosisPromptFactoryTest`  
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add .env.example src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisRequest.java src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisResponse.java src/main/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactory.java src/test/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactoryTest.java
git commit -m "refactor: define Gemini diagnose contract"
```

### Task 2: Add Synchronous AI Diagnosis Service Logic

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/GeminiClient.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/HttpGeminiClient.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisDisabledException.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisProviderException.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisServiceTest.java`

- [ ] **Step 1: Write the failing service tests**

```java
package com.yelshod.diagnosticserviceai.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AiDiagnosisServiceTest {

    private final DiagnosisPromptFactory promptFactory =
            new DiagnosisPromptFactory(new com.yelshod.diagnosticserviceai.common.RedactionService(), new com.fasterxml.jackson.databind.ObjectMapper());
    private final GeminiClient geminiClient = Mockito.mock(GeminiClient.class);
    private final AiDiagnosisPersistenceService persistenceService = Mockito.mock(AiDiagnosisPersistenceService.class);

    @Test
    void throwsWhenGeminiIsNotConfigured() {
        AiDiagnosisService service = new AiDiagnosisService(
                promptFactory,
                geminiClient,
                persistenceService,
                new AppProperties(
                        new AppProperties.Docker("label", "value", 100),
                        new AppProperties.Gemini("", "gemini-2.5-flash", "v1"),
                        new AppProperties.Runtime(List.of()),
                        new AppProperties.Demo(false, false, 0, "", "")
                )
        );

        assertThatThrownBy(() -> service.diagnose(new AiDiagnosisRequest("svc", "why?", List.of("line"))))
                .isInstanceOf(AiDiagnosisDisabledException.class);
    }

    @Test
    void mapsGeminiTextIntoStructuredResponse() {
        when(geminiClient.generateDiagnosisJson(anyString(), anyString()))
                .thenReturn("{\"summary\":\"Likely root cause\",\"bullets\":[\"Observation A\",\"Observation B\"]}");

        AiDiagnosisService service = new AiDiagnosisService(
                promptFactory,
                geminiClient,
                persistenceService,
                new AppProperties(
                        new AppProperties.Docker("label", "value", 100),
                        new AppProperties.Gemini("secret", "gemini-2.5-flash", "v1"),
                        new AppProperties.Runtime(List.of()),
                        new AppProperties.Demo(false, false, 0, "", "")
                )
        );

        AiDiagnosisResponse response = service.diagnose(new AiDiagnosisRequest("svc", "why?", List.of("line")));

        assertThat(response.provider()).isEqualTo("gemini");
        assertThat(response.model()).isEqualTo("gemini-2.5-flash");
        assertThat(response.summary()).isEqualTo("Likely root cause");
        assertThat(response.bullets()).containsExactly("Observation A", "Observation B");
    }
}
```

- [ ] **Step 2: Run the service tests to verify they fail**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.ai.AiDiagnosisServiceTest`  
Expected: FAIL because synchronous `diagnose` and the new exceptions do not exist yet.

- [ ] **Step 3: Add explicit provider/config exceptions**

```java
package com.yelshod.diagnosticserviceai.ai;

public class AiDiagnosisDisabledException extends RuntimeException {
    public AiDiagnosisDisabledException(String message) {
        super(message);
    }
}
```

```java
package com.yelshod.diagnosticserviceai.ai;

public class AiDiagnosisProviderException extends RuntimeException {
    public AiDiagnosisProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Extend `AiDiagnosisService` with synchronous request-response behavior**

```java
public AiDiagnosisResponse diagnose(AiDiagnosisRequest request) {
    AppProperties.Gemini gemini = appProperties.gemini();
    if (gemini.apiKey() == null || gemini.apiKey().isBlank()) {
        throw new AiDiagnosisDisabledException("Gemini integration is not configured");
    }

    if ((request.question() == null || request.question().isBlank())
            && (request.logLines() == null || request.logLines().isEmpty())) {
        throw new IllegalArgumentException("Question or logLines must be provided");
    }

    try {
        String prompt = diagnosisPromptFactory.buildInputJson(request);
        String rawText = geminiClient.generateDiagnosisJson(gemini.model(), prompt);
        JsonNode parsed = new ObjectMapper().readTree(rawText);

        List<String> bullets = new ArrayList<>();
        if (parsed.path("bullets").isArray()) {
            parsed.path("bullets").forEach(node -> bullets.add(node.asText()));
        }

        String summary = parsed.path("summary").asText(rawText);
        return new AiDiagnosisResponse("gemini", gemini.model(), gemini.promptVersion(), summary, bullets, rawText);
    } catch (AiDiagnosisDisabledException ex) {
        throw ex;
    } catch (Exception ex) {
        throw new AiDiagnosisProviderException("Gemini diagnosis failed", ex);
    }
}
```

- [ ] **Step 5: Harden `HttpGeminiClient` for provider failures**

```java
@Override
public String generateDiagnosisJson(String model, String prompt) {
    try {
        JsonNode response = client.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1beta/models/{model}:generateContent")
                        .queryParam("key", appProperties.gemini().apiKey())
                        .build(model))
                .contentType(MediaType.APPLICATION_JSON)
                .body(diagnosisPromptFactory.buildRequestPayload(prompt))
                .retrieve()
                .body(JsonNode.class);

        return extractModelText(response);
    } catch (Exception ex) {
        throw new AiDiagnosisProviderException("Gemini request failed", ex);
    }
}
```

- [ ] **Step 6: Run the service tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.ai.AiDiagnosisServiceTest`  
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/ai/GeminiClient.java src/main/java/com/yelshod/diagnosticserviceai/ai/HttpGeminiClient.java src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisService.java src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisDisabledException.java src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisProviderException.java src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisServiceTest.java
git commit -m "feat: add synchronous Gemini diagnosis service"
```

### Task 3: Expose `/api/ai/diagnose` And Validate Requests

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisController.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/security/SecurityConfig.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisControllerTest.java`

- [ ] **Step 1: Write the failing controller tests**

```java
package com.yelshod.diagnosticserviceai.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiDiagnosisController.class)
class AiDiagnosisControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AiDiagnosisService aiDiagnosisService;

    @Test
    void returnsStructuredDiagnosis() throws Exception {
        when(aiDiagnosisService.diagnose(any())).thenReturn(
                new AiDiagnosisResponse("gemini", "gemini-2.5-flash", "v1", "Likely root cause", List.of("Observation A"), "{\"summary\":\"Likely root cause\"}")
        );

        mockMvc.perform(post("/api/ai/diagnose")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AiDiagnosisRequest("svc", "why?", List.of("line")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("gemini"))
                .andExpect(jsonPath("$.summary").value("Likely root cause"));
    }

    @Test
    void returnsServiceUnavailableWhenGeminiIsNotConfigured() throws Exception {
        when(aiDiagnosisService.diagnose(any()))
                .thenThrow(new AiDiagnosisDisabledException("Gemini integration is not configured"));

        mockMvc.perform(post("/api/ai/diagnose")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new AiDiagnosisRequest("svc", "why?", List.of("line")))))
                .andExpect(status().isServiceUnavailable());
    }
}
```

- [ ] **Step 2: Run the controller tests to verify they fail**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.ai.AiDiagnosisControllerTest`  
Expected: FAIL because the controller does not exist yet.

- [ ] **Step 3: Add the controller with validation and exception mapping**

```java
package com.yelshod.diagnosticserviceai.ai;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiDiagnosisController {

    private final AiDiagnosisService aiDiagnosisService;

    @PostMapping("/diagnose")
    public AiDiagnosisResponse diagnose(@Valid @RequestBody AiDiagnosisRequest request) {
        return aiDiagnosisService.diagnose(request);
    }

    @ExceptionHandler(AiDiagnosisDisabledException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ErrorResponse handleDisabled(AiDiagnosisDisabledException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler({AiDiagnosisProviderException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleBadRequest(RuntimeException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    record ErrorResponse(String message) {}
}
```

- [ ] **Step 4: Confirm security leaves `/api/ai/diagnose` authenticated**

```java
.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/demo/scenarios/**", "/actuator/health", "/error", "/ws/**")
.permitAll()
.anyRequest()
.authenticated()
```

If the endpoint is already protected by `anyRequest().authenticated()`, do not add a permissive matcher for `/api/ai/**`.

- [ ] **Step 5: Run the controller tests to verify they pass**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.ai.AiDiagnosisControllerTest`  
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisController.java src/main/java/com/yelshod/diagnosticserviceai/security/SecurityConfig.java src/test/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisControllerTest.java
git commit -m "feat: expose Gemini diagnosis endpoint"
```

### Task 4: Final Verification Of The Gemini Vertical Slice

**Files:**
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisService.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/HttpGeminiClient.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisController.java`
- Modify: `.env.example`

- [ ] **Step 1: Add final structured logging**

```java
log.info("AI diagnosis requested service={} model={}", request.service(), gemini.model());
log.info("AI diagnosis completed service={} model={}", request.service(), gemini.model());
log.warn("AI diagnosis rejected because Gemini is not configured");
log.error("AI diagnosis provider call failed model={}", gemini.model(), ex);
```

- [ ] **Step 2: Run the focused Gemini test suite**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.ai.DiagnosisPromptFactoryTest --tests com.yelshod.diagnosticserviceai.ai.AiDiagnosisServiceTest --tests com.yelshod.diagnosticserviceai.ai.AiDiagnosisControllerTest`  
Expected: PASS

- [ ] **Step 3: Run broader backend verification**

Run: `./gradlew test --tests com.yelshod.diagnosticserviceai.auth.AuthControllerTest --tests com.yelshod.diagnosticserviceai.auth.AccountControllerTest --tests com.yelshod.diagnosticserviceai.api.ProjectControllerTest`  
Expected: PASS, confirming the new AI endpoint did not disturb existing authenticated API behavior.

- [ ] **Step 4: Commit**

```bash
git add .env.example src/main/java/com/yelshod/diagnosticserviceai/ai/*.java src/test/java/com/yelshod/diagnosticserviceai/ai/*.java
git commit -m "test: verify Gemini diagnose endpoint flow"
```

## Self-Review

### Spec coverage

- Secret handling and placeholder-only `.env.example` are covered in Task 1.
- Real Gemini-backed synchronous service logic is covered in Task 2.
- Public REST endpoint and clean failure mapping are covered in Task 3.
- Final focused verification and logging discipline are covered in Task 4.

### Placeholder scan

- No `TODO` / `TBD` placeholders remain.
- Each task includes exact files, commands, and concrete code snippets.
- Commit steps are explicit and scoped.

### Type consistency

- `AiDiagnosisRequest`, `AiDiagnosisResponse`, `AiDiagnosisDisabledException`, and `AiDiagnosisProviderException` are introduced before they are used elsewhere in the plan.
- The service and controller test expectations match the proposed endpoint contract and exception mapping.
