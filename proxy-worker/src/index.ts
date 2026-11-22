export interface Env {
  /** Secret stored in Cloudflare Worker secret store (`wrangler secret put ZHIPU_API_KEY`). */
  ZHIPU_API_KEY: string;
  /** Firebase project ID used to validate `iss` and `aud` claims on Firebase ID tokens. */
  FIREBASE_PROJECT_ID: string;
  /** Optional per-user requests-per-minute limit (defaults to "20"). */
  RATE_LIMIT_PER_MINUTE?: string;
  /** Optional upstream base URL override for testing. */
  ZHIPU_UPSTREAM_URL?: string;
}

interface JwtHeader {
  alg: string;
  kid?: string;
  typ?: string;
}

interface FirebaseJwtPayload {
  iss: string;
  aud: string;
  sub: string;
  exp: number;
  iat: number;
}

interface GoogleJwksResponse {
  keys: JsonWebKey[];
}

const GOOGLE_SECURETOKEN_JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
const DEFAULT_ZHIPU_CHAT_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
const ALLOWED_MODELS = new Set(["glm-4-flash", "glm-4v"]);

// In-isolate JWK cache and sliding-window rate limiter per Firebase UID
let cachedJwks: { keys: Map<string, CryptoKey>; expiresAtMs: number } | null = null;
const userRateWindows = new Map<string, number[]>();

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") {
      return jsonResponse({ status: "ok", project: env.FIREBASE_PROJECT_ID }, 200);
    }

    if (request.method !== "POST" || !url.pathname.endsWith("/chat/completions")) {
      return jsonResponse({ error: "Not found. Expected POST /chat/completions" }, 404);
    }

    if (!env.ZHIPU_API_KEY) {
      return jsonResponse({ error: "Server misconfiguration: missing ZHIPU_API_KEY secret" }, 500);
    }

    // 1. Authenticate caller via Firebase ID Token (RS256 JWT)
    const authHeader = request.headers.get("Authorization") ?? "";
    const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
    if (!idToken) {
      return jsonResponse({ error: "Missing Authorization: Bearer <firebase_id_token>" }, 401);
    }

    let uid: string;
    try {
      const payload = await verifyFirebaseIdToken(idToken, env.FIREBASE_PROJECT_ID);
      uid = payload.sub;
    } catch (err) {
      const message = err instanceof Error ? err.message : "Invalid Firebase ID token";
      return jsonResponse({ error: `Unauthorized: ${message}` }, 401);
    }

    // 2. Enforce per-user sliding-window rate limit
    const limitPerMin = Number.parseInt(env.RATE_LIMIT_PER_MINUTE ?? "20", 10) || 20;
    if (!checkAndRecordRateLimit(uid, limitPerMin)) {
      return jsonResponse(
        { error: "Rate limit exceeded. Please wait before sending more requests." },
        429,
        { "Retry-After": "60" },
      );
    }

    // 3. Validate request payload and model allowlist
    let bodyJson: { model?: string; stream?: boolean; [key: string]: unknown };
    try {
      bodyJson = (await request.json()) as { model?: string; stream?: boolean };
    } catch {
      return jsonResponse({ error: "Invalid JSON request body" }, 400);
    }

    const requestedModel = typeof bodyJson.model === "string" ? bodyJson.model : "glm-4-flash";
    if (!ALLOWED_MODELS.has(requestedModel)) {
      return jsonResponse(
        { error: `Model '${requestedModel}' is not permitted by this proxy.` },
        400,
      );
    }

    // 4. Forward request to Zhipu AI and stream SSE / JSON back transparently
    const upstreamUrl = env.ZHIPU_UPSTREAM_URL ?? DEFAULT_ZHIPU_CHAT_URL;
    const upstreamResp = await fetch(upstreamUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${env.ZHIPU_API_KEY}`,
      },
      body: JSON.stringify(bodyJson),
    });

    const isSse = Boolean(bodyJson.stream) && upstreamResp.ok;
    const responseHeaders = new Headers(corsHeaders());
    responseHeaders.set(
      "Content-Type",
      isSse
        ? "text/event-stream; charset=utf-8"
        : (upstreamResp.headers.get("Content-Type") ?? "application/json"),
    );
    if (isSse) {
      responseHeaders.set("Cache-Control", "no-cache");
      responseHeaders.set("Connection", "keep-alive");
    }

    return new Response(upstreamResp.body, {
      status: upstreamResp.status,
      headers: responseHeaders,
    });
  },
};

/**
 * Verifies a Firebase Authentication ID token (RS256 JWT) against Google's SecureToken JWKS.
 */
export async function verifyFirebaseIdToken(
  token: string,
  firebaseProjectId: string,
): Promise<FirebaseJwtPayload> {
  const segments = token.split(".");
  if (segments.length !== 3) {
    throw new Error("Malformed JWT");
  }

  const [headerB64, payloadB64, signatureB64] = segments;
  const header = JSON.parse(decodeBase64UrlToString(headerB64)) as JwtHeader;
  if (header.alg !== "RS256" || !header.kid) {
    throw new Error("Unsupported JWT algorithm or missing kid");
  }

  const payload = JSON.parse(decodeBase64UrlToString(payloadB64)) as FirebaseJwtPayload;
  const nowSec = Math.floor(Date.now() / 1000);
  const expectedIssuer = `https://securetoken.google.com/${firebaseProjectId}`;

  if (payload.iss !== expectedIssuer) {
    throw new Error(`Invalid issuer: ${payload.iss}`);
  }
  if (payload.aud !== firebaseProjectId) {
    throw new Error(`Invalid audience: ${payload.aud}`);
  }
  if (!payload.sub || typeof payload.sub !== "string") {
    throw new Error("Missing subject (uid) claim");
  }
  if (typeof payload.exp !== "number" || payload.exp <= nowSec) {
    throw new Error("Token expired");
  }

  const publicKey = await getGooglePublicKey(header.kid);
  const signedData = new TextEncoder().encode(`${headerB64}.${payloadB64}`);
  const signatureBytes = decodeBase64UrlToBytes(signatureB64);

  const isValid = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    publicKey,
    signatureBytes,
    signedData,
  );
  if (!isValid) {
    throw new Error("Invalid JWT signature");
  }

  return payload;
}

async function getGooglePublicKey(kid: string): Promise<CryptoKey> {
  const now = Date.now();
  if (!cachedJwks || cachedJwks.expiresAtMs <= now || !cachedJwks.keys.has(kid)) {
    const resp = await fetch(GOOGLE_SECURETOKEN_JWKS_URL);
    if (!resp.ok) {
      throw new Error(`Failed to fetch Google JWKS (${resp.status})`);
    }
    const jwks = (await resp.json()) as GoogleJwksResponse;
    const keyMap = new Map<string, CryptoKey>();
    for (const jwk of jwks.keys) {
      const jwkWithKid = jwk as JsonWebKey & { kid?: string };
      if (jwkWithKid.kid && jwk.kty === "RSA") {
        const cryptoKey = await crypto.subtle.importKey(
          "jwk",
          jwk,
          { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
          false,
          ["verify"],
        );
        keyMap.set(jwkWithKid.kid, cryptoKey);
      }
    }
    cachedJwks = { keys: keyMap, expiresAtMs: now + 3_600_000 };
  }

  const key = cachedJwks.keys.get(kid);
  if (!key) {
    throw new Error(`Unknown JWK kid: ${kid}`);
  }
  return key;
}

export function checkAndRecordRateLimit(
  uid: string,
  limitPerMinute: number,
  nowMs: number = Date.now(),
): boolean {
  const windowStart = nowMs - 60_000;
  const recent = (userRateWindows.get(uid) ?? []).filter((ts) => ts > windowStart);
  if (recent.length >= limitPerMinute) {
    userRateWindows.set(uid, recent);
    return false;
  }
  recent.push(nowMs);
  userRateWindows.set(uid, recent);
  return true;
}

function decodeBase64UrlToBytes(base64Url: string): Uint8Array {
  const padded = base64Url
    .replace(/-/g, "+")
    .replace(/_/g, "/")
    .padEnd(Math.ceil(base64Url.length / 4) * 4, "=");
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function decodeBase64UrlToString(base64Url: string): string {
  return new TextDecoder().decode(decodeBase64UrlToBytes(base64Url));
}

function corsHeaders(): Record<string, string> {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
  };
}

function jsonResponse(
  body: Record<string, unknown>,
  status: number,
  extraHeaders: Record<string, string> = {},
): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...corsHeaders(),
      ...extraHeaders,
    },
  });
}
