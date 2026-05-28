# Error Handling

## Retry Rule

Each task gets up to 3 Builder iteration attempts after a failed Adversarial Reviewer pass or a closed merge gate.

Track attempts in `docs/RUN_STATE.md`.

## 3-Strike "Zoom Out" Rule

If the same task fails 3 times, do not stop immediately. Give the Builder a fresh perspective for another 3 runs:

1. Spawn the Builder again for attempts 4, 5, and 6.
2. Concatenate this explicit override to the Builder's prompt:
   > "CRITICAL OVERRIDE: The 3-strike rule has been triggered. You are failing. Before you write code, zoom out, look at the bigger picture of the architecture, and rethink your approach."

## 6-Strike Hard Stop Rule

If the same task fails 6 times:

1. Mark task as `blocked`.
2. Append all attempts and evidence to `docs/DECISION_LOG.md`.
3. Invoke `orchestration/stop.md`.
4. Prepare a Telegram CEO ping with options:
   - retry with extra context
   - skip task
   - narrow task
   - escalate to human/manual work

## Intake Failure

If brief, spec, or plan is too vague:

- Pause before implementation.
- Ask precise questions.
- Do not infer missing product intent.

## Codebase Conflict

If requested work conflicts with existing architecture:

- Log the conflict.
- Ask the human to choose between preserving architecture, changing architecture, or narrowing the feature.

## Out-of-Scope Findings

Do not derail the active task. Route issues to:

- `inboxes/arch_issues.md`
- `inboxes/security_issues.md`
- `inboxes/ux_issues.md`
- `inboxes/product_issues.md`

Only stop immediately if the issue is severe enough that continuing would compound harm.
