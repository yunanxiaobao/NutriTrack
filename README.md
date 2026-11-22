# NutriTrack

An Android nutrition-tracking app with an AI nutrition coach, built with
Jetpack Compose, Hilt, Room, Retrofit and Firebase.

> **Status:** actively being extended from a university coursework project into a
> production-style app. See [Roadmap](docs/roadmap.md) for what is done and what is next.

## Features

| Area | What it does |
|---|---|
| Accounts | Email/password sign-up and login via Firebase Authentication; in-app account deletion; offline `demo` flavor with a pre-seeded persona |
| Onboarding | Questionnaire for food-category preferences, eating persona and meal/sleep timing |
| Food Logging | Built-in food nutrition catalog (`assets/food_catalog.json`) with SQLite **FTS4** prefix search, relational meal slots (`@Relation`, `@Transaction`, foreign keys), and live recalculation of daily HEIFA-2013 scores via a pure-Kotlin scoring engine |
| Home & Insights | Live HEIFA-2013 diet-quality score, per-category breakdown, and **7-day / 30-day trend sparkline chart** |
| NutriCoach | Fruit nutrition lookup (Fruityvice) with a stale-while-revalidate Room cache; streaming AI tips over SSE; fruit recognition from a photo with a vision model |
| Cloud sync | Questionnaire answers and saved tips are mirrored to Firestore per user, guarded by [security rules](firestore.rules) |
| Clinician | SQL aggregate (`GROUP BY` / `AVG`) cohort statistics plus AI-generated pattern analysis |

## Tech stack

- **UI:** Jetpack Compose, Material 3, Navigation Compose
- **Architecture:** MVVM, Hilt dependency injection, repository pattern
- **Async:** Kotlin coroutines and Flow (SSE streamed as a cold `Flow`, cancellation-aware)
- **Persistence:** Room (SQLite), SharedPreferences
- **Networking:** Retrofit + OkHttp, Gson
- **Backend:** Firebase Authentication, Cloud Firestore
- **AI:** Zhipu GLM via its OpenAI-compatible API (`glm-4-flash` for text, `glm-4v` for vision)

## Project structure

```
app/src/main/java/com/songshawn/nutritrack/
├── data/
│   ├── models/        # Domain models and enums
│   ├── network/       # Retrofit service interfaces and wire models
│   ├── repository/    # Data sources behind a single API for the ViewModels
│   ├── room/          # Entities, DAOs and the Room database
│   └── store/         # SharedPreferences helpers
├── di/                # Hilt modules
├── navigation/        # NavHost and routes
├── ui/                # Compose screens and components
├── viewmodel/         # ViewModels and UI state
└── util/              # Helpers
```

## Building

Requirements: JDK 17+ (Android Studio's bundled JBR works) and the Android SDK (compileSdk 36).

```bash
git clone https://github.com/yunanxiaobao/NutriTrack.git
cd NutriTrack
./gradlew assembleDemoDebug        # offline-ready demo APK (30-day seeded history + fake LLM)
./gradlew assembleProdDebug        # production flavor debug APK
./gradlew testProdDebugUnitTest    # unit tests (HEIFA scoring engine, CSV, FTS query builder)
```

AI features need a Zhipu API key. Add it to `local.properties` (never commit this file):

```properties
zhipu.apiKey=your-key-here
```

The app builds and runs without a key; only the AI calls will fail.

> [!WARNING]
> The key is currently compiled into the APK, so do not distribute builds that contain a
> real key. Moving the key behind a server-side proxy is planned (roadmap phase 3).

## Provenance

NutriTrack started as an assignment for a mobile-development course at Monash
University. The course provided the HEIFA dataset (`app/src/main/assets/data.csv`) and
the original feature brief (login by patient ID, questionnaire, score screens,
NutriCoach and the clinician dashboard). The coursework version is the first commit in
this repository (September 2025).

Everything after that is my own extension of the project: the Hilt migration, the
switch to Zhipu GLM, SSE streaming, the Room fruit cache, Firebase accounts and sync,
and all work tracked in the [roadmap](docs/roadmap.md).

## License

[MIT](LICENSE)
