# AI-Assisted Engineering Workflow (`docs/ai-workflow.md`)

This document explains how AI coding agents (Claude Code / Antigravity) were directed, constrained, verified, and reviewed while evolving **NutriTrack** from a university coursework submission into a production-grade Android application.

---

## 1. Repository-Level Agent Guardrails (`CLAUDE.md` & `AGENTS.md`)

Rather than prompting ad-hoc, all AI coding sessions are governed by [`CLAUDE.md`](../CLAUDE.md) (mirrored in [`AGENTS.md`](../AGENTS.md)). It establishes:

- **Mandatory verification gate before any commit:**
  ```bash
  ./gradlew detekt lintProdDebug testProdDebugUnitTest testDemoDebugUnitTest assembleProdDebug assembleDemoDebug --console=plain
  ```
- **Architectural invariants:**
  - Single state model (`StateFlow<UiState>`, zero `LiveData` in ViewModels);
  - Explicit Room schema exports (`app/schemas/6.json` → `7.json` → `8.json` → `9.json`) and `MigrationTestHelper` tests without `fallbackToDestructiveMigration`;
  - Strict coroutine cancellation propagation (`CancellationException` must never be swallowed);
  - Security redlines (no `HttpLoggingInterceptor.Level.BODY`, redacted `Authorization` headers, no API keys in release APKs).

---

## 2. Phased Plan → Small Atomic Commits → Automated Gate Review

Work was structured into 6 phases in [`docs/roadmap.md`](roadmap.md), with each phase decomposed into small, independently verifiable commits:

1. **Plan:** Define schema changes, pure-Kotlin domain contracts, and evaluation criteria first.
2. **Execute in small batches:** Keep data/migration changes separate from UI and product-flavor changes.
3. **Automated & Human Review:** Every change must pass **Detekt** (`maxIssues: 0`), **Android Lint**, and **JUnit / MockWebServer / Turbine tests** before landing.

---

## 3. Real Examples of Catching & Correcting AI Mistakes

A critical part of AI-assisted software engineering is auditing AI output for subtle bugs, security flaws, and architectural shortcuts. Below are four real examples caught and fixed in this repository:

### Case 1: Unreviewed AI Inner-Monologue Left in Production Code
- **What happened:** In an early commit (`GenAIRepository.kt:162–179`), an AI assistant left 15 lines of conversational self-debate inside the source file:
  > `// Note: User asked for GLM-4.5V, but let's stick to glm-4v for broader compatibility...`
  > `// Wait, the curl example had "glm-4.5v". I will use "glm-4v" to be safe...`
  > `// Correction: The user explicitly asked to change to GLM-4.5V. I should use "glm-4.5v" if I want to be obedient.`
  It also left functions named `askGemini()` and `askGeminiStream()` even though the backend had already been switched to Zhipu GLM, and left `Thinking(type = "enabled")` (a GLM-4.5V parameter) on a `glm-4v` request.
- **Fix:** Removed the monologue, renamed the API to `generateText()` / `streamText()`, stripped dead Gemini DTOs, and added an explicit rule in `CLAUDE.md` forbidding conversational comments.

### Case 2: `HttpLoggingInterceptor.Level.BODY` Breaking SSE Streaming & Leaking Secrets
- **What happened:** AI-generated networking code enabled `HttpLoggingInterceptor.Level.BODY` unconditionally in `NetworkModule.kt`. This had two severe side effects:
  1. It printed the full `Authorization: Bearer <api_key>` header and Base64-encoded food photos into `logcat` in release builds.
  2. `Level.BODY` buffers the entire HTTP response body in memory before returning it to Retrofit, which completely defeated Server-Sent Events (`@Streaming`) and caused Time-To-First-Token (TTFT) to equal full-response latency.
- **Fix:** Gated the interceptor behind `if (BuildConfig.DEBUG)`, switched the level to `HttpLoggingInterceptor.Level.HEADERS`, and called `redactHeader("Authorization")`.

### Case 3: `TimeoutCancellationException` Swallowed by `catch (ce: CancellationException)`
- **What happened:** In `NutriCoachAgent.runTurn()`, we wrapped the ReAct loop in `withTimeout(timeoutMs)` and added `catch (ce: CancellationException) { throw ce }` before `catch (t: Throwable)` to follow structured concurrency rules.
- **How we caught it:** Our unit test `NutriCoachAgentTest.agent returns Failed when turn exceeds timeoutMs` failed with an uncaught `TimeoutCancellationException`. Because `TimeoutCancellationException` is a subclass of `CancellationException`, rethrowing all `CancellationException`s caused internal agent timeouts to crash the caller instead of returning `AgentRunOutcome.Failed`.
- **Fix:** Explicitly caught `TimeoutCancellationException` *before* `CancellationException` to map internal step timeouts to `AgentRunOutcome.Failed` while still rethrowing external coroutine cancellations.

### Case 4: Substring Collision (`"catalog"` vs `"log "`) Caught by the Tool-Selection Eval Suite
- **What happened:** In `FakeLlmClient.routeUserPromptToTools()`, write-intent detection checked `lower.contains("log ")`.
- **How we caught it:** When we ran the 12-case evaluation benchmark (`AgentToolEvalTest`), case `eval_06_search_catalog_tofu` (*"Search the food catalog for tofu nutrition."*) failed with 83.3% accuracy: the word `"catalog "` ends with `"log "`, causing a read-only catalog search to be misrouted to the `log_meal` write tool!
- **Fix:** Replaced naive substring matching with word-boundary regex matching (`\b(log|record|ate)\b`), bringing benchmark accuracy on `AgentToolEvalTest` to **12/12 (100%)**.
