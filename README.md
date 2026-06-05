# BlueWater Agentic AI Revenue Prediction Model

BlueWater is a standalone Kotlin portfolio project that shows how an AI-assisted backend can help a marketing organization forecast revenue before launch, then learn from actual results after execution.

The project is intentionally shaped for hiring review:

- easy to run locally
- clear business use case
- clean Ktor service boundaries
- Koog-based workflow orchestration
- mocked persistence by default, with Mongo-style seams already in place

## The business problem

Marketing leaders rarely need another dashboard. They need better prioritization.

BlueWater is built around that problem:

1. take a short revenue-growth brief
2. generate forecast-ready initiative ideas
3. estimate upside and confidence
4. record actual results later
5. audit forecast accuracy
6. create reusable calibration guidance for the next cycle

That makes the system more useful than a one-shot content generator. It behaves like a closed-loop planning tool.

## Why this is a strong portfolio project

This repo demonstrates three things at once:

- product thinking: the AI output is tied to planning and prioritization, not novelty
- technical judgment: the agent layer is bounded by typed contracts and deterministic parsing
- business empathy: the project focuses on forecast quality, explainability, and learning loops

For a hiring marketing head, the signal is straightforward: this is not AI for show. It is AI applied to pipeline planning, post-campaign review, and decision quality.

## What the project does

### 1. Revenue forecast pipeline

`POST /api/predictions/run`

Takes a concise brief and returns multiple revenue initiative ideas with:

- predicted revenue
- confidence score
- target segment
- recommended channel motion
- rationale tied to historical context

The workflow is implemented as a Koog-shaped pipeline in `koog/predictRevenue/`.

### 2. Prediction audit pipeline

`POST /api/predictions/{id}/audit`

Takes actual revenue for a completed initiative and:

- measures prediction error
- classifies overprediction or underprediction
- explains likely miss reasons
- generates updated calibration rules
- persists reusable forecast-learning records

The workflow is implemented in `koog/auditPredictions/`.

### 3. Mongo-ready persistence with local-safe defaults

The public version runs without a live database. It ships with:

- a mocked Mongo-shaped store
- seeded sample initiatives
- seeded calibration rules

That keeps the repo easy to review and run, while preserving a production-ready separation between:

- route layer
- domain layer
- Koog workflow layer
- persistence layer

## Architecture

```text
src/main/kotlin/com/bluewater/revenuepredictor/
  api/                 HTTP routes
  config/              local runtime configuration
  domain/              typed business models and API contracts
  koog/
    predictRevenue/    pre-launch forecasting pipeline
    auditPredictions/  post-launch learning pipeline
    shared/            shared Koog and JSON helpers
  mongo/               mocked Mongo-shaped database surface
  repository/          persistence adapters and seeded data
```

## Technology choices

- `Kotlin` for a concise, strongly typed backend
- `Ktor` for a small, readable HTTP service
- `Koog` for agent-style prediction and audit orchestration
- `kotlinx.serialization` for deterministic wire contracts
- `MongoDB Community` compatibility via repository seams and Mongo-shaped document modeling

## Local run

### Requirements

- JDK 17+
- Gradle or the included Gradle wrapper

### Start the server

```bash
./gradlew run
```

The API starts on `http://localhost:8080` by default.

### Environment variables

See `.env.example`.

Important defaults:

- `BLUEWATER_PORT=8080`
- `BLUEWATER_MONGO_URI=mongodb://localhost:27017/bluewater_predictor`
- `BLUEWATER_MONGO_DATABASE=bluewater_predictor`

The public repo does **not** require a real Mongo instance to run. The default mode uses mocked persistence.

If you provide live LLM settings:

- `BLUEWATER_LLM_API_KEY`
- `BLUEWATER_LLM_BASE_URL`
- `BLUEWATER_LLM_MODEL`

the Koog prompt calls can run in live mode. Without them, the project stays runnable via deterministic mock behavior.

## Example API flow

### Health check

```bash
curl http://localhost:8080/
```

### Run a forecast

```bash
curl -X POST http://localhost:8080/api/predictions/run \
  -H "Content-Type: application/json" \
  -d '{
    "brief": "Goal: Increase qualified pipeline for AI platform offers\nAudience: Marketing leaders at mid-market SaaS companies\nIdeas: 3\nSolutions: GPUs, App Runtime"
  }'
```

### Review seeded initiatives

```bash
curl http://localhost:8080/api/initiatives
```

### Audit a forecast after results arrive

```bash
curl -X POST http://localhost:8080/api/predictions/bw-init-003/audit \
  -H "Content-Type: application/json" \
  -d '{
    "actualRevenueUsd": 163000,
    "influencedAccounts": 17,
    "performanceNotes": "Strong response from platform teams already budgeting for migration."
  }'
```

## What a reviewer should notice

### For marketing leadership

- the forecast is tied to pipeline value, not generic AI copy
- confidence is treated as a first-class planning input
- the system learns from actuals instead of treating every campaign as a fresh guess
- the architecture supports iterative planning workflows, not just content generation

### For engineering leadership

- Koog is used in bounded workflow stages, not spread through the whole codebase
- parsing and persistence stay in Kotlin, not hidden inside prompts
- the repository layer can swap from mock mode to Mongo-backed mode without a route rewrite
- the codebase is compact enough to review quickly and still shows senior-level separation of concerns

## Public-safe scope

This repository is intentionally clean for public review:

- no proprietary company names
- no private links
- no internal documentation systems
- no external infrastructure required by default

Everything in the repo is self-contained and safe to show in interviews, portfolio reviews, and public GitHub discussions.

## Next extension points

If this were expanded beyond the portfolio version, the next practical steps would be:

1. replace the mocked store with a real MongoDB Community adapter
2. add authentication and user-scoped workspaces
3. persist richer audit histories and confidence calibration trends
4. add a small frontend for briefing, review, and post-launch analysis

---

BlueWater is meant to show applied judgment: how to design an AI-assisted planning system that a revenue team could actually use, and how to package that system in a codebase that another engineer can review in one sitting.
