# AI Chat V1 Design

## Goal

Introduce a real `AI Chat` page that is useful for two different operator jobs:

- `Diagnosis`: ask about logs, runtime targets, and incidents
- `Product Help`: ask about how the application works and where to perform actions

The first version should reuse the existing backend `POST /api/ai/diagnose` endpoint instead of inventing a second AI transport prematurely.

## Why

The current inline Gemini diagnose flow in `Live Logs` is useful, but narrow:
- it is tied to the logs screen
- it assumes the user is investigating runtime events
- it cannot answer product-help questions like:
  - where to change password
  - where to configure API/WS URL
  - what `Runtime targets` means
  - how demo scenarios work

`AI Chat v1` should become a more general AI assistant for the application while still supporting diagnosis.

## Scope

Included:
- dedicated `AI Chat` page
- mode switch between `Diagnosis` and `Product Help`
- reuse of the existing `/api/ai/diagnose` backend endpoint
- frontend payload extended with `mode`
- backend prompt behavior extended for `product_help`
- product knowledge context embedded in backend prompt construction

Not included:
- multi-turn chat memory
- session persistence
- separate `/api/ai/chat` endpoint
- vector search / RAG / embeddings
- user-uploaded documents

## UX Model

### Page structure

`AI Chat` becomes a real working screen with:
- compact intro
- mode switch:
  - `Diagnosis`
  - `Product Help`
- target selector
- optional context summary
- question textarea
- answer panel

### Diagnosis mode

This mode is for:
- log interpretation
- error explanation
- timeline investigation
- root cause analysis

The page should:
- optionally use the currently selected runtime target
- optionally reuse the current log context if entered from `Live Logs`
- send:
  - `mode = diagnosis`
  - `service`
  - `question`
  - `logLines`
  - `timeRange`
  - `levelFilter`
  - `textFilter`

### Product Help mode

This mode is for:
- application usage questions
- navigation questions
- explanation of features
- operational guidance inside this product

Examples:
- where do I change password
- what is the Settings page for
- how do I open live logs for a target
- how do demo targets work

This mode should send:
- `mode = product_help`
- `question`
- optional `service`
- no required log lines

## Backend Prompt Strategy

## Shared endpoint

Keep one backend endpoint for now:
- `POST /api/ai/diagnose`

The backend decides prompt behavior based on `mode`.

## Diagnosis prompt

For `diagnosis`, retain current behavior:
- investigate logs and time window
- return structured diagnosis

## Product help prompt

For `product_help`, inject a compact application knowledge block that describes:
- pages:
  - Overview
  - Runtime targets
  - Live Logs
  - Analysis
  - Settings
  - Account
  - AI Chat
- common actions:
  - login/register
  - change password
  - configure API/WS URL
  - open logs for a target
  - run demo scenarios
- constraints:
  - do not invent features not implemented
  - clearly say when something is not available yet
  - answer in the user’s language

This is prompt-engineered product knowledge, not fine-tuning.

## Response Shape

Keep the current response contract for both modes:
- `provider`
- `model`
- `promptVersion`
- `summary`
- `timeline`
- `probableRootCause`
- `evidence`
- `nextChecks`
- `rawText`

For `product_help`, fields map as:
- `summary`: direct answer
- `timeline`: optional steps or navigation path
- `probableRootCause`: optional warning or main explanation
- `evidence`: product facts used for the answer
- `nextChecks`: what the user should do next

This avoids a second response contract.

## Frontend Behavior

### Diagnosis mode UI

- target selector visible
- context summary visible
- button to reuse current `Live Logs` context if available
- answer card structured like current diagnosis output

### Product help UI

- simpler form
- target selector optional
- no dependence on log lines
- answer card still uses the same structured response render

## Navigation and Entry Points

- sidebar `AI Chat` becomes active, working page
- from `Live Logs`, a secondary action may open `AI Chat` with diagnosis context prefilled later
- initial implementation does not need deep linking from every page

## Success Criteria

This slice is successful when:
- `AI Chat` is a real working screen
- user can switch between `Diagnosis` and `Product Help`
- diagnosis mode works with the existing AI diagnosis flow
- product help mode can answer app-usage questions like password/settings/navigation
- backend still uses one endpoint and one response contract
