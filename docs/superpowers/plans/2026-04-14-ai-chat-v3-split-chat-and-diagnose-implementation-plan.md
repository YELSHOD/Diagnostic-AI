# AI Chat v3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split conversational AI chat from structured log diagnosis, add a dedicated `/api/ai/chat` backend endpoint, and remove visible `Gemini` branding from the UI.

**Architecture:** Keep `Live Logs` on the structured diagnosis pipeline, but add a separate chat pipeline with its own request/response contracts and prompt builder. Frontend `AI Chat` becomes a plain assistant conversation UI that talks only to `/api/ai/chat`.

**Tech Stack:** Spring Boot, Jackson, Jakarta Validation, React, TypeScript, Vitest, Vite.

---

### Task 1: Add backend chat contract and prompt layer

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiChatRequest.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiChatResponse.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/ChatPromptFactory.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/ChatPromptFactoryTest.java`

- [ ] **Step 1: Write the failing prompt test**
- [ ] **Step 2: Run backend AI prompt tests and verify the new chat test fails**
- [ ] **Step 3: Implement request/response records and chat prompt builder**
- [ ] **Step 4: Run prompt tests and make them pass**
- [ ] **Step 5: Commit**

### Task 2: Add backend `/api/ai/chat` service and controller

**Files:**
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiChatService.java`
- Create: `src/main/java/com/yelshod/diagnosticserviceai/ai/AiChatController.java`
- Modify: `src/main/java/com/yelshod/diagnosticserviceai/ai/HttpGeminiClient.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/AiChatServiceTest.java`
- Test: `src/test/java/com/yelshod/diagnosticserviceai/ai/AiChatControllerTest.java`

- [ ] **Step 1: Write failing service/controller tests for `/api/ai/chat`**
- [ ] **Step 2: Run the targeted backend tests and verify failure**
- [ ] **Step 3: Implement chat service/controller and Gemini call path**
- [ ] **Step 4: Run the targeted backend AI tests and make them pass**
- [ ] **Step 5: Commit**

### Task 3: Move frontend AI Chat page to `/api/ai/chat`

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/AiChatPage.tsx`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/AiChatPage.test.tsx`

- [ ] **Step 1: Write/update failing frontend tests for the new chat endpoint contract**
- [ ] **Step 2: Run `AiChatPage` tests and verify failure**
- [ ] **Step 3: Implement frontend chat API client and page rendering for plain-text assistant responses**
- [ ] **Step 4: Run `AiChatPage` tests and make them pass**
- [ ] **Step 5: Commit**

### Task 4: Remove visible `Gemini` branding from UI and preserve diagnosis flow

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] **Step 1: Update UI copy/tests to expect `AI Assistant` instead of `Gemini`**
- [ ] **Step 2: Run targeted frontend tests and verify any copy failures**
- [ ] **Step 3: Implement copy/styling cleanup while keeping `Live Logs` on diagnosis**
- [ ] **Step 4: Run targeted frontend tests and build**
- [ ] **Step 5: Commit**
