# AI Chat V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a working `AI Chat` page with two modes, `Diagnosis` and `Product Help`, while reusing the existing backend `POST /api/ai/diagnose` endpoint and the current structured diagnosis response.

**Architecture:** Keep a single backend AI endpoint and extend its request contract with `mode`. The frontend `AI Chat` page becomes the orchestration surface for both diagnosis and product-help requests. Backend prompt construction branches by mode: diagnosis remains log/time-range oriented, while product help injects application knowledge context and avoids making up unsupported features.

**Tech Stack:** Spring Boot, existing Gemini client and diagnosis service, React, TypeScript, existing frontend routing/i18n/styling patterns, Vitest, Testing Library

---

## File Structure

- Modify: `/Users/admin/IdeaProjects/DiagnosticServiceAI/src/main/java/com/yelshod/diagnosticserviceai/ai/AiDiagnosisRequest.java`
- Modify: `/Users/admin/IdeaProjects/DiagnosticServiceAI/src/main/java/com/yelshod/diagnosticserviceai/ai/DiagnosisPromptFactory.java`
- Modify tests in `/Users/admin/IdeaProjects/DiagnosticServiceAI/src/test/java/com/yelshod/diagnosticserviceai/ai/`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/AiChatPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/AiChatPage.test.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`

## Task 1: Extend Backend Request Contract With AI Mode

**Files:**
- `AiDiagnosisRequest.java`
- `AiDiagnosisControllerTest.java`
- `AiDiagnosisServiceTest.java`

- [ ] Add a request field:
  - `mode: "diagnosis" | "product_help"`
- [ ] Keep backward-compatible validation simple and explicit.
- [ ] Update controller/service tests to cover both modes.
- [ ] Run targeted backend AI tests and confirm failures before implementation.

## Task 2: Add Product-Help Prompt Branch

**Files:**
- `DiagnosisPromptFactory.java`
- `DiagnosisPromptFactoryTest.java`

- [ ] Add product knowledge context describing:
  - major pages
  - navigation/actions
  - account/password/settings/runtime targets/live logs/demo scenarios
  - feature constraints
- [ ] Branch prompt instructions by `mode`:
  - `diagnosis` keeps log-analysis behavior
  - `product_help` answers app-usage questions using the product context
- [ ] Preserve current language behavior and JSON schema.
- [ ] Add prompt tests verifying that `product_help` includes product knowledge and “do not invent features”.
- [ ] Run targeted backend AI tests.

## Task 3: Build AI Chat V1 Frontend

**Files:**
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/AiChatPage.tsx`
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/AiChatPage.test.tsx`
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts`
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`

- [ ] Add mode-aware request type to frontend AI API helper.
- [ ] Replace placeholder `AI Chat` page with a real working screen.
- [ ] Add UI elements:
  - mode switch
  - optional target selector
  - question input
  - submit/loading/error states
  - structured answer render
- [ ] In `Diagnosis` mode, allow sending target + optional log context.
- [ ] In `Product Help` mode, allow question-only requests without log lines.
- [ ] Add tests for both modes.
- [ ] Run `npm test -- AiChatPage`.

## Task 4: Polish Integration

**Files:**
- frontend routing/UI files as needed

- [ ] Ensure sidebar `AI Chat` is no longer labeled as “later”.
- [ ] Keep styling aligned with the current ops-console refresh.
- [ ] Ensure empty and error states are understandable.
- [ ] Run frontend build verification.

## Task 5: Verify End-To-End Behavior

**Files:**
- backend and frontend working trees

- [ ] Run targeted backend AI verification:
  - `DiagnosisPromptFactoryTest`
  - `AiDiagnosisServiceTest`
  - `AiDiagnosisControllerTest`
- [ ] Run frontend verification:
  - `npm test -- AiChatPage`
  - `npm run build`
- [ ] Manually verify:
  - diagnosis mode still works
  - product-help mode answers app questions
  - response renders consistently
- [ ] Commit backend and frontend changes separately.
