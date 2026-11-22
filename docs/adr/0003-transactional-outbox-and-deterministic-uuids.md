# ADR 0003: Transactional Outbox Pattern with Deterministic UUIDs for Cloud Sync

- **Status:** Accepted
- **Date:** 2026-11-22

## Context

Previously, `CloudSyncRepository` pushed questionnaire answers and saved NutriCoach tips directly to Firestore inside a `runCatching { ... }.onFailure { Log.w(...) }` block:
1. If the device was offline or the network call timed out, the exception was logged and the write was **permanently lost**.
2. Tip document IDs were constructed as `"${tip.timestamp.time}_${tip.id}"`. Because `NutriCoachTipEntity.id` was `0` prior to auto-generation or could vary across devices, retrying a sync could create duplicate documents in Firestore.

## Decision

We introduced a durable **Transactional Outbox** (`SyncOutboxEntity` in Room table `sync_outbox`, added via `MIGRATION_8_9`) paired with **WorkManager** (`OutboxSyncWorker`):

1. **Local-First Durable Staging:** Every cloud-bound mutation (`pushQuestionnaire`, `pushTip`) first writes a `SyncOutboxEntity` row into Room.
2. **Deterministic UUID Idempotency:** Document IDs are derived deterministically via `UUID.nameUUIDFromBytes(namespaceKey)` (e.g., `"tip:$uid:${timestamp}:${contentHash}"`). Enqueuing or retrying the same logical item any number of times overwrites the same `documentUuid` in both `sync_outbox` and Firestore (`users/{uid}/tips/{documentUuid}`).
3. **Exponential Backoff via WorkManager:** `OutboxSyncWorker` runs under `NetworkType.CONNECTED` constraints with `BackoffPolicy.EXPONENTIAL` (15s initial backoff). Successful items are deleted from `sync_outbox`; transient failures increment `attemptCount` and return `Result.retry()`.

## Consequences

- **Positive:** Zero silent data loss during offline usage; strict idempotency across retries; full observability of pending sync items in Room.
- **Negative / Trade-off:** Adds one additional Room table (`sync_outbox`) and migration (`MIGRATION_8_9`).
