# ADR 0002: Coroutine ReAct Tool-Calling Agent with Write-Confirmation Gate

- **Status:** Accepted
- **Date:** 2026-11-22

## Context

The earlier AI coaching feature was labelled "RAG", but simply concatenated the user's static CSV row and questionnaire fields into a single prompt string. It had three major limitations:
1. It could not query live meal logs, search the 34-item FTS4 food catalog, or inspect specific nutrient breakdowns on demand.
2. It could not take actions on the user's behalf (such as logging a meal or scanning a meal plate into structured food entries).
3. It had no conversation memory across turns and no visibility into *how* the model reached its recommendation.

## Decision

We replaced prompt-stuffing with a provider-agnostic `LlmClient` abstraction and a coroutine-based **ReAct (Reason + Act) Agent loop** (`NutriCoachAgent.kt`):

1. **Six Domain Tools (`RoomNutriCoachToolHost`):**
   - Read-only tools: `get_today_summary`, `get_score_breakdown`, `search_food_catalog` (SQLite FTS4), `lookup_fruit_nutrition` (Fruityvice SWR cache), and `get_user_preferences`.
   - State-mutating tool: `log_meal` (`requiresConfirmation = true`).
2. **Parallel Read Execution & Safety Guards:**
   - When the model requests multiple read-only tools in a single turn, `NutriCoachAgent` executes them concurrently via `coroutineScope { calls.map { async { ... } }.awaitAll() }`.
   - The loop enforces `maxSteps = 4` and `withTimeout(25_000L)`, distinguishing `TimeoutCancellationException` (mapped to `AgentRunOutcome.Failed`) from external job `CancellationException` (rethrown to respect structured concurrency).
3. **Human-in-the-Loop Write Confirmation:**
   - Any tool flagged with `requiresConfirmation = true` halts the loop immediately and emits `AgentRunOutcome.AwaitingWriteConfirmation(proposal, traceSteps)`. The database is only mutated after the user reviews the calories/servings and taps **Confirm & Log** in the UI.
4. **Visible Reasoning Trace & Benchmark Evaluation:**
   - Every tool invocation is recorded in `ToolCallTraceStep` (tool name, latency in ms, JSON args, JSON result), persisted in Room (`agent_messages.traceJson`), and rendered as an expandable trace card in the chat UI.
   - Tool selection is continuously regression-tested against a 12-case benchmark (`AgentToolEvalTest.kt`).

## Consequences

- **Positive:** Recommendations are grounded in live Room/FTS4 queries; write actions are safe against hallucinated mutations; interviewers can inspect every tool call and latency directly in the UI.
- **Negative / Trade-off:** Multi-step tool turns require 2 HTTP round-trips when calling a remote LLM; mitigated by parallel `async`/`awaitAll` execution for multi-tool steps.
