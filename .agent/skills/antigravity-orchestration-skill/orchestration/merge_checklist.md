# Merge Gate Checklist

*This checklist is applied by the orchestrator after Adversarial Reviewer passes, not by a separate subagent. The orchestrator reads the most recent Builder and Adversarial reports from `docs/autonomous_run.log` and ticks each box deterministically.*

Every item is binary. If uncertain, the gate is closed.

## Builder Self-Report

- [ ] Files changed and why.
- [ ] Pattern used and where it came from.
- [ ] Scope boundary: what was explicitly not changed.
- [ ] Verification run.
- [ ] Artifact generated if required.
- [ ] Uncertainty or fragile areas reported.

## Orchestrator Pre-Merge Verification

- [ ] Adversarial Reviewer returned SATISFIED (not NEEDS_ITERATION).
- [ ] Builder self-report's `Verification run:` field claims tests, lint, formatting, and required commands passed.

## Adversarial Sign-Off

- [ ] Solution matches user intent.
- [ ] No important edge case ignored.
- [ ] No avoidable overengineering.
- [ ] No hidden behavior change outside task.
- [ ] Non-blocking concerns routed to inboxes.

## Architecture Integrity

- [ ] `docs/ARCHITECTURE.md` updated if boundaries changed.
- [ ] `docs/DECISIONS.md` updated if a significant decision was made.
- [ ] Logic remains in the right layer.

## Artifact And Regression

- [ ] Per-task artifact generated when relevant.
- [ ] Artifact passes visual or functional check.
- [ ] Regression check run against previously accepted behavior.

## Gate Output

Append this to `docs/autonomous_run.log`:

```markdown
## Merge Gate - TXXX - YYYY-MM-DD HH:mm

Gate: PASS | CLOSED
Closed reasons:
- ...
Checks passed:
- ...
Routed issues:
- ...
Next action:
```
