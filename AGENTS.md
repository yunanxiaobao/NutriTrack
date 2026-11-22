# CLAUDE.md — AI Coding Agent Guidelines for NutriTrack

This file defines the project conventions, verification gates, and architectural guardrails for AI coding assistants (Claude Code, Antigravity, Cursor, GitHub Copilot) working on **NutriTrack**.

---

## 1. Quick Verification Commands

Before considering any task complete or opening a PR, run the full verification suite:

```bash
./gradlew detekt lintProdDebug testProdDebugUnitTest testDemoDebugUnitTest assembleProdDebug assembleDemoDebug --console=plain
```

- **Build offline-ready Demo APK:** `./gradlew assembleDemoDebug`
- **Build Production flavor APK:** `./gradlew assembleProdDebug`
- **Run unit tests:** `./gradlew testProdDebugUnitTest testDemoDebugUnitTest`
- **Static analysis (Kotlin):** `./gradlew detekt` (config in `config/detekt/detekt.yml`, baseline in `app/detekt-baseline.xml`)
- **Android Lint:** `./gradlew lintProdDebug` (baseline in `app/lint-baseline.xml`)

> **Rule:** Never expand `detekt-baseline.xml` or `lint-baseline.xml` to silence new issues introduced by your changes. Fix new findings in-place.

---

## 2. Project Architecture & Tech Stack

- **Package & Namespace:** `com.songshawn.nutritrack`
- **UI Layer (`ui/`):** Jetpack Compose (Material 3), Navigation Compose (`navigation/`). Screens must be stateless where possible, hoisting state to ViewModels.
- **Presentation Layer (`viewmodel/`):** `@HiltViewModel` classes exposing `StateFlow<UiState>`.
  - *Migration note:* Some legacy ViewModels still expose `LiveData`. New or refactored ViewModels **must** use `StateFlow` + immutable `UiState` data classes/sealed interfaces.
- **Data Layer (`data/`):**
  - `repository/`: Single source of truth coordinating Room DAOs, Retrofit services, and Firestore.
  - `room/`: Room SQLite database (`NutriTrackDatabase`, schema exported to `app/schemas/`).
  - `network/`: Retrofit interfaces (`ApiServices.kt`) and wire DTOs (`fruityvice/`, `genai/`).
- **DI (`di/`):** Hilt modules installed in `SingletonComponent`.

---

## 3. Non-Negotiable Engineering Guardrails

1. **No AI Monologue or Stale Artifacts in Code:**
   - Never leave conversational comments (e.g., `"User asked for X, let's stick to Y..."`, `"Wait, actually..."`, or `"TODO: ask user"`) in source files.
   - Comments must explain *why* a technical decision exists, not narrate the editing process.
2. **Coroutine & Flow Hygiene:**
   - **Never swallow `CancellationException`.** Any broad `catch (e: Exception)` or `catch (t: Throwable)` in a `suspend` function or `Flow` builder **must** rethrow `CancellationException` first.
   - Always close streaming `ResponseBody` / `BufferedReader` resources with `.use { ... }` so collector cancellation releases the underlying socket immediately.
   - Inject dispatchers or use structured concurrency (`viewModelScope`, application scope); never launch unscoped fire-and-forget `CoroutineScope(Dispatchers.IO).launch` tasks.
3. **Security & Logging:**
   - Never log HTTP request/response bodies (`HttpLoggingInterceptor.Level.BODY` is forbidden because it dumps Base64 images/prompts into logcat and buffers SSE streams).
   - Keep HTTP logging gated behind `BuildConfig.DEBUG` with `redactHeader("Authorization")`.
   - Never hardcode secrets or API keys in tracked source files or Gradle scripts. Local keys belong in `local.properties` (untracked) under `zhipu.apiKey`.
4. **Database & Migrations (Room):**
   - `exportSchema = true` is enabled and schemas are tracked under `app/schemas/`. Whenever you bump `NutriTrackDatabase.version`, commit the newly exported `<version>.json` schema file and provide an explicit `Migration(from, to)` covered by a migration test.
   - Do not rely on destructive migrations to paper over schema changes.
5. **Dependencies (`gradle/libs.versions.toml`):**
   - Declare all dependencies and plugins in `gradle/libs.versions.toml`. Do not inline raw version strings in `app/build.gradle.kts`.

---

## 4. PR & Commit Workflow

- Keep PRs small, focused, and independently verifiable (1–3 PRs per roadmap phase in `docs/roadmap.md`).
- Follow Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `build:`, `ci:`, `docs:`, `chore:`).
- Update `docs/roadmap.md` checkboxes as items land.
