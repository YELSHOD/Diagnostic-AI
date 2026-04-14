# AI Chat V2 ChatGPT-Style Design

## Goal

Redesign `AI Chat` from a form-based tools page into a chat-first assistant experience closer to ChatGPT:

- one conversation surface
- message history
- input pinned at the bottom
- no explicit `mode` switch
- no explicit target picker in the main flow
- assistant decides whether the question is about logs/diagnosis or about product usage

## Problem With V1

`AI Chat v1` is functional, but still feels like an operator form:
- user has to think about mode
- user has to think about target selection
- the UI feels like submitting a request, not chatting
- response appears as structured cards, not as a natural assistant answer

This is useful for engineering validation, but not the UX you want.

## UX Direction

## Core model

The page should behave like a single assistant:
- the operator types one question
- the assistant answers in one conversational bubble
- the system automatically decides whether the request is:
  - product help
  - diagnosis

## Layout

- top: simple page intro
- middle: message thread
- bottom: pinned chat composer

The page should feel closer to ChatGPT:
- large empty welcome state
- user message bubble
- assistant message bubble
- typing/loading state while waiting
- no scattered cards and no multi-pane operator form

## Context Strategy

### Automatic routing

The backend or frontend prompt layer should infer:
- if the user asks about pages, settings, password, navigation, features -> `product_help`
- if the user asks about errors, logs, runtime target behavior, timelines -> `diagnosis`

### Automatic log context

If chat is opened from `Live Logs` or if there is known current investigation context:
- automatically include the active target
- automatically include recent filtered log lines
- automatically include time range/filter context

If no live investigation context exists:
- behave as a product-help assistant by default
- do not force the user to supply logs

## Backend Strategy

Keep one backend endpoint for now:
- `POST /api/ai/diagnose`

Payload can still carry:
- `mode`
- `service`
- `question`
- `logLines`
- `timeRange`
- `levelFilter`
- `textFilter`

But `AI Chat v2` should infer `mode` automatically in the client before sending.

This keeps backend stable while simplifying UX.

## Response Rendering

The assistant should render one cohesive answer bubble first.

Inside that bubble we can still structure the content:
- summary paragraph
- short bullet sections for timeline/evidence/next checks

But it must read like one assistant reply, not like multiple dashboard cards.

`rawText` should stay hidden behind an optional disclosure, not as a primary UI element.

## Loading Behavior

The loading state should feel like a chat assistant:
- pending assistant bubble
- subtle animated placeholder / typing state
- no fragmented partial render
- final answer appears as one completed assistant message

## Scope

Included:
- chat-style `AI Chat` redesign
- message history within current page session
- automatic mode inference on the client
- optional auto-use of log context when available
- chat-like loading state

Not included:
- server-side conversation persistence
- streaming token-by-token output
- separate `/api/ai/chat` backend endpoint
- cross-session history

## Success Criteria

This slice is successful when:
- `AI Chat` looks and feels like a real chat assistant
- operator no longer has to choose `Diagnosis` vs `Product Help`
- operator no longer has to manually choose a target in the main flow
- diagnosis questions still work
- product-help questions still work
- responses appear as cohesive assistant replies with chat-style loading
