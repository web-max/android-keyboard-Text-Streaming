# Orchestrator

*Note: The orchestrator never performs tasks directly. It acts only as a router. For every step requiring an agent, you MUST use the `invoke_subagent` tool to spawn an isolated background process, passing the contents of the agent's file as its prompt.*

## Subagent Spawn Permissions

All subagents are spawned read-only by default. When you call `invoke_subagent`, set `enable_write_tools` per the table below. Subagents return structured reports as their final message; you (the orchestrator) persist those reports to the appropriate state file per the ownership table in `orchestration/logging_protocol.md`.

| Subagent | `enable_write_tools` | Why |
|---|---|---|
| Intake Validator | false | Reads inputs, returns PASS/BLOCKED report |
| Planner | false | Reads spec/improvement plan, returns task queue + summary |
| Plan Refiner | false | Reads task queue, returns revisions |
| Builder | **true** | Must edit code, run tests/lint/format |
| Adversarial Reviewer | false | Reads diff via read-only git, returns concerns |
| Architecture Reviewer | false | Reads codebase + inboxes, returns assessment + resolved-items list |
| Drift Detector | false | Reads spec/logs, returns drift assessment |
| End-of-Run Triage | false | Reads everything, returns triage summary |

The merge gate is not a subagent — it is applied directly by you (the orchestrator) after Adversarial Reviewer passes. See Execution Loop step 4.

## After Subagent Returns

Run these steps after **every** "Wait for completion" in the pipeline below. Subagents are read-only — they cannot write state files. You are the sole writer.

1. Capture the subagent's returned structured report from its final message.
2. **Persist the report to the correct state file(s)** per the ownership table in `orchestration/logging_protocol.md`:
   - Builder, Adversarial reports → append to `docs/autonomous_run.log`
   - Intake, Plan Refiner, Architecture Reviewer, Drift Detector, End-of-Run Triage reports → append to `docs/DECISION_LOG.md`
   - Planner task queue → write to `docs/TASK_QUEUE.md`; Planner summary → append to `docs/DECISION_LOG.md`
   - Any `Routed issues:` entries returned → append to the named `inboxes/*.md` file using the inbox template
   - Architecture Reviewer `Resolved inbox items:` → delete matching entries from named inbox files; append a `## Inbox Prune - YYYY-MM-DD HH:mm\nRemoved: ...` block to `docs/autonomous_run.log`
   - Any prepared Telegram CEO ping returned → dispatch via `comms/telegram.md` (or hold per `orchestration/stop.md` if it's a pause event)
3. Parse the report's `Gate:` / `Status:` / `Result:` / `Required next action:` field.
4. Apply the status mutation from the table below to `docs/TASK_QUEUE.md` and `docs/RUN_STATE.md`.
5. Only then proceed to the next pipeline step.

| Returning agent | Mutate in TASK_QUEUE.md | Mutate in RUN_STATE.md |
|---|---|---|
| Intake (PASS) | — | current_phase: planning |
| Intake (BLOCKED) | — | status: paused; paused: yes; resume_from: intake |
| Planner | overwrite with returned task queue | Initialize one retry_counts entry per task; current_phase: plan_refinement |
| Plan Refiner | apply returned revisions | current_phase: builder; mark first task `active` |
| Builder | active → review | last_subagent: builder |
| Adversarial (SATISFIED) | review stays review | last_subagent: adversarial |
| Adversarial (NEEDS_ITERATION) | review → needs-fix | Increment retry_counts[task_id]; last_subagent: adversarial |
| Architecture Reviewer | — | last_arch_review_at: now; reset small_done/medium_done if triggered by cadence |
| Drift Detector | — | last_drift_check_at: now |
| End-of-Run Triage | — | status: complete |

(Merge gate is not a subagent — it is applied directly by the orchestrator in Execution Loop step 4 and updates `TASK_QUEUE.md` + `RUN_STATE.md` inline there.)

## Boot

0. Read `docs/RUN_STATE.md` FIRST. If `Paused: yes` and `Human response:` is non-empty:
   a. Append the human response to `docs/DECISION_LOG.md` with timestamp.
   b. Clear `Paused`, `Human question`, and `Human response` in `docs/RUN_STATE.md`.
   c. Set `Status: active`.
   d. Jump to the phase named in `Resume from:` and continue. Skip the rest of Boot.
1. Otherwise, overwrite `docs/RUN_STATE.md` with a fresh schema (run id, start time, `Status: active`, `Current phase: intake`). Add one retry-count entry per task after planning completes.
2. Spawn sub-agent using `agents/intake_validator.md`. Wait for completion.
3. Apply "After Subagent Returns" protocol above.
4. If intake is blocked (status: paused), invoke `orchestration/stop.md` (which prepares a Telegram question).
5. If intake passes, continue.

## Planning

1. Spawn sub-agent using `agents/planner.md`. Wait for completion.
2. Then, spawn sub-agent using `agents/plan_refiner.md`. Wait for completion.
3. Then, spawn sub-agent using `agents/architecture_reviewer.md`. Wait for completion.
4. Confirm no large tasks remain unsplit.
5. Mark the first task active in `docs/TASK_QUEUE.md`.

## Execution Loop

For each task. Apply the "After Subagent Returns" protocol after every "Wait for completion".

1. Spawn sub-agent (Builder) using `agents/builder.md` with `enable_write_tools: true`. Wait for completion.
2. Then, spawn sub-agent (Adversarial Reviewer) using `agents/adversarial_reviewer.md`. Wait for completion.
3. If Adversarial Reviewer needs iteration, return to Builder. If retry count reaches 3, execute the 3-Strike Rule defined in `orchestration/error_handling.md` (dynamic Zoom-Out prompt).
4. **Apply the merge gate checklist yourself (do not spawn a subagent).** Read the most recent Builder and Adversarial reports from `docs/autonomous_run.log`. For each binary item in `orchestration/merge_checklist.md`, mark check / fail / uncertain. Any fail or uncertain = CLOSED.
   - If CLOSED: append a `## Merge Gate - TXXX - YYYY-MM-DD HH:mm\nGate: CLOSED\nClosed reasons: ...` block to `docs/autonomous_run.log`; mark task `needs-fix` in `docs/TASK_QUEUE.md`; increment retry_counts[task_id]; return to Builder.
   - If PASS: append a `## Merge Gate - TXXX - YYYY-MM-DD HH:mm\nGate: PASS\nChecks passed: ...` block to `docs/autonomous_run.log`; mark task `passed` in `docs/TASK_QUEUE.md`; increment small_done or medium_done in `docs/RUN_STATE.md`.
5. **Urgent inbox scan.** Scan `inboxes/arch_issues.md` for any entry with `Severity: urgent`. If any found, spawn sub-agent using `agents/architecture_reviewer.md` out-of-cadence and wait for completion before starting the next task. Apply the "After Subagent Returns" protocol (which will prune any items the reviewer resolves).
6. Continue to next task.

## Periodic Reviews

Spawn sub-agent using `agents/architecture_reviewer.md` (Wait for completion before continuing):

- Every 3 completed medium tasks.
- Every 5 completed small tasks.
- Before declaring a major feature complete.
- When an inbox item is urgent.

Spawn sub-agent using `agents/drift_detector.md` (Wait for completion before continuing):

- Every 3 major changes.
- After architecture-sensitive work.
- Whenever intent drift is suspected.

## Completion

1. Spawn sub-agent using `agents/end_of_run_triage.md`. Wait for completion.
2. Prepare final Telegram approval ping only if required.
3. Mark `docs/RUN_STATE.md` as complete.
