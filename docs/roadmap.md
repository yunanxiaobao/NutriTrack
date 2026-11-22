# Roadmap

NutriTrack is being taken from coursework to a production-style app in small,
reviewable PRs. Each phase is split into 1–3 PRs; phases 4 and 5 are interleaved with
the others.

## Phase 0 — Clean foundations

- [x] Rename package to `com.songshawn.nutritrack`; remove student ID
- [x] Remove leftover AI-generated "thinking out loud" comments and dead code
- [x] Rename misleading APIs (`askGemini` → `generateText` / `streamText`)
- [x] Clean up the version catalog: remove duplicates, unused libraries and the `x.y.z` placeholder; pin stable versions instead of an accidental RC
- [x] HTTP logging only in debug builds, `Authorization` header redacted, no BODY logging (it buffered SSE streams)
- [x] CI with GitHub Actions: build, unit tests, Android Lint, detekt
- [x] `AGENTS.md` / `CLAUDE.md`: project conventions for AI coding agents

## Phase 1 — Food logging (database, Android)

- [x] Tables: food catalog, meals, meal items, daily scores (foreign keys, indices, `@Relation`, `@Transaction`)
- [x] Prepopulated food catalog from a public dataset; FTS4 search
- [x] Pure-Kotlin HEIFA-style scoring engine with thorough unit tests
- [x] Proper migrations (6 → 7) tested with `MigrationTestHelper`; exported schemas; no destructive fallback
- [x] 7/30-day trend charts; clinician stats via SQL aggregates
- [x] `demo` product flavor: seeded demo user with 30 days of data plus a fake LLM, runs offline without a key

## Phase 2 — NutriCoach agent

- [x] `LlmClient` abstraction (chat, streaming, tool calling); Zhipu and fake implementations
- [x] Coroutine-based tool-calling loop with step limit, timeout, parallel tool calls and cancellation
- [x] User confirmation for write tools (e.g. "log a meal")
- [x] Meal photo → structured items (JSON) → confirmed meal
- [x] Conversation memory in Room with summarisation
- [x] Visible tool-call trace in the chat UI
- [x] Tool-selection evaluation set
- [ ] (Optional) on-device RAG over dietary guidelines with citations

## Phase 3 — Move the API key off the device

- [x] Cloudflare Worker proxy: verifies Firebase ID tokens, per-user rate limiting, SSE pass-through
- [x] App sends only the Firebase ID token; R8 enabled for release
- [x] Replace the hard-coded clinician password (custom claims or demo-flavor only)

## Phase 4 — Concurrency, background work, offline

- [x] WorkManager sync with an outbox table, exponential backoff, idempotent UUID document IDs
- [x] Injected application `CoroutineScope` and dispatchers; `Mutex` where needed
- [x] Debounced search with `flatMapLatest`
- [x] Throttled recomposition while streaming (measured); downscale images before upload
- [x] Daily reminder notification

## Phase 5 — Architecture and tests

- [ ] Single state model (`StateFlow` + `UiState`); split oversized files
- [ ] Tests: ViewModels (Turbine), DAOs and migrations, SSE parsing (MockWebServer), agent loop, key Compose screens
- [ ] Kover coverage ≥ 70% for data and domain layers

## Phase 6 — Presentation (ongoing)

- [ ] README with architecture diagram and GIFs
- [ ] ADRs in `docs/adr/`
- [ ] `docs/ai-workflow.md`: how AI coding tools are used and reviewed on this project
- [ ] GitHub Release with demo APK and a short demo video
