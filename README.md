# DiagnosticServiceAI

Spring Boot backend for local log aggregation, clustering, analytics, and AI-assisted diagnosis.

## Local Setup

1. Copy the example environment file:

```bash
cp .env.example .env
```

Fill in the values you need on the current device:
- PostgreSQL host/port/user/password
- `GEMINI_API_KEY` from Google AI Studio if you want AI diagnosis enabled
- any local Docker label settings you use for project discovery

2. Start PostgreSQL only:

```bash
docker compose up -d postgres
```

3. Run the backend locally with the dev profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Flyway runs automatically on startup and validates the schema through JPA.

## Gemini API Key

If you want Gemini-backed diagnosis:

1. Open `Google AI Studio`: https://ai.google.dev/aistudio
2. Create or view an API key
3. Put it into your local `.env`:

```bash
GEMINI_API_KEY=your_real_key_here
GEMINI_MODEL=gemini-2.5-flash
GEMINI_PROMPT_VERSION=v1
```

Do not commit the real key to git.

## Full Docker Run

To run both PostgreSQL and the backend in containers:

```bash
docker compose --profile full up --build
```

## Profile Split

- `dev` profile: run the app from IntelliJ or `bootRun`, connect to local PostgreSQL on `localhost`
- `docker` profile: run the app inside Docker, connect to the `postgres` service by container hostname

This keeps local development and container execution cleanly separated.

## Why This Setup

- every device uses the same PostgreSQL image and credentials shape
- Flyway owns schema creation through versioned SQL migrations
- `.env` keeps machine-specific values out of committed config
- `compose.yml` removes manual database setup drift between laptops
