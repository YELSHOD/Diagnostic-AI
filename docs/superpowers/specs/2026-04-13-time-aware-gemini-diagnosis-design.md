# Time-Aware Gemini Diagnosis Design

## Goal

Make Gemini diagnosis explicitly aware of the currently selected time window in `Live Logs`, so the answer is grounded in a concrete interval and can explain the sequence of events inside that interval.

The current diagnosis flow already sends filtered log lines. This slice makes the time context first-class in both the request payload and the AI response structure.

## Problem

Right now the operator can ask a good question, but the AI request does not explicitly tell the backend:
- which time interval the operator is investigating
- which filters shaped the visible buffer
- that the answer should be organized as a timeline within that interval

This weakens answers for questions like:
- "What happened between 11:20 and 11:35?"
- "Why did the error start in the last 15 minutes?"
- "What happened before the websocket disconnect?"

## Scope

Included:
- extend frontend diagnose payload with time-window and filter context
- extend backend request contract to accept that context
- make the Gemini prompt explicitly time-aware
- return a richer structured diagnosis response
- update frontend rendering to show the richer diagnosis blocks

Not included:
- full incident persistence
- arbitrary server-side historical search across the whole log file
- multi-turn AI chat memory
- separate AI page redesign

## UX Model

### Trigger model

The operator still launches AI from `Live Logs`.

The active `time range` on the page becomes the investigation window. No second time picker is introduced inside the AI panel.

This keeps the flow coherent:
- set filters
- set time window
- ask question
- get an answer about that exact window

### Request context shown to the user

The AI panel should make this explicit:
- selected target
- active time range summary
- current level filter, if any
- current text filter, if any
- count of visible lines sent to AI

This avoids the feeling that the request is detached from the current investigation state.

## Request Contract

Frontend should send:
- `service`
- `question`
- `logLines`
- `timeRange`
  - `mode`: `all` | `relative` | `custom`
  - `label`: UI summary like `Showing: 15m`
  - `from`: ISO timestamp or `null`
  - `to`: ISO timestamp or `null`
- `levelFilter`: current selected level or empty
- `textFilter`: current text filter or empty

This gives the backend enough context to build a prompt that is explicit about the investigation window.

## Prompt Design

The prompt must instruct Gemini to:
- analyze only the supplied interval
- treat timestamps as the primary ordering signal
- identify notable events before, during, and after the main failure point inside the interval
- separate evidence from inference
- answer the user's concrete question
- produce a concise operational diagnosis, not a generic essay

The prompt should also tell Gemini to return structured JSON with:
- `summary`
- `timeline`
- `probableRootCause`
- `evidence`
- `nextChecks`
- `rawText`

## Response Contract

Backend should return a richer response:
- `provider`
- `model`
- `promptVersion`
- `summary`
- `timeline`
  - ordered items with time and event explanation
- `probableRootCause`
- `evidence`
  - supporting observations tied to logs in the interval
- `nextChecks`
  - actionable follow-up checks
- `rawText`

This preserves a usable UI structure while still keeping the raw model output available.

## Frontend Rendering

The inline AI result panel on `Live Logs` should render:
- summary block
- compact timeline list
- probable root cause block
- evidence bullets
- next checks bullets
- collapsible raw response

The result should visually feel like an investigation artifact attached to the current console session.

## Behavioral Rules

- The active page time range is the source of truth.
- If the active time range is `All streamed`, frontend still sends the available visible buffer and marks the mode as `all`.
- `Clear console` does not clear diagnosis results automatically.
- Changing filters or time range after a diagnosis does not auto-rerun AI.
- The operator must explicitly run diagnosis again to get a new answer.

## Error Handling

Existing provider/config/auth failures remain unchanged.

Additional validation:
- if `custom` range is malformed, frontend blocks submit
- if there are no visible lines in the selected interval, frontend blocks submit and explains why

## Success Criteria

This slice is successful when:
- the AI request includes explicit time-window and filter context
- backend prompt uses that context to produce interval-aware analysis
- frontend shows a structured timeline-oriented answer
- user can ask time-specific questions and receive answers tied to the chosen window
