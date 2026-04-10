# Auth And Account Plan

## Current Status

Completed backend work:
- `Task 1` done: security dependencies added
- `Task 2` done: `SecurityConfig`, `JwtService`, base auth package structure added
- `Task 3` done: `V2__auth_init.sql` added with `users`, `roles`, `user_roles`, `refresh_tokens`
- `Task 4` done: `UserEntity`, `RoleEntity`, `RefreshTokenEntity` and repositories added
- `Task 5` done: auth/account DTOs and validation added
- `Task 6` done: `register`, `login`, `refresh`, `logout` implemented
- `Task 8` done: `GET /api/auth/me`, `GET /api/account`, `PATCH /api/account`, `PATCH /api/account/password` implemented
- `Task 9` done: protected REST routes now return explicit `401` for unauthenticated requests
- `Task 11` done: auth security integration tests and repository integration tests added
- `Task 17` done: websocket log stream handshake is protected by JWT
- route protection baseline is active through JWT authentication filter
- tests for auth/account layer are added and `./gradlew test` is green

Completed backend commits:
- `1d0a427` `feat: add auth security foundation and schema`
- `96fe432` `feat: add auth entities and repositories`
- `c7d9d78` `feat: add auth request and response models`
- `00affd0` `feat: implement auth service and controller`
- `caccaf1` `feat: add current-user account endpoints`
- `2ece559` `test: harden auth security and repository coverage`
- `2aa81a4` `feat: protect websocket log stream with jwt`

Remaining backend work:
- decide whether to rotate and hash refresh tokens exactly as-is or introduce a dedicated refresh token service
- after backend auth stabilizes, connect frontend login/register/account flow

## Goal
Add a first real authentication layer to `DiagnosticServiceAI`, then connect the frontend to it so the product has:
- registration
- login
- account/profile
- roles
- protected API access
- a real profile menu instead of placeholder UI

This plan intentionally starts with backend work and only then moves to frontend integration.

## Constraints
- Keep the current monolith structure.
- Do not introduce OAuth or external identity providers in this wave.
- Use JWT access tokens plus refresh tokens.
- Support a single chosen role at registration time in the UI, while keeping backend storage extensible for multiple roles.
- Preserve the current monitoring architecture and add auth around it.

## Phase 1: Dependencies And Security Baseline

### Task 1: Add security dependencies
Files:
- `build.gradle`

Changes:
- Add `spring-boot-starter-security`
- Add JWT library dependencies

Verification:
- `./gradlew test`

### Task 2: Add security package structure
New backend packages:
- `auth`
- `security`

Expected classes:
- `SecurityConfig`
- `JwtService`
- `JwtAuthenticationFilter`
- `CustomUserDetailsService`
- `AuthService`
- `RefreshTokenService`

Verification:
- project compiles

## Phase 2: Database Auth Model

### Task 3: Add auth migration
New file:
- `src/main/resources/db/migration/V2__auth_init.sql`

Tables:
- `users`
- `roles`
- `user_roles`
- `refresh_tokens`

Suggested columns:
- `users`: `id`, `email`, `username`, `password_hash`, `status`, `created_at`, `updated_at`
- `roles`: `id`, `code`, `title`
- `user_roles`: `user_id`, `role_id`
- `refresh_tokens`: `id`, `user_id`, `token_hash`, `expires_at`, `revoked`, `created_at`

Seed roles:
- `ADMIN`
- `DEVOPS`
- `BACKEND`
- `FRONTEND`
- `ANALYST`
- `QA`

Verification:
- Flyway starts cleanly
- schema validates

### Task 4: Add auth entities and repositories
New entities:
- `UserEntity`
- `RoleEntity`
- `RefreshTokenEntity`

New repositories:
- `UserRepository`
- `RoleRepository`
- `RefreshTokenRepository`

Verification:
- repository integration tests

## Phase 3: Backend Auth API

### Task 5: Add DTOs and validation
New DTOs:
- `RegisterRequest`
- `LoginRequest`
- `AuthResponse`
- `MeResponse`
- `AccountResponse`
- `UpdateAccountRequest`
- `ChangePasswordRequest`

Validation:
- email format
- username length and normalization
- password minimum length
- role code validation

### Task 6: Implement register
Status:
- completed
Endpoint:
- `POST /api/auth/register`

Behavior:
- create user
- hash password with BCrypt
- assign selected role
- return tokens + user summary

### Task 7: Implement login/refresh/logout
Status:
- completed
Endpoints:
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

Behavior:
- issue access + refresh token
- refresh with stored token validation
- revoke refresh token on logout

### Task 8: Implement current user and account
Status:
- completed
Endpoints:
- `GET /api/auth/me`
- `GET /api/account`
- `PATCH /api/account`
- `PATCH /api/account/password`

Behavior:
- return current user identity
- allow basic account edits
- allow password change with old password confirmation

Verification for Phase 3:
- controller tests
- auth service tests
- negative cases for invalid password, duplicate email, invalid refresh token

## Phase 4: Backend Security Rules

### Task 9: Protect routes
Status:
- completed
Public:
- `/api/auth/register`
- `/api/auth/login`
- `/api/auth/refresh`
- `/actuator/health`

Protected:
- `/api/auth/me`
- `/api/account/**`
- `/api/analytics/**`
- `/api/projects/**`
- websocket log stream handshake

Verification:
- unauthorized requests return `401`
- valid token reaches secured endpoints

### Task 10: Add role awareness
Keep authorization simple in this wave:
- authenticated access first
- role data exposed in responses
- role checks can be added selectively later

Optional first checks:
- only `ADMIN` can list all users in future admin APIs

## Phase 5: Backend Test Coverage

### Task 11: Add auth tests
Status:
- completed
New tests:
- `JwtServiceTest`
- `AuthServiceTest`
- `AccountServiceTest`
- `AuthControllerTest`
- `AccountControllerTest`
- `UserRepositoryIntegrationTest`
- `RefreshTokenRepositoryIntegrationTest`

Verification:
- `./gradlew test`

## Phase 6: Frontend Auth Foundation

### Task 12: Add frontend auth state
Status:
- next
Frontend project:
- `diagnostic-ai-front`

New state:
- `accessToken`
- `refreshToken`
- `currentUser`
- `roles`
- `authStatus`

New responsibilities:
- attach bearer token to API requests
- refresh on `401`
- clear session on invalid refresh

### Task 13: Add auth routes and pages
Status:
- next
New pages:
- `LoginPage`
- `RegisterPage`
- `AccountPage`

Register form fields:
- email
- username
- password
- selected role

### Task 14: Protect frontend routes
Status:
- next
Public:
- login
- register

Protected:
- main shell routes

Behavior:
- unauthenticated user redirected to login
- authenticated user sees main app shell

## Phase 7: Frontend Profile Integration

### Task 15: Replace placeholder profile actions
Status:
- next
Profile dropdown should show:
- user display name
- role
- session status

Actions:
- `Учетные данные` / `Account`
- `Выход` / `Logout`

`Settings` stays as a technical app settings page, but user identity goes to `Account`.

### Task 16: Connect account page
Status:
- next
Account page should support:
- reading current profile
- updating visible account fields
- changing password

## Phase 8: WebSocket Auth

### Task 17: Protect websocket stream
Status:
- completed
Add JWT validation to websocket connection flow.

Options:
- token in query param for first version
- handshake interceptor or pre-connect validation

Requirement:
- unauthorized client must not receive logs

## Recommended Execution Order
1. Task 1
2. Task 2
3. Task 3
4. Task 4
5. Task 5
6. Task 6
7. Task 7
8. Task 8
9. Task 9
10. Task 11
11. Task 12
12. Task 13
13. Task 14
14. Task 15
15. Task 16
16. Task 17

## Verification Gates

### Backend gate
- Flyway migration runs
- auth endpoints pass tests
- secured endpoints reject anonymous access
- `./gradlew test` passes

### Frontend gate
- build passes
- login/register/account flow works
- protected routes redirect correctly
- profile menu reflects real backend identity

## Out Of Scope For This Wave
- OAuth providers
- social login
- admin user management UI
- fine-grained permission matrix
- audit logging
- password recovery via email

## Expected Outcome
At the end of this plan the system will have:
- real registration and login
- role selection at onboarding
- JWT-based session model
- protected backend APIs
- protected frontend app shell
- real account/profile instead of placeholder menu actions
