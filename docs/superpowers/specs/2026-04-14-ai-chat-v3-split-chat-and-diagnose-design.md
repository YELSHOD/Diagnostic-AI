# AI Chat v3 Design

## Goal
Split conversational AI chat from log diagnosis so the app has a normal assistant chat experience while keeping structured incident analysis in `Live Logs`.

## Scope
- Add a new backend endpoint: `POST /api/ai/chat`
- Keep `POST /api/ai/diagnose` only for structured log diagnosis
- Move `AI Chat` page to the new chat endpoint
- Remove user-facing `Gemini` naming and replace it with `AI Assistant` / `ИИ ассистент`

## Behavior
### AI Chat
- Accepts normal user messages such as greetings, product questions, and how-to questions.
- Returns ordinary conversational text.
- May optionally include lightweight follow-up suggestions or related pages, but no diagnosis sections.
- May receive optional context from the current live target/log buffer, but the response remains free-form assistant text.

### Live Logs Diagnose
- Keeps the current structured diagnosis response with `summary`, `timeline`, `probableRootCause`, `evidence`, and `nextChecks`.
- Remains on `/api/ai/diagnose`.

## Backend Design
### New chat contract
- `AiChatRequest`
  - `message`
  - `history` (latest frontend chat turns only)
  - `context` (optional current target name and compact log context)
- `AiChatResponse`
  - `answer`
  - `suggestions`
  - `relatedPages`
  - `rawText`

### New backend units
- `AiChatController`
- `AiChatService`
- `ChatPromptFactory`

### Prompt behavior
- Product-aware assistant prompt with knowledge of the current app pages and capabilities.
- Answer in the user’s language.
- Do not invent unavailable features.
- No diagnosis schema and no forced root-cause structure.

## Frontend Design
### AI Chat page
- Uses `/api/ai/chat`
- Renders ChatGPT-style thread and bottom composer
- Shows loading as a single assistant bubble
- Shows ordinary assistant text only
- May render compact suggestion chips if present

### UI naming
- Replace `Gemini` in visible UI copy with `AI Assistant` / `ИИ ассистент`
- Provider/model names stay backend/internal, not visible in the normal UI

## Testing
- Backend controller/service/prompt tests for `/api/ai/chat`
- Frontend tests for normal chat answers and assistant loading bubble
- Regression tests to ensure `Live Logs` still uses `/api/ai/diagnose`
