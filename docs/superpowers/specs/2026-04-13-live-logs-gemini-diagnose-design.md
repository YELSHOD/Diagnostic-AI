# Live Logs Gemini Diagnose Design

## Goal

Let the operator run Gemini diagnosis directly from `Live Logs` using the current runtime target, the current filtered log buffer, and a free-form question.

This should be the first frontend integration of the backend `POST /api/ai/diagnose` endpoint.

## Scope

Included:
- `Diagnose with Gemini` action on `Live Logs`
- question input flow
- request payload built from current page context
- loading state
- rendered AI result panel on the same page
- graceful handling for backend/config/provider errors

Not included:
- full conversational AI chat
- persistent diagnosis history
- multi-turn context
- custom selection of arbitrary log rows

## UX Model

### Entry point

`Live Logs` gets a compact action:
- `Diagnose with Gemini`

This should feel like an investigation action, not a navigation to a different product area.

### Input model

When activated, the page opens a compact inline panel or drawer with:
- current target shown read-only
- question input
- helper note that the request uses the latest visible log lines
- submit action
- cancel action

### Request payload

Frontend sends:
- `service`: selected runtime target name
- `question`: user text
- `logLines`: last `50` visible log lines from the already filtered buffer

The log lines should reflect:
- current text filter
- current level filter
- current time range filter

That keeps the AI request aligned with what the operator is actually looking at.

## Result Presentation

The result should stay on `Live Logs` as a compact AI diagnosis panel.

Recommended structure:
- provider/model meta
- summary
- bullets
- collapsible raw response text

This should feel like a diagnosis attachment to the investigation screen, not like a separate chat transcript.

## Loading and Errors

### Loading

Show a clear pending state while the request is in flight:
- disable repeated submit
- show spinner/text such as `Diagnosing...`

### Empty input

If question is blank:
- block submit
- show validation hint

### No visible logs

If there are no visible lines after filters:
- still allow submit only if product wants question-only mode
- for this slice, prefer requiring at least one visible log line
- show a clear message if there is nothing to send

### Backend/config/provider failure

If backend returns:
- `503`: Gemini is not configured
- `400`: invalid request
- `502` or other failure: provider/backend error

Show a readable inline error message in the diagnosis panel or form area.

## Visual Direction

The new controls should match the ops-console refresh:
- compact action button
- drawer/panel aligned with `Live Logs`
- no giant modal
- readable but dense result panel

The AI panel should visually differ from raw incident details, but stay in the same product language.

## State Behavior

- Diagnosis result belongs to the current page session only
- `Clear console` should not silently clear the AI result unless explicitly desired
- switching target or filters should not auto-rerun diagnosis
- operator must explicitly click diagnose again

## API Contract

Backend endpoint:
- `POST /api/ai/diagnose`

Expected response:
- `provider`
- `model`
- `promptVersion`
- `summary`
- `bullets`
- `rawText`

Frontend should consume this shape directly.

## Success Criteria

This integration is successful when:
- operator can ask a question directly from `Live Logs`
- frontend sends the last `50` visible filtered lines
- backend response appears inline on the same screen
- errors are readable and not raw stack traces
- the feature feels like part of the investigation workflow, not a disconnected demo
