# Ops Console UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refresh `Live Logs`, `Settings`, and shared light-theme styling so the frontend reads like a compact ops console with safer settings interactions.

**Architecture:** Keep the existing page structure and data flow, but split the work into three focused layers: settings state semantics, page-level UI composition, and shared visual tokens/styles. `Settings` moves from immediate persistence to a draft/apply model, while `Live Logs` stays on the same websocket/store contract and only changes layout, control density, and presentation. Shared CSS tokens and smaller control styles provide the cross-page polish needed for dark and light themes to look deliberate.

**Tech Stack:** React, TypeScript, Zustand, React Router, Vitest, Testing Library, CSS in `globals.css`

---

## File Structure

- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/settings/store.ts`
  Keep persisted settings in Zustand, but expose defaults and helpers needed by a draft-based page UI.
- Create: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/settings/store.test.ts`
  Verify persist/apply/reset behavior for the updated settings store.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/SettingsPage.tsx`
  Replace the raw instant-save form with a control-panel layout that uses draft state and explicit actions.
- Create: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/SettingsPage.test.tsx`
  Verify draft behavior, helper text, and reset/save interactions.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
  Recompose the toolbar and console chrome into the denser `Ops Console` layout without touching websocket behavior.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
  Lock in compact controls, sticky table-style headings, and collapsed error details behavior.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
  Add missing copy for explicit save/reset states, connection helper messaging, presets, and new live-log toolbar labels.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
  Tighten fonts and spacing, add settings page styles, compact console toolbar styles, and light-theme fixes while preserving the dark log viewport.
- Optional inspect-only reference: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/ui/PageIntro.tsx`
  Only touch if the action bar spacing blocks the new settings/live logs layouts; otherwise keep it unchanged.

### Task 1: Refactor Settings State For Draft/Apply UX

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/settings/store.ts`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/settings/store.test.ts`

- [ ] **Step 1: Write the failing store tests**

```ts
import { beforeEach, describe, expect, it } from "vitest";
import { useSettingsStore } from "./store";

describe("settings store", () => {
  beforeEach(() => {
    localStorage.clear();
    useSettingsStore.getState().resetDefaults();
  });

  it("persists applied connection settings", () => {
    useSettingsStore.getState().applyConnectionSettings({
      apiBaseUrl: "http://localhost:8081",
      wsBaseUrl: "ws://localhost:8081",
      reconnectMinMs: 1200,
      reconnectMaxMs: 9000
    });

    const state = useSettingsStore.getState();
    expect(state.apiBaseUrl).toBe("http://localhost:8081");
    expect(state.wsBaseUrl).toBe("ws://localhost:8081");
    expect(JSON.parse(localStorage.getItem("diagnostic-ui-settings") ?? "{}")).toMatchObject({
      apiBaseUrl: "http://localhost:8081",
      wsBaseUrl: "ws://localhost:8081",
      reconnectMinMs: 1200,
      reconnectMaxMs: 9000
    });
  });

  it("restores defaults without changing theme and locale helpers", () => {
    useSettingsStore.getState().applyConnectionSettings({
      apiBaseUrl: "http://localhost:8081",
      wsBaseUrl: "ws://localhost:8081",
      reconnectMinMs: 1200,
      reconnectMaxMs: 9000
    });

    useSettingsStore.getState().resetConnectionDefaults();

    const state = useSettingsStore.getState();
    expect(state.apiBaseUrl).toBe("http://localhost:8080");
    expect(state.wsBaseUrl).toBe("ws://localhost:8080");
    expect(state.reconnectMinMs).toBe(800);
    expect(state.reconnectMaxMs).toBe(10_000);
  });
});
```

- [ ] **Step 2: Run the store tests to verify they fail**

Run: `npm test -- store.test`  
Expected: FAIL because `applyConnectionSettings` and `resetConnectionDefaults` do not exist yet.

- [ ] **Step 3: Add store helpers and exported defaults**

```ts
type ConnectionSettings = {
  apiBaseUrl: string;
  wsBaseUrl: string;
  reconnectMinMs: number;
  reconnectMaxMs: number;
};

export const settingsDefaults: ConnectionSettings & {
  theme: Theme;
  locale: Locale;
  sidebarCollapsed: boolean;
} = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080",
  wsBaseUrl: import.meta.env.VITE_WS_BASE_URL ?? "ws://localhost:8080",
  reconnectMinMs: 800,
  reconnectMaxMs: 10_000,
  theme: "dark",
  locale: "ru",
  sidebarCollapsed: false
};

type SettingsState = ConnectionSettings & {
  theme: Theme;
  locale: Locale;
  sidebarCollapsed: boolean;
  applyConnectionSettings: (next: ConnectionSettings) => void;
  resetConnectionDefaults: () => void;
  // keep existing theme/locale/sidebar methods
};

applyConnectionSettings: (next) => set((state) => persist({ ...state, ...next })),
resetConnectionDefaults: () =>
  set((state) =>
    persist({
      ...state,
      apiBaseUrl: settingsDefaults.apiBaseUrl,
      wsBaseUrl: settingsDefaults.wsBaseUrl,
      reconnectMinMs: settingsDefaults.reconnectMinMs,
      reconnectMaxMs: settingsDefaults.reconnectMaxMs
    })
  ),
resetDefaults: () => {
  document.documentElement.setAttribute("data-theme", settingsDefaults.theme);
  document.documentElement.lang = settingsDefaults.locale;
  set(() => persist({ ...settingsDefaults }));
}
```

- [ ] **Step 4: Run the store tests to verify they pass**

Run: `npm test -- store.test`  
Expected: PASS with the new store helpers and persisted values.

- [ ] **Step 5: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/settings/store.ts /Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/settings/store.test.ts
git commit -m "refactor: add draft-friendly settings store helpers"
```

### Task 2: Rebuild Settings As A Safe Control Panel

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/SettingsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/SettingsPage.test.tsx`

- [ ] **Step 1: Write the failing settings page tests**

```ts
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { SettingsPage } from "./SettingsPage";
import { useSettingsStore } from "@features/settings/store";

describe("SettingsPage", () => {
  beforeEach(() => {
    localStorage.clear();
    useSettingsStore.getState().resetDefaults();
  });

  it("keeps edits in draft state until save is pressed", async () => {
    const user = userEvent.setup();
    render(<SettingsPage />);

    const input = screen.getByLabelText(/api base url/i);
    await user.clear(input);
    await user.type(input, "http://localhost:8081");

    expect(useSettingsStore.getState().apiBaseUrl).toBe("http://localhost:8080");

    await user.click(screen.getByRole("button", { name: /save/i }));

    expect(useSettingsStore.getState().apiBaseUrl).toBe("http://localhost:8081");
  });

  it("resets draft values without wiping active persisted settings", async () => {
    const user = userEvent.setup();
    render(<SettingsPage />);

    const input = screen.getByLabelText(/api base url/i);
    await user.clear(input);
    await user.type(input, "http://localhost:8081");
    await user.click(screen.getByRole("button", { name: /reset/i }));

    expect(input).toHaveValue("http://localhost:8080");
    expect(useSettingsStore.getState().apiBaseUrl).toBe("http://localhost:8080");
  });
});
```

- [ ] **Step 2: Run the settings page tests to verify they fail**

Run: `npm test -- SettingsPage`  
Expected: FAIL because the current page writes directly to Zustand and has no explicit `Save`/`Reset` actions.

- [ ] **Step 3: Replace the instant-save form with a draft-based control panel**

```tsx
const active = useSettingsStore((s) => ({
  apiBaseUrl: s.apiBaseUrl,
  wsBaseUrl: s.wsBaseUrl,
  reconnectMinMs: s.reconnectMinMs,
  reconnectMaxMs: s.reconnectMaxMs
}));
const applyConnectionSettings = useSettingsStore((s) => s.applyConnectionSettings);
const resetConnectionDefaults = useSettingsStore((s) => s.resetConnectionDefaults);

const [draft, setDraft] = useState(active);
const [savedTick, setSavedTick] = useState(0);

useEffect(() => {
  setDraft(active);
}, [active.apiBaseUrl, active.wsBaseUrl, active.reconnectMinMs, active.reconnectMaxMs]);

const dirty =
  draft.apiBaseUrl !== active.apiBaseUrl ||
  draft.wsBaseUrl !== active.wsBaseUrl ||
  draft.reconnectMinMs !== active.reconnectMinMs ||
  draft.reconnectMaxMs !== active.reconnectMaxMs;

function handleSave() {
  applyConnectionSettings(draft);
  setSavedTick((value) => value + 1);
}

function handleResetDraft() {
  setDraft(active);
}

function handleResetDefaults() {
  resetConnectionDefaults();
  setSavedTick((value) => value + 1);
}

return (
  <div className="settings-page">
    <PageIntro
      title={t("settings.title")}
      description={t("settings.description")}
      actions={
        <div className="settings-actions">
          <span className={`settings-save-state ${dirty ? "is-dirty" : "is-saved"}`}>
            {dirty ? t("settings.unsavedChanges") : t("settings.saved")}
          </span>
          <button className="button secondary" onClick={handleResetDraft} disabled={!dirty}>
            {t("settings.resetDraft")}
          </button>
          <button className="button secondary" onClick={handleResetDefaults}>
            {t("common.resetDefaults")}
          </button>
          <button className="button" onClick={handleSave} disabled={!dirty}>
            {t("settings.save")}
          </button>
        </div>
      }
    />

    <section className="settings-layout">
      <article className="card settings-panel">
        <header className="settings-panel-header">
          <h2>{t("settings.connectionTitle")}</h2>
          <p>{t("settings.transportHelper")}</p>
        </header>
        <label className="field">
          <span>{t("settings.apiBaseUrl")}</span>
          <input
            className="input"
            value={draft.apiBaseUrl}
            onChange={(e) => setDraft((current) => ({ ...current, apiBaseUrl: e.target.value }))}
          />
        </label>
        <label className="field">
          <span>{t("settings.wsBaseUrl")}</span>
          <input
            className="input"
            value={draft.wsBaseUrl}
            onChange={(e) => setDraft((current) => ({ ...current, wsBaseUrl: e.target.value }))}
          />
        </label>
      </article>
    </section>
  </div>
);
```

- [ ] **Step 4: Add i18n copy and settings-specific styles**

```ts
settings: {
  title: "Settings",
  description: "...",
  connectionTitle: "Connection",
  reconnectTitle: "Reconnect behavior",
  transportHelper: "These values control where the frontend connects for REST and WebSocket. They do not change runtime target ports.",
  presetsTitle: "Local presets",
  preset8080: "Local 8080",
  preset8081: "Local 8081",
  save: "Save",
  resetDraft: "Reset",
  saved: "Saved",
  unsavedChanges: "Unsaved changes"
}
```

```css
.settings-page {
  display: grid;
  gap: 18px;
}

.settings-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(260px, 0.8fr);
  gap: 16px;
}

.settings-panel {
  padding: 18px;
  border-radius: 18px;
}

.settings-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.settings-save-state {
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-muted);
}

.settings-save-state.is-dirty {
  color: var(--warn);
}

.settings-save-state.is-saved {
  color: var(--ok);
}
```

- [ ] **Step 5: Run the settings page tests to verify they pass**

Run: `npm test -- SettingsPage`  
Expected: PASS with draft edits, explicit save, and reset behavior covered.

- [ ] **Step 6: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/SettingsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/SettingsPage.test.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts /Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css
git commit -m "feat: redesign settings as draft control panel"
```

### Task 3: Refresh Live Logs Into A Denser Ops Console

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`

- [ ] **Step 1: Extend the live logs tests with the intended toolbar behavior**

```ts
it("renders compact status chips and action controls instead of a large paused button", async () => {
  renderPage();

  expect(screen.getByText(/connected/i)).toHaveClass("logs-console-status-chip");
  expect(screen.getByRole("button", { name: /follow live stream/i })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: /clear console/i })).toBeInTheDocument();
});

it("keeps latest error details collapsed by default", () => {
  renderPage();

  expect(screen.queryByText(/incident details/i)).not.toBeInTheDocument();
  expect(screen.getByRole("button", { name: /show incident details/i })).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the live logs tests to verify they fail**

Run: `npm test -- LiveLogsPage`  
Expected: FAIL because the toolbar labels, chip classes, and incident-detail copy do not exist yet.

- [ ] **Step 3: Recompose `LiveLogsPage` around compact chips and grouped controls**

```tsx
<header className="logs-console-toolbar">
  <div className="logs-console-toolbar-primary">
    <div className="logs-console-target-block">
      <span className="logs-console-eyebrow">{t("logs.targetLabel")}</span>
      <div className="logs-console-target-row">
        <strong>{runtimeTargetId ? selectedTargetName : t("logs.notSelected")}</strong>
        <span className={`logs-console-status-chip ${connected ? "is-live" : "is-offline"}`}>
          {connected ? t("common.connected") : t("common.disconnected")}
        </span>
        <button
          type="button"
          className={`logs-console-follow-chip ${follow ? "is-live" : "is-paused"}`}
          onClick={handleFollowToggle}
        >
          {follow ? t("logs.following") : t("logs.paused")}
        </button>
      </div>
    </div>

    <div className="logs-console-actions">
      <button type="button" className="button secondary logs-console-action" onClick={handleClear}>
        {t("logs.clearConsole")}
      </button>
      {runtimeTargetId ? (
        <Link className="button secondary logs-console-action" to="/runtime-targets">
          {t("common.changeContainer")}
        </Link>
      ) : null}
    </div>
  </div>

  <div className="logs-console-toolbar-secondary">
    <input className="input logs-console-input" placeholder={t("logs.filterText")} />
    <select className="select logs-console-select" value={level} onChange={(e) => setLevel(e.target.value)}>
      <option value="">{t("logs.allLevels")}</option>
      <option value="INFO">INFO</option>
      <option value="WARN">WARN</option>
      <option value="ERROR">ERROR</option>
      <option value="DEBUG">DEBUG</option>
    </select>
  </div>
</header>
```

- [ ] **Step 4: Tighten console styles and keep the viewport dark in both themes**

```css
.logs-console-toolbar {
  padding: 12px 14px;
  border-radius: 16px;
}

.logs-console-toolbar-primary,
.logs-console-toolbar-secondary {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.logs-console-status-chip,
.logs-console-follow-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.logs-console-viewport {
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(8, 13, 24, 0.98), rgba(6, 10, 20, 1)),
    radial-gradient(circle at top left, rgba(45, 212, 191, 0.08), transparent 38%);
}

:root[data-theme="light"] .logs-console-shell,
:root[data-theme="light"] .logs-console-toolbar {
  background: color-mix(in srgb, #f7fbff 84%, var(--card));
}
```

- [ ] **Step 5: Reframe error details as an opt-in incident panel**

```tsx
<div className="logs-console-footer">
  <div className="logs-console-footer-stats">
    <span>{filtered.length} {t("logs.linesCount")}</span>
    <span>{errors.length} {t("logs.errorCount")}</span>
  </div>
  <button
    type="button"
    className="button secondary logs-console-action"
    disabled={!latestError}
    onClick={() => setErrorPanelOpen((value) => !value)}
  >
    {errorPanelOpen ? t("logs.hideIncidentDetails") : t("logs.showIncidentDetails")}
  </button>
</div>
```

- [ ] **Step 6: Run the live logs tests to verify they pass**

Run: `npm test -- LiveLogsPage`  
Expected: PASS with compact toolbar controls, collapsed error panel, and new copy.

- [ ] **Step 7: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts /Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css
git commit -m "feat: refresh live logs into compact ops console"
```

### Task 4: Apply Shared Light-Theme And Density Polish

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/SettingsPage.test.tsx`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] **Step 1: Add regression checks for light-theme-safe rendering**

```ts
it("keeps the logs viewport rendered when the theme is light", () => {
  document.documentElement.setAttribute("data-theme", "light");
  renderPage();

  expect(screen.getByTestId("logs-console")).toBeInTheDocument();
});
```

```ts
it("shows explicit connection helper copy on settings page", () => {
  render(<SettingsPage />);

  expect(
    screen.getByText(/these values control where the frontend connects for rest and websocket/i)
  ).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the page tests to verify any missing copy/styles are exposed**

Run: `npm test -- SettingsPage LiveLogsPage`  
Expected: FAIL if the helper copy or theme-safe rendering hooks are missing.

- [ ] **Step 3: Tighten global typography and light-theme surfaces**

```css
:root {
  --control-height: 40px;
  --control-height-compact: 34px;
}

:root[data-theme="light"] {
  --bg: #edf3fb;
  --bg-soft: #f6f9fd;
  --card: #fcfeff;
  --border: #d4dfec;
  --text: #14263f;
  --text-muted: #5d7290;
}

.card {
  border-radius: 16px;
  padding: 14px;
}

.table th,
.table td {
  font-size: 13px;
  padding: 9px 10px;
}

.input,
.select,
.button,
.textarea {
  min-height: var(--control-height);
  padding: 10px 12px;
  font-size: 13px;
}

.button.secondary {
  background: color-mix(in srgb, var(--bg-soft) 84%, transparent);
}
```

- [ ] **Step 4: Run the focused test suite and production build**

Run: `npm test -- store.test SettingsPage LiveLogsPage`  
Expected: PASS

Run: `npm run build`  
Expected: successful Vite production build with no TypeScript errors.

- [ ] **Step 5: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/SettingsPage.test.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx
git commit -m "style: polish light theme and control density"
```

## Self-Review

### Spec coverage

- `Live Logs` compact control bar, smaller controls, collapsed error details, and preserved dark viewport are covered by Task 3 and Task 4.
- `Settings` draft state, explicit `Save` / `Reset` / `Reset to defaults`, helper messaging, and safer UX are covered by Task 1 and Task 2.
- Shared light-theme polish, tighter density, and smaller fonts are covered by Task 4.

### Placeholder scan

- No `TODO` / `TBD` markers remain.
- Each task lists exact files, test commands, and implementation snippets.
- Commit steps are concrete and scoped.

### Type consistency

- `applyConnectionSettings`, `resetConnectionDefaults`, and `settingsDefaults` are defined in Task 1 and reused consistently in Task 2.
- `SettingsPage.test.tsx` and `LiveLogsPage.test.tsx` assertions line up with the new copy introduced in Tasks 2 and 3.

