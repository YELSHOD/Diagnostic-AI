# Gemini Diagnose Endpoint Design

## Goal

Connect the backend to Gemini so the product can send a diagnostics prompt and receive an AI-generated response through a real backend endpoint.

This should be the first usable vertical slice of AI integration, not a placeholder config-only change.

## Scope

Included:
- backend reads Gemini settings from config
- backend validates whether Gemini integration is configured
- backend exposes a real REST endpoint for AI diagnosis
- backend sends a prompt to Gemini over direct HTTP
- backend returns a structured response that frontend can consume later
- safe logging and failure behavior

Not included:
- frontend chat UI
- persistence of prompts or responses
- streaming AI responses
- multiple providers
- long conversation history

## Architecture

The backend should own the Gemini integration behind a small service abstraction.

Recommended shape:
- `GeminiClient` or `GeminiService` handles outbound HTTP
- `AiDiagnosisService` builds the request and interprets the response
- `AiDiagnosisController` exposes the REST endpoint

This keeps provider details away from the controller and gives a clean place for prompt shaping and future provider swaps.

## Configuration

The backend already has config keys under:
- `app.gemini.api-key`
- `app.gemini.model`
- `app.gemini.prompt-version`

These should remain the source of truth.

### Secret handling

`GEMINI_API_KEY` must **not** be committed as a real token in repo files.

Rules:
- `.env.example` contains only placeholder/example values
- real token stays in local `.env`, shell env, or IntelliJ run config
- logs must never print the API key

## Endpoint

Recommended endpoint:
- `POST /api/ai/diagnose`

Recommended request shape:

```json
{
  "service": "diagnosticserviceai",
  "question": "Why is this service unstable?",
  "logLines": [
    "2026-04-13T11:21:40Z WARN ...",
    "2026-04-13T11:21:41Z ERROR ..."
  ]
}
```

Recommended response shape:

```json
{
  "provider": "gemini",
  "model": "gemini-2.5-flash",
  "promptVersion": "v1",
  "summary": "Likely root cause is ...",
  "bullets": [
    "Observation 1",
    "Observation 2"
  ],
  "rawText": "Full Gemini answer text"
}
```

## Prompt Strategy

The first version should be simple and deterministic.

Prompt should include:
- role framing: local diagnostics assistant
- service name if available
- user question
- bounded log excerpt
- instruction to produce concise root-cause-oriented output

Suggested output contract from Gemini:
- short summary
- 2-5 bullets
- no markdown tables

The backend may still parse the response loosely if Gemini returns plain text, but the prompt should guide it into a stable structure.

## Failure Behavior

### Missing API key

If Gemini is not configured:
- endpoint should fail clearly
- response should be a 503-style service-unavailable error or a 400-level configuration error
- message should say Gemini integration is not configured

### Provider failure

If Gemini request fails:
- backend logs the failure without leaking secret data
- endpoint returns a clean backend error response
- do not dump giant provider payloads into logs unless in debug-level summaries

### Empty input

If request has neither meaningful question nor log lines:
- validation should fail
- endpoint should return a clear 400

## Logging

Use the same disciplined operational logging already added in the project.

Recommended levels:
- `INFO`: AI diagnosis request started/completed with service and model
- `WARN`: Gemini not configured or invalid request shape
- `ERROR`: outbound Gemini failure
- `DEBUG`: bounded provider diagnostics without secrets

Never log:
- API key
- full unbounded prompts with sensitive content

## Security

The endpoint should stay behind existing authenticated API security unless there is a strong reason not to.

If current MVP auth already protects normal API routes, this endpoint should follow the same rule.

## Testing Strategy

Backend verification should include:
- config present/missing cases
- request validation
- successful Gemini response mapping
- provider failure mapping
- no real network calls in tests

Use mocked HTTP exchange boundaries so tests stay deterministic.

## Success Criteria

This feature is successful when:
- backend can read Gemini config safely
- a caller can hit `/api/ai/diagnose` and get a real AI response
- missing config fails clearly
- no secret token is committed or logged
- the design leaves room for future frontend chat integration
