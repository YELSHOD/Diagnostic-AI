# Live Logs Time Range Filter Design

## Goal

Add an Elastic-style time range filter to `Live Logs` so the operator can quickly narrow the currently streamed buffer by time without leaving the page or changing the backend websocket contract.

## Scope

Included:
- quick time ranges in `Live Logs`
- custom `from/to` time range inputs
- combined filtering with existing `text` and `level` filters
- empty-state and helper messaging that explain this is a client-side streamed-buffer filter

Not included:
- backend history endpoint
- fetching old logs by date range
- time-range controls on other pages

## UX Model

### Default state

The page opens with:
- `All streamed` selected
- all currently buffered lines visible
- no hidden time filtering

This is the honest default for a live console, because the page only knows about logs already loaded into the browser session.

### Quick ranges

The toolbar gets compact quick-range chips:
- `All streamed`
- `5m`
- `15m`
- `1h`
- `6h`
- `24h`

Only one quick range is active at a time unless the user switches to a custom range.

### Custom range

`Custom range` opens a compact inline panel or popover with:
- `From`
- `To`
- `Apply`
- `Reset`

The panel should feel like an ops-console control, not a large modal dialog.

### Filtering semantics

Time filtering runs against the already loaded client buffer:
- websocket lines already received in the current session
- bootstrap history already loaded into the page if present

It does **not** request more history from the backend.

This must be explained clearly in the UI with helper text such as:

`Time range filters only the streamed logs already loaded in this session.`

## Filtering Rules

The final visible result is the intersection of:
- time range
- level filter
- text filter

Order does not matter logically, but implementation should keep it simple and readable.

### All streamed

No time restriction.

### Relative ranges

For `5m`, `15m`, `1h`, `6h`, `24h`:
- compute `now - range`
- show only lines whose `ts` falls within that interval

### Custom range

For a custom interval:
- `from` and `to` are optional until apply
- invalid or incomplete values should not silently apply
- if both are present, `from <= to` must hold
- `Apply` activates the custom range
- `Reset` returns the control to `All streamed`

## Empty State

If the active time range produces zero matching lines, the page should say that clearly.

Recommended message direction:
- no logs match the selected time range in the current streamed buffer

This should be distinct from the broader empty state where there are no lines at all.

## Visual Direction

The control should match the refreshed ops-console styling:
- compact chips
- small labels
- dense spacing
- clear active state
- dark console remains dark in both themes

The time filter must not make the toolbar bulky again.

## Implementation Notes

Likely frontend touch points:
- `LiveLogsPage.tsx`
- `LiveLogsPage.test.tsx`
- `globals.css`
- `messages.ts`

The implementation should keep the filtering logic local to the page unless reuse becomes obviously necessary.

## Success Criteria

This feature is successful when:
- operators can switch between `All streamed`, quick ranges, and a custom interval
- the filtered results update predictably without backend changes
- the UI makes it clear that this is filtering the current client buffer
- the toolbar stays compact and visually aligned with the refreshed console
