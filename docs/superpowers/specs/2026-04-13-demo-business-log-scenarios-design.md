# Demo Business Log Scenarios Design

## Goal

Add believable demo business log scenarios that stream through the existing backend log pipeline and frontend `Live Logs` screen, while reducing noisy Docker socket messages during local `dev` work.

The result should help with dissertation demos:
- choose a demo runtime target
- watch realistic business events arrive in `Live Logs`
- show `INFO`, `WARN`, and `ERROR` transitions in a recognizable order
- avoid distracting Docker transport noise when Docker is not part of the demo

## Scope

This design covers:
- two demo runtime targets: `orders-demo` and `restaurant-demo`
- backend generation of real log lines into dedicated demo log files
- manual demo scenario triggering through a backend endpoint
- optional auto-start in `dev` through config
- Docker-noise reduction for local development

This design does not cover:
- a new frontend page for scenario control
- persistence of demo events in the database
- real payment, restaurant, or courier domain logic
- real personal data

## Desired Demo Experience

During a demo, the operator should be able to:

1. Start the backend in `dev`
2. Open `Runtime targets`
3. See `orders-demo` and `restaurant-demo`
4. Open `Live Logs` for one of those targets
5. Trigger a scenario
6. Watch realistic business logs appear as if they came from live services

The logs should look operational rather than synthetic. They should resemble service logs from a food delivery system with order creation, payment, restaurant acceptance, preparation, and delivery-related outcomes.

## Runtime Targets

Two demo local targets are introduced:

- `orders-demo`
  - service-like role: customer-facing order lifecycle and payment state
  - log file: dedicated demo log file
- `restaurant-demo`
  - service-like role: restaurant acceptance and kitchen preparation state
  - log file: dedicated demo log file

These targets should use the same `FILE_TAIL` flow as existing local services so no frontend protocol changes are needed.

## Scenario Model

The demo uses ordered scripted scenarios rather than random log generation.

Each scenario consists of timed steps. Every step writes a real log line into one of the demo target files.

### Baseline scenario

Primary happy-path story:

1. customer creates order
2. payment authorized
3. order confirmed by `orders-demo`
4. restaurant receives order
5. restaurant accepts order
6. kitchen starts preparation
7. kitchen marks order ready
8. courier assigned
9. order handed off
10. order delivered

### Variants

To make the demo more convincing, at least two non-happy-path variants should exist:

- payment delay / retry
- restaurant preparation delay

These variants should introduce `WARN` and optionally one `ERROR` without making the whole scenario feel broken.

## Log Format Expectations

The generator should write plain log lines that fit the existing parser and look believable in the UI.

Example kinds of messages:

- `INFO Order created orderId=ORD-20260413-1001 customer=\"Aruzhan S.\" phone=\"+7 700 *** 12 34\" restaurant=\"Tokyo Bowl\" amount=8450`
- `INFO Payment authorized orderId=ORD-20260413-1001 paymentId=PAY-88421 method=CARD`
- `WARN Payment confirmation delayed orderId=ORD-20260413-1001 gateway=KaspiPay retry=1`
- `INFO Restaurant accepted orderId=ORD-20260413-1001 etaMinutes=24`
- `WARN Kitchen load elevated restaurantId=RST-14 activeOrders=18`
- `INFO Order delivered orderId=ORD-20260413-1001 courier=\"Nurlan T.\"`

The exact line format can stay compatible with current backend parsing rules, but each message should:
- start with a short action phrase
- include stable identifiers like `orderId`
- include a few business attributes for realism
- avoid raw JSON blobs unless the existing parser requires them

## Data Safety

All people and identifiers must be explicitly demo-only:

- use synthetic masked names like `Aruzhan S.`, `Nurlan T.`
- use masked phones like `+7 700 *** 12 34`
- use synthetic restaurants such as `Tokyo Bowl`, `Chef Plov`, `Neo Pizza`
- use synthetic order and payment ids

No real personal data is allowed in generated demo logs.

## Control Model

### Manual trigger

A backend endpoint should trigger scenario generation on demand.

Recommended shape:
- `POST /api/demo/scenarios/orders/start`
- optional request body or query parameter for scenario variant

The endpoint should return quickly after scheduling the scenario rather than blocking until all log steps are written.

### Optional auto-start

For dissertation demos, auto-start should be available behind config.

Recommended config shape:
- `app.demo.enabled`
- `app.demo.auto-start`
- `app.demo.step-delay-ms`

If auto-start is off, demo targets may still exist, but logs only begin when triggered manually.

## Implementation Shape

Recommended backend units:

- `DemoScenarioProperties`
  - holds enablement flags, auto-start, delays, log file paths
- `DemoRuntimeTargetConfig`
  - contributes default local targets for `orders-demo` and `restaurant-demo`
- `DemoScenarioService`
  - starts scenarios and sequences steps
- `DemoScenarioWriter`
  - appends lines to the correct demo log file
- `DemoScenarioController`
  - manual trigger API

This keeps demo generation isolated from auth, runtime target CRUD, and log streaming internals.

## Docker Noise Reduction

Two noisy messages should be addressed for `dev` demos:

- Apache HTTP client retry noise from Docker transport internals
- repeated `Docker discovery skipped because Docker socket is unavailable`

Desired behavior:

- third-party Docker client retry logs should be suppressed through logging configuration in `dev`
- our own Docker skip message should not spam every poll/refresh cycle

Recommended approach:

- set the relevant third-party logger to `ERROR` or `OFF` in `dev`
- reduce our own missing-socket message from repeated `WARN` spam to either:
  - one-time `INFO` on first detection, or
  - `DEBUG` for repeated detections after the first one

The goal is to keep local demos focused on business scenarios instead of infrastructure noise that is irrelevant when Docker is intentionally unused.

## Error Handling

If demo log file creation fails:
- log one clear backend `ERROR`
- keep the app running
- return a failure response from the manual trigger endpoint

If a scenario is triggered while another one is already running:
- either reject with a clear response, or
- queue the next scenario

Recommended default:
- reject concurrent starts per scenario target with a clear message

## Testing Strategy

Coverage should include:

- demo target registration through runtime defaults/config
- scenario trigger endpoint behavior
- writing lines to the correct log files
- order and timing of generated scenario steps
- Docker-noise suppression logic
- safety check that generated values are synthetic/masked

## Success Criteria

This design is successful when:

- `orders-demo` and `restaurant-demo` appear in `Runtime targets`
- starting a scenario writes real lines to their log files
- `Live Logs` displays those lines without frontend changes to the transport
- the scenario reads like a believable food-delivery business flow
- repeated Docker socket noise no longer dominates local demo logs
