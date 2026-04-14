# AI Chat V2 ChatGPT-Style Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign `AI Chat` into a ChatGPT-style assistant with one input flow, automatic mode inference, optional automatic diagnosis context, and cohesive assistant-style responses.

**Architecture:** Keep the existing backend `POST /api/ai/diagnose` endpoint and current structured response contract. Simplify the UX in the frontend by inferring `mode` in the chat page, storing a local thread of messages, and rendering responses as assistant bubbles instead of as form/result cards. The backend contract remains stable; the frontend becomes the main orchestrator for intent inference and session-local history.

**Tech Stack:** React, TypeScript, existing frontend routing, existing Gemini diagnose API helper, Vitest, Testing Library, CSS in `globals.css`

---

## File Structure

- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/AiChatPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/AiChatPage.test.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
- Optionally modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts` only if request helper types need small support updates

## Task 1: Replace Mode Form With Chat Thread

**Files:**
- `AiChatPage.tsx`
- `AiChatPage.test.tsx`

- [ ] Replace the current form-card layout with:
  - empty welcome state
  - chat message list
  - bottom composer
- [ ] Keep messages page-local only.
- [ ] Add tests that verify:
  - user message appears in the thread
  - assistant answer appears as one assistant bubble
  - empty state disappears after first message
- [ ] Run `npm test -- AiChatPage` and confirm the red/green cycle.

## Task 2: Add Automatic Mode Inference

**Files:**
- `AiChatPage.tsx`
- `AiChatPage.test.tsx`

- [ ] Implement a small frontend intent heuristic:
  - product-help terms such as password/settings/page/how do I/where is
  - diagnosis terms such as logs/error/exception/why failed/runtime target/trace
- [ ] Default to `product_help` when no diagnosis signals are present.
- [ ] Update tests to assert:
  - product-help questions send `mode: product_help`
  - diagnosis questions send `mode: diagnosis`
- [ ] Keep the backend endpoint unchanged.

## Task 3: Add Automatic Context Handling

**Files:**
- `AiChatPage.tsx`
- `AiChatPage.test.tsx`

- [ ] Support optional diagnosis context:
  - if available, include target/logLines/timeRange automatically
  - if not available, still allow the question to run
- [ ] Do not expose target selection in the primary UX.
- [ ] Provide a small inline note when diagnosis context is auto-attached.
- [ ] Add tests for both:
  - diagnosis with auto context
  - product-help without context

## Task 4: Improve Loading and Answer Rendering

**Files:**
- `AiChatPage.tsx`
- `messages.ts`
- `globals.css`
- `AiChatPage.test.tsx`

- [ ] Add chat-like pending assistant bubble / typing state.
- [ ] Render answer as one assistant message with structured inner sections:
  - summary
  - timeline
  - evidence
  - next checks
- [ ] Hide raw response behind a disclosure.
- [ ] Ensure the final answer appears as one cohesive reply, not fragmented cards.
- [ ] Add tests for loading bubble and final answer bubble.

## Task 5: Verify and Commit

**Files:**
- frontend working tree

- [ ] Run `npm test -- AiChatPage`
- [ ] Run `npm run build`
- [ ] Manually sanity-check:
  - product-help question
  - diagnosis question
  - loading state
- [ ] Commit frontend changes separately after verification.
