# ADR 0001: Cloudflare Worker Proxy with Firebase ID Token Verification

- **Status:** Accepted
- **Date:** 2026-11-22

## Context

In the initial coursework version of NutriTrack, the Zhipu AI (`glm-4-flash` / `glm-4v`) API key was injected into `BuildConfig` from `local.properties` and compiled directly into the APK. Furthermore, OkHttp was configured with `HttpLoggingInterceptor.Level.BODY` in all builds, which dumped the `Authorization: Bearer <key>` header and Base64-encoded food photos into `logcat` and buffered the entire HTTP response in memory (breaking SSE streaming).

Anyone with the APK could decompile `BuildConfig.java` and extract the upstream LLM API key in seconds.

We considered two serverless proxy options:
1. **Firebase Cloud Functions (2nd Gen):** Native integration with Firebase Auth context, but requires upgrading the Firebase project from the free Spark plan to the pay-as-you-go Blaze billing plan.
2. **Cloudflare Workers (TypeScript):** Free tier includes 100,000 requests/day, zero cold-start V8 isolates, native Web Crypto (`crypto.subtle`) for RS256 JWT verification, and native `ReadableStream` pass-through for Server-Sent Events (SSE).

## Decision

We implemented a TypeScript Cloudflare Worker (`proxy-worker/src/index.ts`) that:
1. Authenticates every request by verifying the caller's **Firebase Authentication ID Token (`RS256` JWT)** against Google's SecureToken JWKS (`https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com`), checking `iss`, `aud` (`nutritrack-e74f6`), `exp`, and `sub` (UID).
2. Enforces a per-UID sliding-window rate limit (`RATE_LIMIT_PER_MINUTE`, returning `429 Too Many Requests` with `Retry-After: 60`) and an explicit model allowlist (`glm-4-flash`, `glm-4v`).
3. Injects `Authorization: Bearer ${env.ZHIPU_API_KEY}` from Cloudflare Worker Secrets and pipes the upstream `ReadableStream` back as `text/event-stream` without buffering.
4. Strips `BuildConfig.ZHIPU_API_KEY` from all Android `release` builds (`buildConfigField("String", "ZHIPU_API_KEY", "\"\"")`) and enables R8 shrinking/obfuscation (`isMinifyEnabled = true`, `isShrinkResources = true`).

## Consequences

- **Positive:** Release APKs contain zero third-party secrets; compromised or decompiled clients cannot abuse the Zhipu API key; SSE streaming latency (TTFT) is preserved end-to-end.
- **Negative / Trade-off:** Requires deploying the Worker (`npx wrangler deploy`) for live cloud inference; mitigated by the `demo` product flavor (`FakeLlmClient`) which runs the full ReAct tool-calling loop completely offline.
