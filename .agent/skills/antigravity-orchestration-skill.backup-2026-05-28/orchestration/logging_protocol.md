# Logging Protocol

## File Ownership (Single-Writer Rule)

The orchestrator is the sole writer of every state file. Subagents are spawned read-only by default (Antigravity `enable_write_tools = false`); Builder is the only exception because it has to edit code. All other subagents return structured reports as their final message — the orchestrator persists those reports.

| File | Sole writer |
|---|---|
| `docs/RUN_STATE.md` | Orchestrator only |
| `docs/TASK_QUEUE.md` | Orchestrator only (creates and mutates based on Planner / Plan Refiner returned text) |
| `docs/autonomous_run.log` | Orchestrator only (appends subagent returned reports + phase-transition markers) |
| `docs/DECISION_LOG.md` | Orchestrator only (appends senior subagent returned decisions) |
| `inboxes/*.md` | Orchestrator only (writes routed issues; prunes resolved items from Architecture Reviewer's returned list) |
| Code files | Builder only |

## Telegram Is Not A Log

Telegram is a CEO-ping channel. Use it only for:

- Required human decisions.
- Critical information the human should know immediately.
- Run-ending or run-pausing events.

## DECISION_LOG.md

Use for durable decisions and rationale:

- Intake pass/block decisions.
- Plan revisions.
- Architecture decisions.
- Drift assessments.
- 3-strike summaries.
- Human responses.

## autonomous_run.log

Use for chronological run events:

- Task start and finish.
- Builder reports.
- Adversarial review reports.
- Verification output.
- Merge gate status.

## RUN_STATE.md

Use for current state:

- run id
- current task
- retry counts
- paused/active/complete status
- next action

## Inboxes

Use for out-of-scope issues that should not block the current task.
