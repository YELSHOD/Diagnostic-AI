# Live Logs Console Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the frontend `Live Logs` page into a single dominant console-like workspace that reads like IntelliJ IDEA while preserving the current websocket/store/backend data flow.

**Architecture:** Keep the existing realtime data pipeline and redesign only the presentation and local interaction behavior of `LiveLogsPage`. Implement the new console layout in place first, add follow-mode and client-side clear behavior on top of the existing Zustand store, and use CSS to make the console viewport visually dominant without introducing unnecessary component churn.

**Tech Stack:** React, TypeScript, React Router, Zustand, TanStack Query, Vitest, Testing Library, existing global CSS

---

## File Map

- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
  - Replace dashboard layout with console-first layout, toolbar, follow toggle, clear action, and collapsible error details.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
  - Add focused tests for console rendering, clear behavior, and target display.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
  - Add console-specific layout and row styling.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/realtime/store.ts` only if a page-local clear helper needs better support from the store.
  - Prefer using existing `clearStream` directly; do not change data contracts unless necessary.

### Task 1: Lock In Console Rendering Expectations

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] **Step 1: Expand the page tests to describe the new console-first layout**

Add tests that assert:
```tsx
it("shows the selected runtime target in the console toolbar", () => {
  expect(screen.getByText(/selected runtime target: diagnosticserviceai/i)).toBeInTheDocument();
});

it("renders a console viewport instead of dashboard KPI cards", () => {
  expect(screen.getByTestId("logs-console")).toBeInTheDocument();
  expect(screen.queryByText(/visible lines/i)).not.toBeInTheDocument();
  expect(screen.queryByText(/error events/i)).not.toBeInTheDocument();
});
```

Expected: test file now encodes the intended visual direction before implementation.

- [ ] **Step 2: Run the page test to verify RED**

Run:
```bash
cd /Users/admin/IdeaProjects/Diagnostic-AI-front
npm test -- LiveLogsPage
```

Expected: FAIL because the current component still renders KPI/dashboard content and lacks `data-testid="logs-console"`.

- [ ] **Step 3: Commit the failing test checkpoint only if working in isolated TDD commits**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx
git commit -m "test: define live logs console layout"
```

Expected: optional red-phase checkpoint exists, or this step is skipped intentionally.

### Task 2: Replace Dashboard Layout With Console Layout

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] **Step 1: Rewrite the page structure around a single console viewport**

Implement a structure shaped like:
```tsx
<section className="logs-console-page">
  <header className="logs-console-toolbar">
    <div className="logs-console-target">{runtimeTargetLabel}</div>
    <div className="logs-console-status">{connected ? "Connected" : "Disconnected"}</div>
    <input ... />
    <select ... />
    <button type="button">Follow</button>
    <button type="button">Clear</button>
    <Link ...>Change target</Link>
  </header>

  <section className="logs-console-shell">
    <div className="logs-console-viewport" data-testid="logs-console">
      {filtered.map(...)}
    </div>
  </section>
</section>
```

Expected: KPI cards and the current two-column dashboard structure are removed from the page.

- [ ] **Step 2: Keep the existing data flow unchanged**

Preserve:
```tsx
const logs = useRealtimeStore((s) => s.logs);
const errors = useRealtimeStore((s) => s.errors);
const connected = useRealtimeStore((s) => s.connected);
useLogsSocket({ runtimeTargetId, wsBaseUrl, reconnectMinMs, reconnectMaxMs });
```

Expected: only layout and page-local interaction change; websocket/store contracts remain untouched.

- [ ] **Step 3: Re-run the page test to verify GREEN for layout**

Run:
```bash
cd /Users/admin/IdeaProjects/Diagnostic-AI-front
npm test -- LiveLogsPage
```

Expected: PASS for the console-layout assertions.

- [ ] **Step 4: Commit the console layout**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx
git commit -m "feat: convert live logs page to console layout"
```

### Task 3: Add Follow Mode And Client-Side Clear

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/realtime/store.ts` only if absolutely needed

- [ ] **Step 1: Add tests for clear and follow controls**

Add tests like:
```tsx
it("clears the client-side stream when clear is pressed", async () => {
  useRealtimeStore.setState({
    logs: [{ ts: "2026-04-11T12:00:00Z", service: "diagnosticserviceai", payload: { level: "INFO", message: "hello" } }],
    errors: [],
    clusters: {},
    connected: true,
    selectedContainerId: "local-backend"
  });

  renderPage();
  await user.click(screen.getByRole("button", { name: /clear/i }));

  expect(screen.queryByText(/hello/i)).not.toBeInTheDocument();
});
```

Expected: tests define the page-local behavior before implementation.

- [ ] **Step 2: Run the focused page test to verify RED**

Run:
```bash
cd /Users/admin/IdeaProjects/Diagnostic-AI-front
npm test -- LiveLogsPage
```

Expected: FAIL because clear/follow controls are not fully implemented yet.

- [ ] **Step 3: Implement follow-state and clear behavior with minimal code**

Implement page-local state shaped like:
```tsx
const clearStream = useRealtimeStore((s) => s.clearStream);
const [follow, setFollow] = useState(true);
const consoleRef = useRef<HTMLDivElement | null>(null);

useEffect(() => {
  if (follow && consoleRef.current) {
    consoleRef.current.scrollTop = consoleRef.current.scrollHeight;
  }
}, [filtered, follow]);
```

Add:
```tsx
<button type="button" onClick={() => setFollow((value) => !value)}>
  {follow ? "Following" : "Paused"}
</button>
<button type="button" onClick={clearStream}>Clear</button>
```

Expected: clear empties the visible stream and follow mode is present without backend changes.

- [ ] **Step 4: Re-run the page tests**

Run:
```bash
cd /Users/admin/IdeaProjects/Diagnostic-AI-front
npm test -- LiveLogsPage
```

Expected: PASS

- [ ] **Step 5: Commit interaction behavior**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx
git commit -m "feat: add live logs follow and clear controls"
```

### Task 4: Add Console Styling And Row Presentation

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`

- [ ] **Step 1: Add console-specific CSS classes**

Implement styles for:
```css
.logs-console-page {}
.logs-console-toolbar {}
.logs-console-shell {}
.logs-console-viewport {}
.logs-console-row {}
.logs-console-time {}
.logs-console-level {}
.logs-console-service {}
.logs-console-message {}
.logs-console-empty {}
.logs-console-error-panel {}
```

Rules:
```text
- viewport dominates page height
- monospace font for rows
- compact spacing
- sticky toolbar if helpful
- no dashboard-card feel in the main reading lane
```

Expected: the screen visually reads like a console.

- [ ] **Step 2: Change row markup to a structured console format**

Render rows in a shape like:
```tsx
<div className={`logs-console-row log-${line.payload.level ?? "INFO"}`}>
  <span className="logs-console-time">{formatTime(line.ts)}</span>
  <span className="logs-console-level">{line.payload.level ?? "-"}</span>
  <span className="logs-console-service">{line.service}</span>
  <span className="logs-console-message">{line.payload.message}</span>
</div>
```

Expected: logs are easier to scan and feel closer to IntelliJ-style console output.

- [ ] **Step 3: Run the page tests**

Run:
```bash
cd /Users/admin/IdeaProjects/Diagnostic-AI-front
npm test -- LiveLogsPage
```

Expected: PASS

- [ ] **Step 4: Commit console styling**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css
git commit -m "feat: style live logs as console"
```

### Task 5: Move Latest Error Into Collapsible Details Panel

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] **Step 1: Add a failing test for collapsible error details**

Add a test like:
```tsx
it("keeps latest error details out of the main console until expanded", async () => {
  useRealtimeStore.setState({
    logs: [],
    errors: [{
      ts: "2026-04-11T12:00:00Z",
      service: "diagnosticserviceai",
      payload: {
        exceptionType: "java.lang.IllegalStateException",
        message: "boom",
        eventTime: "2026-04-11T12:00:00Z",
        traceId: null,
        topFrames: ["frame1"],
        clusterKey: "cluster-1"
      }
    }],
    clusters: {},
    connected: true,
    selectedContainerId: "local-backend"
  });

  renderPage();

  expect(screen.queryByText(/java.lang.IllegalStateException/i)).not.toBeVisible();
});
```

Expected: FAIL because the current page shows latest error content directly.

- [ ] **Step 2: Implement a collapsible details section**

Use a local state shape like:
```tsx
const [errorPanelOpen, setErrorPanelOpen] = useState(false);
```

Render:
```tsx
<button type="button" onClick={() => setErrorPanelOpen((v) => !v)}>
  {errorPanelOpen ? "Hide latest error" : "Show latest error"}
</button>
{errorPanelOpen && latestError ? <section className="logs-console-error-panel">...</section> : null}
```

Expected: structured diagnostics remain available without permanently taking half the page width.

- [ ] **Step 3: Re-run the page tests**

Run:
```bash
cd /Users/admin/IdeaProjects/Diagnostic-AI-front
npm test -- LiveLogsPage
```

Expected: PASS

- [ ] **Step 4: Commit error details panel behavior**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx
git commit -m "feat: collapse live logs error details"
```

### Task 6: Final Verification

**Files:**
- Modify: no files unless verification reveals an issue
- Test: page tests and manual browser verification

- [ ] **Step 1: Run the focused frontend test suite**

Run:
```bash
cd /Users/admin/IdeaProjects/Diagnostic-AI-front
npm test -- LiveLogsPage RuntimeTargetsPage
```

Expected: PASS

- [ ] **Step 2: Run the frontend locally and verify the console manually**

Run:
```bash
cd /Users/admin/IdeaProjects/Diagnostic-AI-front
npm run dev
```

Manual checks:
```text
- Open Live Logs with a selected runtime target
- Verify the log viewport dominates the page
- Verify new log lines appear in the console area
- Verify follow mode can be toggled
- Verify clear only clears the client-side view
- Verify latest error details stay out of the main reading lane until opened
- Verify mobile/narrow widths still keep the console readable
```

- [ ] **Step 3: Commit the verified redesign**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css
git commit -m "feat: redesign live logs as console"
```

## Self-Review

- Spec coverage:
  - console-first layout: Tasks 1 and 2
  - follow mode and clear: Task 3
  - console styling and row structure: Task 4
  - collapsible latest error details: Task 5
  - manual and automated verification: Task 6
- Placeholder scan:
  - no `TODO` or `TBD` placeholders remain
  - every task includes exact files and commands
- Type consistency:
  - references existing `useRealtimeStore`, `useLogsSocket`, and `useRuntimeTargets`
  - keeps the existing page-level data flow and only changes layout/interaction
