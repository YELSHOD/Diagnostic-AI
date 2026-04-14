# Live Logs Gemini Diagnose Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a `Diagnose with Gemini` flow to `Live Logs` that sends the current target, the operator’s question, and the last 50 visible log lines to the backend `/api/ai/diagnose` endpoint, then renders the AI result inline on the same page.

**Architecture:** Keep the new AI diagnose flow page-local to `LiveLogsPage` and use a thin API helper for the backend call. This avoids inventing a chat subsystem and keeps the first AI UX tied directly to the existing investigation context: current target, current filters, current visible buffer. The page owns the request draft, loading/error/result state, while `shared/lib/http.ts` stays the shared transport layer.

**Tech Stack:** React, TypeScript, existing `apiRequest` fetch helper, Vitest, Testing Library, CSS in `globals.css`, local i18n messages

---

## File Structure

- Create: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts`
  Small API helper for `POST /api/ai/diagnose`.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
  Add the inline diagnose panel, request payload building, pending/error/result states, and the Gemini result rendering.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
  Add tests for the diagnose panel flow, payload behavior, validation, and result rendering.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
  Add all user-facing labels, hints, loading copy, and error/help text for the Gemini flow.
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
  Add compact styles for the diagnose action, inline form panel, result card, and raw-response disclosure.

### Task 1: Add Frontend Gemini API Contract

**Files:**
- Create: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] **Step 1: Write the failing Live Logs test for submitting a diagnosis request**

```ts
it("submits the current target, question, and visible log lines to Gemini", async () => {
  const user = userEvent.setup();
  const diagnose = vi.fn().mockResolvedValue({
    provider: "gemini",
    model: "gemini-2.5-flash",
    promptVersion: "v1",
    summary: "Likely root cause",
    bullets: ["Observation A"],
    rawText: "Likely root cause"
  });

  vi.doMock("@features/ai/api", () => ({
    diagnoseLogsWithGemini: diagnose
  }));

  renderPage();
  await user.click(screen.getByRole("button", { name: /diagnose with gemini/i }));
  await user.type(screen.getByLabelText(/question/i), "Why is this unstable?");
  await user.click(screen.getByRole("button", { name: /run diagnosis/i }));

  expect(diagnose).toHaveBeenCalledWith({
    service: "diagnosticserviceai",
    question: "Why is this unstable?",
    logLines: expect.arrayContaining(["User login succeeded"])
  });
});
```

- [ ] **Step 2: Run the `LiveLogsPage` tests to verify they fail**

Run: `npm test -- LiveLogsPage`  
Expected: FAIL because there is no Gemini API helper and no diagnose action yet.

- [ ] **Step 3: Add the dedicated Gemini API helper**

```ts
import { apiRequest } from "@shared/lib/http";

export type AiDiagnosisRequest = {
  service: string;
  question: string;
  logLines: string[];
};

export type AiDiagnosisResponse = {
  provider: string;
  model: string;
  promptVersion: string;
  summary: string;
  bullets: string[];
  rawText: string;
};

export function diagnoseLogsWithGemini(body: AiDiagnosisRequest) {
  return apiRequest<AiDiagnosisResponse>("/api/ai/diagnose", {
    method: "POST",
    body
  });
}
```

- [ ] **Step 4: Build a helper in `LiveLogsPage` that extracts the last 50 visible log lines**

```tsx
function buildVisibleLogLines(lines: typeof filtered) {
  return lines.slice(-50).map((line) => {
    const parsed = parseConsoleSource(line.payload.message, line.service);
    const level = line.payload.level ?? "INFO";
    return `${line.ts} ${level} [${parsed.source}] ${parsed.message}`;
  });
}
```

- [ ] **Step 5: Run the `LiveLogsPage` tests to verify the API contract layer now has a place to attach**

Run: `npm test -- LiveLogsPage`  
Expected: still failing, but now for missing UI behavior rather than missing import/function boundaries.

- [ ] **Step 6: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx
git commit -m "refactor: add frontend Gemini diagnose contract"
```

### Task 2: Add The Inline Diagnose Panel To `Live Logs`

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`
- Test: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`

- [ ] **Step 1: Write the failing UI tests for the panel, validation, and result block**

```ts
it("opens the diagnose panel and blocks submit when question is empty", async () => {
  const user = userEvent.setup();

  renderPage();
  await user.click(screen.getByRole("button", { name: /diagnose with gemini/i }));

  expect(screen.getByText(/the latest 50 visible log lines will be sent/i)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: /run diagnosis/i })).toBeDisabled();
});

it("renders the Gemini response inline after a successful request", async () => {
  const user = userEvent.setup();
  vi.mocked(diagnoseLogsWithGemini).mockResolvedValue({
    provider: "gemini",
    model: "gemini-2.5-flash",
    promptVersion: "v1",
    summary: "Likely root cause",
    bullets: ["Observation A", "Observation B"],
    rawText: "Full answer"
  });

  renderPage();
  await user.click(screen.getByRole("button", { name: /diagnose with gemini/i }));
  await user.type(screen.getByLabelText(/question/i), "Why is this unstable?");
  await user.click(screen.getByRole("button", { name: /run diagnosis/i }));

  expect(await screen.findByText(/likely root cause/i)).toBeInTheDocument();
  expect(screen.getByText(/observation a/i)).toBeInTheDocument();
  expect(screen.getByText(/gemini-2.5-flash/i)).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the `LiveLogsPage` tests to verify these expectations fail**

Run: `npm test -- LiveLogsPage`  
Expected: FAIL because the panel, validation, and result block do not exist yet.

- [ ] **Step 3: Add page-local diagnose state and submit flow**

```tsx
const [diagnoseOpen, setDiagnoseOpen] = useState(false);
const [question, setQuestion] = useState("");
const [diagnosePending, setDiagnosePending] = useState(false);
const [diagnoseError, setDiagnoseError] = useState<string | null>(null);
const [diagnosis, setDiagnosis] = useState<AiDiagnosisResponse | null>(null);
const visibleLogLines = useMemo(() => buildVisibleLogLines(filtered), [filtered]);

async function handleRunDiagnosis() {
  if (!runtimeTargetId || !selectedTargetName) return;
  if (!question.trim() || visibleLogLines.length === 0) return;

  setDiagnosePending(true);
  setDiagnoseError(null);
  try {
    const result = await diagnoseLogsWithGemini({
      service: selectedTargetName,
      question: question.trim(),
      logLines: visibleLogLines
    });
    setDiagnosis(result);
  } catch (error) {
    setDiagnoseError(error instanceof Error ? error.message : t("logs.ai.failed"));
  } finally {
    setDiagnosePending(false);
  }
}
```

- [ ] **Step 4: Add the compact panel UI and inline result panel**

```tsx
<button
  type="button"
  className="button secondary logs-console-action"
  onClick={() => setDiagnoseOpen((value) => !value)}
>
  {t("logs.ai.action")}
</button>
```

```tsx
{diagnoseOpen ? (
  <section className="logs-ai-panel">
    <div className="logs-ai-header">
      <h3>{t("logs.ai.title")}</h3>
      <span className="badge">{selectedTargetName}</span>
    </div>
    <p>{t("logs.ai.helper")}</p>
    <label className="field">
      <span>{t("logs.ai.question")}</span>
      <textarea
        className="textarea logs-ai-textarea"
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
      />
    </label>
    {diagnoseError ? <div className="card auth-inline-alert">{diagnoseError}</div> : null}
    <div className="logs-ai-actions">
      <button type="button" className="button secondary logs-console-action" onClick={() => setDiagnoseOpen(false)}>
        {t("logs.ai.cancel")}
      </button>
      <button
        type="button"
        className="button logs-console-action"
        disabled={diagnosePending || !question.trim() || visibleLogLines.length === 0}
        onClick={handleRunDiagnosis}
      >
        {diagnosePending ? t("logs.ai.loading") : t("logs.ai.submit")}
      </button>
    </div>
  </section>
) : null}
```

```tsx
{diagnosis ? (
  <section className="logs-ai-result">
    <div className="logs-ai-result-meta">
      <span>{diagnosis.provider}</span>
      <span>{diagnosis.model}</span>
      <span>{diagnosis.promptVersion}</span>
    </div>
    <h3>{diagnosis.summary}</h3>
    <ul className="logs-ai-bullets">
      {diagnosis.bullets.map((bullet) => (
        <li key={bullet}>{bullet}</li>
      ))}
    </ul>
    <details>
      <summary>{t("logs.ai.rawToggle")}</summary>
      <pre className="logs-console-error-frames">{diagnosis.rawText}</pre>
    </details>
  </section>
) : null}
```

- [ ] **Step 5: Add i18n copy for the new Gemini flow**

```ts
ai: {
  action: "Diagnose with Gemini",
  title: "AI diagnosis",
  helper: "The latest 50 visible log lines will be sent with your question.",
  question: "Question",
  cancel: "Cancel",
  submit: "Run diagnosis",
  loading: "Diagnosing...",
  failed: "Gemini diagnosis failed.",
  rawToggle: "Show raw response",
  noVisibleLogs: "Adjust filters or wait for logs before running Gemini diagnosis."
}
```

- [ ] **Step 6: Add compact styles for the AI panel and result**

```css
.logs-ai-panel,
.logs-ai-result {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: color-mix(in srgb, var(--card) 92%, transparent);
}

.logs-ai-actions,
.logs-ai-result-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.logs-ai-textarea {
  min-height: 112px;
}
```

- [ ] **Step 7: Run the `LiveLogsPage` tests to verify the panel and result flow pass**

Run: `npm test -- LiveLogsPage`  
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts /Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css
git commit -m "feat: add Gemini diagnosis flow to live logs"
```

### Task 3: Handle Errors And Final Verification

**Files:**
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts`
- Modify: `/Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css`

- [ ] **Step 1: Add the failing error-state test**

```ts
it("renders a readable inline error when Gemini is unavailable", async () => {
  const user = userEvent.setup();
  vi.mocked(diagnoseLogsWithGemini).mockRejectedValue(new Error("Gemini integration is not configured"));

  renderPage();
  await user.click(screen.getByRole("button", { name: /diagnose with gemini/i }));
  await user.type(screen.getByLabelText(/question/i), "Why is this unstable?");
  await user.click(screen.getByRole("button", { name: /run diagnosis/i }));

  expect(await screen.findByText(/gemini integration is not configured/i)).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the `LiveLogsPage` tests to verify the explicit error handling still fails**

Run: `npm test -- LiveLogsPage`  
Expected: FAIL if the error path is not yet rendered cleanly.

- [ ] **Step 3: Finalize no-visible-logs handling and error rendering**

```tsx
const diagnoseDisabled = diagnosePending || !question.trim() || visibleLogLines.length === 0;

{diagnoseOpen && visibleLogLines.length === 0 ? (
  <div className="logs-ai-inline-hint">{t("logs.ai.noVisibleLogs")}</div>
) : null}
```

```tsx
catch (error) {
  setDiagnosis(null);
  setDiagnoseError(error instanceof Error ? error.message : t("logs.ai.failed"));
}
```

- [ ] **Step 4: Run the focused regression suite**

Run: `npm test -- store.test SettingsPage LiveLogsPage`  
Expected: PASS

- [ ] **Step 5: Run the production build**

Run: `npm run build`  
Expected: successful Vite build with no TypeScript errors.

- [ ] **Step 6: Commit**

```bash
git add /Users/admin/IdeaProjects/Diagnostic-AI-front/src/features/ai/api.ts /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/pages/LiveLogsPage.test.tsx /Users/admin/IdeaProjects/Diagnostic-AI-front/src/shared/i18n/messages.ts /Users/admin/IdeaProjects/Diagnostic-AI-front/src/app/styles/globals.css
git commit -m "test: verify live logs Gemini diagnosis flow"
```

## Self-Review

### Spec coverage

- Entry action, inline question flow, and sending the last 50 visible lines are covered by Tasks 1 and 2.
- Inline result rendering, loading, and readable error handling are covered by Tasks 2 and 3.
- The feature remains page-local and does not expand into a full chat system.

### Placeholder scan

- No `TODO` / `TBD` placeholders remain.
- Each task includes exact files, commands, and concrete code snippets.
- Commit steps are explicit and scoped.

### Type consistency

- `AiDiagnosisRequest` / `AiDiagnosisResponse` in the frontend API helper align with the backend contract already implemented.
- The `LiveLogsPage` tests and copy keys use one consistent `logs.ai.*` namespace throughout the plan.
