# Live Logs Time Range Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add compact Elastic-style quick ranges and a custom time interval filter to `Live Logs`, applied only to the currently streamed client-side log buffer.

**Architecture:** Keep all time-range logic local to `LiveLogsPage` so the feature stays honest about its data source and does not imply backend history retrieval. Extend the existing `filtered` memo into a combined `time + level + text` filter, add a small toolbar control cluster for quick ranges and custom `from/to`, and update empty-state/helper copy to explain that the filter only applies to logs already loaded in the session.

**Tech Stack:** React, TypeScript, Vitest, Testing Library, CSS in `globals.css`, local i18n messages

---

## File Structure

- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
  Add time-range state, quick-range controls, custom interval draft/apply logic, and combined filtering.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
  Add focused tests for quick ranges, custom ranges, and the time-range-specific empty state.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
  Add labels and helper copy for quick ranges, custom range inputs, apply/reset actions, and time-range empty state.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
  Style compact quick-range chips and the inline custom range panel without making the toolbar bulky.

### Task 1: Add Time Range Filtering Logic To `LiveLogsPage`

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] **Step 1: Write the failing time-range tests**

```ts
it("filters buffered logs by a quick time range", async () => {
  const user = userEvent.setup();

  useRealtimeStore.setState({
    ...useRealtimeStore.getState(),
    logs: [
      {
        ts: "2026-04-11T11:40:00Z",
        service: "diagnosticserviceai",
        payload: { message: "older line", level: "INFO", traceId: null }
      },
      {
        ts: "2026-04-11T12:00:00Z",
        service: "diagnosticserviceai",
        payload: { message: "recent line", level: "INFO", traceId: null }
      }
    ]
  });
  vi.setSystemTime(new Date("2026-04-11T12:05:00Z"));

  renderPage();
  await user.click(screen.getByRole("button", { name: /15m/i }));

  expect(screen.getByText(/recent line/i)).toBeInTheDocument();
  expect(screen.queryByText(/older line/i)).not.toBeInTheDocument();
});

it("applies a custom time range after pressing apply", async () => {
  const user = userEvent.setup();

  renderPage();
  await user.click(screen.getByRole("button", { name: /custom range/i }));
  await user.type(screen.getByLabelText(/from/i), "2026-04-11T11:59");
  await user.type(screen.getByLabelText(/to/i), "2026-04-11T12:01");
  await user.click(screen.getByRole("button", { name: /apply/i }));

  expect(screen.getByText(/user login succeeded/i)).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the `LiveLogsPage` tests to verify they fail**

Run: `npm test -- LiveLogsPage`  
Expected: FAIL because the page has no time-range controls or time-based filtering yet.

- [ ] **Step 3: Add local time-range types, defaults, and helpers**

```tsx
type QuickRangeKey = "all" | "5m" | "15m" | "1h" | "6h" | "24h";

type CustomRangeDraft = {
  from: string;
  to: string;
};

type ActiveTimeRange =
  | { kind: "all" }
  | { kind: "relative"; key: Exclude<QuickRangeKey, "all">; minutes: number }
  | { kind: "custom"; from: number; to: number };

const QUICK_RANGES: Array<{ key: QuickRangeKey; minutes?: number }> = [
  { key: "all" },
  { key: "5m", minutes: 5 },
  { key: "15m", minutes: 15 },
  { key: "1h", minutes: 60 },
  { key: "6h", minutes: 360 },
  { key: "24h", minutes: 1_440 }
];

function parseDatetimeLocal(value: string) {
  if (!value) {
    return null;
  }

  const parsed = new Date(value).getTime();
  return Number.isNaN(parsed) ? null : parsed;
}
```

- [ ] **Step 4: Extend the `filtered` memo to include time filtering**

```tsx
const [activeTimeRange, setActiveTimeRange] = useState<ActiveTimeRange>({ kind: "all" });
const [customRangeDraft, setCustomRangeDraft] = useState<CustomRangeDraft>({ from: "", to: "" });
const [customPanelOpen, setCustomPanelOpen] = useState(false);

const filtered = useMemo(() => {
  const now = Date.now();

  return logs.filter((line) => {
    const lineTs = new Date(line.ts).getTime();
    const timeOk =
      activeTimeRange.kind === "all" ||
      (activeTimeRange.kind === "relative" && lineTs >= now - activeTimeRange.minutes * 60_000) ||
      (activeTimeRange.kind === "custom" &&
        lineTs >= activeTimeRange.from &&
        lineTs <= activeTimeRange.to);

    const lvlOk = !level || line.payload.level === level;
    const txtOk = !text || line.payload.message.toLowerCase().includes(text.toLowerCase());

    return timeOk && lvlOk && txtOk;
  });
}, [logs, activeTimeRange, level, text]);
```

- [ ] **Step 5: Add quick-range button handlers and custom apply/reset handlers**

```tsx
function handleQuickRangeSelect(key: QuickRangeKey) {
  setCustomPanelOpen(false);

  if (key === "all") {
    setActiveTimeRange({ kind: "all" });
    return;
  }

  const config = QUICK_RANGES.find((entry) => entry.key === key);
  if (!config?.minutes) {
    return;
  }

  setActiveTimeRange({ kind: "relative", key, minutes: config.minutes });
}

function handleApplyCustomRange() {
  const from = parseDatetimeLocal(customRangeDraft.from);
  const to = parseDatetimeLocal(customRangeDraft.to);

  if (from === null || to === null || from > to) {
    return;
  }

  setActiveTimeRange({ kind: "custom", from, to });
  setCustomPanelOpen(false);
}

function handleResetTimeRange() {
  setActiveTimeRange({ kind: "all" });
  setCustomRangeDraft({ from: "", to: "" });
  setCustomPanelOpen(false);
}
```

- [ ] **Step 6: Run the `LiveLogsPage` tests to verify the filtering logic passes**

Run: `npm test -- LiveLogsPage`  
Expected: PASS for quick ranges and custom range behavior.

- [ ] **Step 7: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx
git commit -m "feat: add live logs time range filtering"
```

### Task 2: Add Time Range Controls And Empty-State Messaging

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] **Step 1: Write the failing UI-specific test for time-range controls**

```ts
it("shows helper copy that explains time ranges only filter the streamed buffer", () => {
  renderPage();

  expect(
    screen.getByText(/time range filters only the streamed logs already loaded in this session/i)
  ).toBeInTheDocument();
});

it("shows a time-range-specific empty state when the selected interval has no lines", async () => {
  const user = userEvent.setup();
  vi.setSystemTime(new Date("2026-04-11T13:00:00Z"));

  renderPage();
  await user.click(screen.getByRole("button", { name: /5m/i }));

  expect(screen.getByText(/no logs match the selected time range/i)).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the `LiveLogsPage` tests to verify these expectations fail**

Run: `npm test -- LiveLogsPage`  
Expected: FAIL because the helper copy and dedicated time-range empty state do not exist yet.

- [ ] **Step 3: Add quick-range chips and a compact custom range panel to the toolbar**

```tsx
<div className="logs-console-timebar">
  <span className="logs-console-toolbar-label">{t("logs.timeRange")}</span>
  <div className="logs-console-range-chips">
    {QUICK_RANGES.map((entry) => (
      <button
        key={entry.key}
        type="button"
        className={`logs-console-range-chip ${isActiveRange(entry.key) ? "is-active" : ""}`}
        onClick={() => handleQuickRangeSelect(entry.key)}
      >
        {t(`logs.range.${entry.key}`)}
      </button>
    ))}
    <button
      type="button"
      className={`logs-console-range-chip ${activeTimeRange.kind === "custom" || customPanelOpen ? "is-active" : ""}`}
      onClick={() => setCustomPanelOpen((value) => !value)}
    >
      {t("logs.range.custom")}
    </button>
  </div>

  {customPanelOpen ? (
    <div className="logs-console-range-panel">
      <label className="field">
        <span>{t("logs.from")}</span>
        <input
          className="input"
          type="datetime-local"
          value={customRangeDraft.from}
          onChange={(e) => setCustomRangeDraft((current) => ({ ...current, from: e.target.value }))}
        />
      </label>
      <label className="field">
        <span>{t("logs.to")}</span>
        <input
          className="input"
          type="datetime-local"
          value={customRangeDraft.to}
          onChange={(e) => setCustomRangeDraft((current) => ({ ...current, to: e.target.value }))}
        />
      </label>
      <div className="logs-console-range-panel-actions">
        <button type="button" className="button secondary logs-console-action" onClick={handleResetTimeRange}>
          {t("logs.resetRange")}
        </button>
        <button type="button" className="button logs-console-action" onClick={handleApplyCustomRange}>
          {t("logs.applyRange")}
        </button>
      </div>
    </div>
  ) : null}
</div>
```

- [ ] **Step 4: Add helper copy, quick-range labels, and time-range empty-state copy**

```ts
logs: {
  timeRange: "Time range",
  streamedBufferHint: "Time range filters only the streamed logs already loaded in this session.",
  from: "From",
  to: "To",
  applyRange: "Apply",
  resetRange: "Reset",
  noLinesInRange: "No logs match the selected time range in the current streamed buffer.",
  range: {
    all: "All streamed",
    "5m": "5m",
    "15m": "15m",
    "1h": "1h",
    "6h": "6h",
    "24h": "24h",
    custom: "Custom range"
  }
}
```

- [ ] **Step 5: Add compact styles for the time filter controls**

```css
.logs-console-timebar {
  display: grid;
  gap: 8px;
}

.logs-console-range-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.logs-console-range-chip {
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg-soft) 84%, transparent);
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.logs-console-range-chip.is-active {
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 60%, var(--border));
}

.logs-console-range-panel {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: color-mix(in srgb, var(--bg-soft) 88%, transparent);
}
```

- [ ] **Step 6: Update the empty-state rendering to distinguish no lines at all vs. no lines in the active time range**

```tsx
const hasAnyLogs = logs.length > 0;
const hasTimeRestriction = activeTimeRange.kind !== "all";

{filtered.length === 0 ? (
  <div className="logs-console-empty">
    {hasAnyLogs && hasTimeRestriction ? t("logs.noLinesInRange") : t("logs.noLinesYet")}
  </div>
) : null}
```

- [ ] **Step 7: Run the `LiveLogsPage` tests again**

Run: `npm test -- LiveLogsPage`  
Expected: PASS with quick ranges, custom range controls, helper copy, and time-range-specific empty state.

- [ ] **Step 8: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts /Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css
git commit -m "feat: add live logs time range picker"
```

### Task 3: Final Verification And Build

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`

- [ ] **Step 1: Do a final code pass for invalid custom-range edge cases**

```tsx
const customRangeInvalid =
  customRangeDraft.from.length > 0 &&
  customRangeDraft.to.length > 0 &&
  parseDatetimeLocal(customRangeDraft.from) !== null &&
  parseDatetimeLocal(customRangeDraft.to) !== null &&
  parseDatetimeLocal(customRangeDraft.from)! > parseDatetimeLocal(customRangeDraft.to)!;

<button
  type="button"
  className="button logs-console-action"
  disabled={
    !customRangeDraft.from ||
    !customRangeDraft.to ||
    customRangeInvalid
  }
  onClick={handleApplyCustomRange}
>
  {t("logs.applyRange")}
</button>
```

- [ ] **Step 2: Run the focused verification suite**

Run: `npm test -- LiveLogsPage SettingsPage store.test`  
Expected: PASS

- [ ] **Step 3: Run the production build**

Run: `npm run build`  
Expected: successful build with no TypeScript errors. Existing chunk-size warnings are acceptable if no new failures appear.

- [ ] **Step 4: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts /Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css
git commit -m "test: verify live logs time range flow"
```

## Self-Review

### Spec coverage

- Quick ranges, custom `from/to`, helper messaging, and time-range-specific empty state are all explicitly covered by Tasks 1 and 2.
- The feature stays client-side only and does not add backend work.
- Final verification of the integrated `Live Logs` experience is covered by Task 3.

### Placeholder scan

- No `TODO` / `TBD` placeholders remain.
- Each task includes exact files, test commands, and concrete code snippets.
- Commit steps are explicit and scoped.

### Type consistency

- `QuickRangeKey`, `ActiveTimeRange`, and `CustomRangeDraft` are introduced once in Task 1 and used consistently in later tasks.
- Message keys in Tasks 2 and 3 align with the planned `messages.ts` additions.
