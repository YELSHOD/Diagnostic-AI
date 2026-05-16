# DiagnosticServiceAI

Spring Boot backend for local log aggregation, clustering, analytics, and AI-assisted diagnosis.

## Local Setup (Recommended)

1. Copy the example environment file:

```bash
cp .env.example .env
```

Fill in the values you need on the current device:
- local PostgreSQL host/port/user/password
- `APP_LOG_FILE` if you want the backend log file somewhere else
- `DOCKER_HOST` if your machine uses a non-default Docker socket path
- `GEMINI_API_KEY` from Google AI Studio if you want AI diagnosis enabled
- any local Docker label settings you use for project discovery

2. Run the backend locally from IntelliJ or terminal with the `dev` profile:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Flyway runs automatically on startup and validates the schema through JPA.
In `dev` profile the backend also writes its own logs to `./logs/diagnosticserviceai.log`, so the `diagnosticserviceai` runtime target can stream logs through `FILE_TAIL` while the app is running from IntelliJ.

## Local PostgreSQL

The preferred workflow is:
- backend runs from IntelliJ or `bootRun`
- PostgreSQL runs from your installed local PostgreSQL service
- Docker stays optional and is only needed when you want Docker container discovery/logs or a disposable database

If your local PostgreSQL does not already have the expected database/user, either:
- create them manually, or
- override `DB_NAME`, `DB_USER`, and `DB_PASSWORD` in `.env`

With the `dev` profile, `DB_AUTO_CREATE` defaults to `true`. On startup the app first checks whether the configured PostgreSQL database exists; if it is missing, it connects to the maintenance database from `DB_MAINTENANCE_NAME` (`postgres` by default) and creates `DB_NAME`. After that, Flyway applies all migrations from `src/main/resources/db/migration`.

If your PostgreSQL user cannot create databases, either grant it `CREATEDB`, run once with a superuser such as `postgres`, or create the database manually:

```sql
CREATE DATABASE diagnostic_ai;
```

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

The app container mounts `/var/run/docker.sock` and defaults `DOCKER_HOST` to `unix:///var/run/docker.sock`, so `/api/projects` and live logs can inspect local Docker containers from inside the backend container.

## Profile Split

- `dev` profile: run the app from IntelliJ or `bootRun`, connect to local PostgreSQL on `localhost`, write app logs to `APP_LOG_FILE`
- `docker` profile: run the app inside Docker, connect to the `postgres` service by container hostname

This keeps local development and container execution cleanly separated.

## Why This Setup

- IntelliJ development no longer depends on rebuilding a Docker image for every backend change
- local PostgreSQL can be your default database workflow
- Flyway owns schema creation through versioned SQL migrations
- `.env` keeps machine-specific values out of committed config
- `compose.yml` remains available when you still want a disposable PostgreSQL container
