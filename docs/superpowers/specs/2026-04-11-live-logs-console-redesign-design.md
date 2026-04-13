# Live Logs Console Redesign

## Goal

Redesign the frontend `Live Logs` page so it behaves and reads like a developer console similar to IntelliJ IDEA rather than a dashboard with multiple competing cards.

The page should make the log stream the primary object on screen, with filtering and status controls staying accessible but visually secondary.

## Problem

The current `Live Logs` page uses a dashboard composition:
- KPI cards occupy the upper area
- the log stream is constrained inside a smaller card
- latest error details permanently compete for space
- the layout makes the page look layered and cramped instead of console-first

This creates two usability problems:
- the user cannot immediately understand where the main logs are supposed to be read
- the screen does not resemble the console mental model developers expect from IntelliJ IDEA or similar tools

## Scope

In scope:
- redesign the `Live Logs` page layout
- keep the existing websocket/store/data flow
- make the log viewport the dominant area
- keep filters and connection state in a compact toolbar
- move structured error details out of the main reading lane

Out of scope:
- backend protocol changes
- new websocket message types
- changing backend parsing or log transport
- redesigning unrelated pages

## Existing Context

The current frontend already has the required data:
- selected runtime target id from query params
- websocket-driven log lines
- level and text filtering
- connection state
- latest structured error

This means the work is a presentation and interaction redesign, not a data-model redesign.

## Design Direction

The page should become a console-style workspace with three visual layers:

1. a compact top toolbar for controls
2. a large main log viewport for continuous reading
3. an optional details panel for the latest structured error

The layout should visually prioritize the log stream above all other content.

## Recommended Approach

Use a single large console surface instead of preserving dashboard cards.

Why this approach:
- best matches the user expectation of “logs like IntelliJ IDEA”
- removes ambiguity about where to read logs
- reduces visual competition
- keeps the existing data flow intact

Rejected alternatives:
- preserving the KPI/dashboard layout and only enlarging the log card: still visually confused
- fixed split-pane with error panel always visible: useful for investigations, but less console-like
- tabbed console/errors layout: cleaner, but worse for quick live reading because details require mode switching

## Layout

### Top Toolbar

The top toolbar should include:
- selected runtime target name
- connected/disconnected state
- text filter
- level filter
- follow toggle
- clear button
- change target action

Rules:
- the toolbar stays compact and horizontally aligned where possible
- on smaller widths it can wrap, but the log viewport must still dominate the page
- controls must not visually read like independent dashboard cards

### Main Console Viewport

The main area should be a single large scrollable console panel.

Rules:
- use a monospace font
- compact row height
- dark console surface consistent with the existing theme
- readable separation between timestamp, level, service, and message
- the viewport should take most of the page height
- the page should feel like a log terminal first and a product page second

Each row should use this conceptual structure:
- time
- level
- service
- message

Example display shape:
`12:41:08.421 | INFO  | diagnosticserviceai | User login succeeded userId=...`

## Interaction Behavior

### Follow Mode

Default behavior:
- follow mode is on when the page opens
- new lines auto-scroll the viewport to the bottom

When the user scrolls upward:
- follow mode is disabled automatically
- new lines continue arriving but should not force-scroll

When the user re-enables follow:
- viewport jumps back to the latest lines

### Clear Behavior

The clear action should:
- clear only the client-side visible buffer for the current page session
- not affect backend logs
- not affect websocket connectivity
- not truncate files or server-side buffers

### Filtering

Filtering remains local in the client:
- text filter matches message content
- level filter matches parsed level

Filtering should feel immediate and must not rearrange the page structure.

## Structured Error Details

The latest structured error should not permanently consume half the screen.

Recommended behavior:
- render it as a collapsible panel below or attached to the console surface
- default to collapsed when there is no error
- expand when the user wants investigation details

It should include:
- exception type
- service
- message
- event time
- trace id if present
- top frames

This keeps structured diagnostics available without damaging the console reading flow.

## Empty State

If no target is selected:
- show a direct empty-state message that tells the user to choose a runtime target

If a target is selected but no lines are available yet:
- show a console-style empty state inside the viewport
- the message should say that logs will appear here once the target emits lines

The empty state should not look like a failed dashboard card.

## Files To Change

Primary files:
- `src/pages/LiveLogsPage.tsx`
- `src/app/styles/globals.css`

No data contract changes are required.

## Technical Constraints

- Preserve the existing store and websocket hook usage
- Preserve existing level parsing and structured error handling
- Avoid unnecessary component extraction in the first pass
- Prefer implementing the redesigned page in place unless the file becomes materially harder to understand

## Acceptance Criteria

- `Live Logs` clearly presents logs in one dominant console viewport
- dashboard KPI cards are removed from this page
- the page visually resembles a developer console more than a dashboard
- users can immediately identify where to read live logs
- follow mode works as expected
- filters remain available and readable
- latest structured error remains accessible without permanently taking major horizontal space
- the redesign works on desktop and remains usable on mobile widths
