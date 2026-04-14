# Ops Console UI Refresh Design

## Goal

Redesign the frontend experience around the most visibly weak operational screens so the product feels like a coherent observability tool instead of a raw prototype.

This refresh focuses on:
- `Live Logs`
- `Settings`
- global light-theme polish

The redesign should improve readability, density, and interaction safety without replacing the product's existing information architecture.

## Scope

Included:
- `Live Logs` layout and controls
- `Settings` interaction model and visual structure
- shared light-theme fixes and spacing/typography tuning
- global font-size tightening for data-heavy screens

Not included:
- full redesign of `Overview`, `Analytics`, `Runtime targets`, `Auth`, or sidebar structure
- backend API changes except if tiny frontend-safe labels/help text are required

## Visual Direction

The product should keep its current dark-navy and teal identity, but become much cleaner and more disciplined.

### Dark theme

- deep navy background
- console viewport remains dark and operational
- borders become quieter and more intentional
- controls are compact and tool-like rather than oversized

### Light theme

- use a cool light workspace instead of flat white
- keep strong panel definition with subtle surfaces
- preserve a dark log viewport even in light theme
- fix contrast failures where dark-on-dark or low-contrast text becomes unreadable

### Typography

- reduce font sizes by roughly one step on dense screens
- use smaller labels, tighter table rows, and more compact controls
- make hierarchy rely more on spacing and weight than oversized text

## Live Logs Redesign

### Problems in current screen

- toolbar controls are oversized and visually heavy
- `Paused` and `Clear` look like full-width buttons rather than lightweight console controls
- spacing is too tall for a log-focused tool
- console footer and error details consume too much visual weight
- light theme inherits styles that make some areas look broken or washed out

### Desired layout

`Live Logs` becomes a compact operations console:

- top control bar
- main log viewport
- compact footer status row
- collapsible error details area

### Toolbar behavior

- selected target and connection state stay visible
- `Paused` becomes a compact toggle/chip instead of a large button
- `Clear` becomes a compact secondary action
- `Change target` stays available but visually de-emphasized
- text filter and level filter remain primary controls

### Console behavior

- keep the four-column model: `Level | Time | Source | Message`
- use a sticky column header
- reduce row height and padding
- keep `Message` as the widest column
- truncate `Source` safely
- preserve wrapping for long messages

### Error details

- latest error stays collapsed by default
- details panel opens on demand
- panel should look like incident details, not a second dashboard card

### Light theme rule

The log viewport remains dark in both themes. Only the outer page chrome changes between dark and light themes.

This preserves console readability and avoids a washed-out observability screen.

## Settings Redesign

### Problems in current screen

- values save too implicitly
- users cannot tell whether they changed draft values or active settings
- it is easy to break the frontend transport accidentally
- the page looks like a raw form rather than an operational control panel

### Desired interaction model

`Settings` should use a draft state.

- typing into fields changes local form state only
- values apply only after pressing `Save`
- explicit actions:
  - `Save`
  - `Reset`
  - `Reset to defaults`
- show save state:
  - `Unsaved changes`
  - `Saved`

### Information architecture

Split the page into two sections:

- `Connection`
  - API Base URL
  - WS Base URL
- `Reconnect behavior`
  - reconnect min
  - reconnect max

### Safety messaging

The page must clearly explain:

- these values control where the frontend connects for REST and WebSocket
- they do not change runtime target ports

Recommended helper copy:

`These values control where the frontend connects for REST and WebSocket. They do not change runtime target ports.`

### Presets

Optional quick presets may be added for common local cases:

- `Local 8080`
- `Local 8081`

They should update draft fields but still require explicit save.

## Shared UI Polish

Apply the following globally where relevant:

- smaller control heights
- smaller table/log fonts
- tighter vertical spacing
- calmer borders
- more reliable light-theme contrast

The goal is not to make the app minimal for its own sake. The goal is to make it feel like a serious ops console.

## Testing Strategy

Frontend verification should include:

- `Live Logs` renders compact toolbar and sticky header layout correctly
- `Paused` appears as a compact state control rather than a large block button
- `Settings` uses draft state and explicit save/reset actions
- light theme keeps the log viewport readable and no longer produces broken low-contrast sections

## Success Criteria

This redesign is successful when:

- `Live Logs` feels dense, readable, and deliberate
- the current oversized controls are removed or visually reduced
- `Settings` is explicit and safe to use
- light theme no longer looks visually broken
- the product reads as one coherent observability workspace rather than unrelated prototype pages
