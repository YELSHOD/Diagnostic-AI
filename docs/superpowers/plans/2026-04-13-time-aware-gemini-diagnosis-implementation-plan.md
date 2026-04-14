# Time-Aware Gemini Diagnosis Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Gemini diagnosis flow aware of the active `Live Logs` time window and filters, and return a richer structured answer centered on timeline, evidence, root cause, and next checks.

**Architecture:** Extend the existing backend `AiDiagnosisRequest` / `AiDiagnosisResponse` contract and the frontend `Live Logs` diagnose flow instead of creating a parallel AI system. The current page remains the orchestration point, while the backend prompt factory becomes responsible for turning UI context into a time-aware diagnosis prompt.

**Tech Stack:** Spring Boot, Jackson, existing Gemini HTTP client, React, TypeScript, Vitest, existing `LiveLogsPage`, CSS in `globals.css`

---

## File Structure

- Modify: `/Users/admin/IdeaProjects/DiagnosticServiceAI/src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisRequest.java`
- Modify: `/Users/admin/IdeaProjects/DiagnosticServiceAI/src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisResponse.java`
- Modify: `/Users/admin/IdeaProjects/DiagnosticServiceAI/src/main/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactory.java`
- Modify: `/Users/admin/IdeaProjects/DiagnosticServiceAI/src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisService.java`
- Modify tests in `/Users/admin/IdeaProjects/DiagnosticServiceAI/src/test/java/com/yelshod/diagnosticserviceai/ai/`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`

## Task 1: Extend Backend AI Contract For Time-Aware Diagnosis

**Files:**
- `AiDiagnosisRequest.java`
- `AiDiagnosisResponse.java`
- `AiDiagnosisControllerTest.java`
- `AiDiagnosisServiceTest.java`

- [ ] Add request fields for:
  - `timeRange.mode`
  - `timeRange.label`
  - `timeRange.from`
  - `timeRange.to`
  - `levelFilter`
  - `textFilter`
- [ ] Add response fields for:
  - `timeline`
  - `probableRootCause`
  - `evidence`
  - `nextChecks`
- [ ] Update controller/service tests to cover the richer request/response payloads.
- [ ] Run targeted backend AI tests and confirm red/green progression.

## Task 2: Make The Gemini Prompt Explicitly Time-Aware

**Files:**
- `DiagnosisPromptFactory.java`
- `AiDiagnosisService.java`
- `DiagnosisPromptFactoryTest.java`

- [ ] Update prompt construction so it explicitly includes:
  - active time-range label
  - `from` / `to`
  - active text filter
  - active level filter
  - instruction to analyze only the supplied interval
  - instruction to produce timeline, root cause, evidence, and next checks
- [ ] Keep the model output parseable into the richer response object.
- [ ] Add tests asserting the prompt contains the time-window and filter context.
- [ ] Run targeted backend AI tests.

## Task 3: Send Time Context From Live Logs

**Files:**
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts`
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] Extend the frontend request type to include `timeRange`, `levelFilter`, and `textFilter`.
- [ ] Build request context directly from the current page state:
  - active time range
  - selected level filter
  - current text filter
  - visible log buffer
- [ ] Show the active time-range summary inside the AI panel.
- [ ] Update tests to assert the richer payload is sent.
- [ ] Run `npm test -- LiveLogsPage`.

## Task 4: Render The Richer Structured Diagnosis

**Files:**
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] Replace the simple summary-only render with structured sections:
  - summary
  - timeline
  - probable root cause
  - evidence
  - next checks
  - raw response
- [ ] Keep the panel compact and aligned with the current ops-console design.
- [ ] Add tests for successful rendering of the new structured blocks.
- [ ] Run `npm test -- LiveLogsPage`.

## Task 5: Verify End-To-End Behavior

**Files:**
- Backend and frontend working trees

- [ ] Run targeted backend AI tests:
  - `DiagnosisPromptFactoryTest`
  - `AiDiagnosisServiceTest`
  - `AiDiagnosisControllerTest`
- [ ] Run frontend verification:
  - `npm test -- LiveLogsPage`
  - `npm run build`
- [ ] Manually confirm that:
  - `Live Logs` sends the selected time range
  - backend returns structured fields
  - frontend renders timeline/root-cause/evidence/next-checks
- [ ] Commit backend and frontend changes separately after verification.
