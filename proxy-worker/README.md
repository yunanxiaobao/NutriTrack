# NutriTrack LLM Proxy (Cloudflare Worker)

Eliminates client-side API key exposure by holding `ZHIPU_API_KEY` exclusively in Cloudflare Worker Secrets while authenticating Android clients with their **Firebase Authentication ID Token (RS256 JWT)**.

## Request Flow

1. The Android app requests a short-lived Firebase ID token from `FirebaseAuth.getInstance().currentUser.getIdToken(false)` and calls `POST /chat/completions` with `Authorization: Bearer <firebase_id_token>`.
2. The Worker verifies the JWT RS256 signature against Google's SecureToken JWKS (`https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com`) and checks `iss`, `aud` (`nutritrack-e74f6`), `exp`, and `sub` (UID).
3. The Worker applies a sliding-window rate limit per Firebase UID (`RATE_LIMIT_PER_MINUTE`, default 20 req/min) and validates that the requested `model` is in the allowlist (`glm-4-flash`, `glm-4v`).
4. The Worker forwards the payload to Zhipu AI (`https://open.bigmodel.cn/api/paas/v4/chat/completions`) with `Authorization: Bearer ${env.ZHIPU_API_KEY}` and pipes the response (`application/json` or `text/event-stream` for SSE streaming) back with zero buffering.

## Deploying

```bash
cd proxy-worker
npm install
npx wrangler secret put ZHIPU_API_KEY
npx wrangler deploy
```

Then configure the deployed Worker URL in the Android project's `local.properties`:

```properties
llm.proxyUrl=https://nutritrack-llm-proxy.<your-subdomain>.workers.dev/
```
