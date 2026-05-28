# /stop Protocol

## Function

`/stop` halts the autonomous run and asks the human for a decision before continuing.

## Who May Invoke

Only senior bigger-picture agents:

- Architecture Reviewer
- Drift Detection Agent
- End-of-Run Triage Agent
- Intake Validator when the run cannot safely start

Builder and Adversarial Reviewer cannot call `/stop`.

## Required Steps

1. Mark `docs/RUN_STATE.md` as `paused`.
2. Append the reason to `docs/DECISION_LOG.md`.
3. Prepare a Telegram CEO ping using `comms/message_templates.md`.
4. Do not continue implementation until the human responds.

## Message Shape

```markdown
Factory pause - [EVENT TYPE]

What happened:
Context:
Decision needed:
Options:
A.
B.
C.
Recommended option:
```
