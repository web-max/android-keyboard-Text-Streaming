# [PLACEHOLDER] Telegram Sub-skill

*Note: This file is a placeholder for the Telegram integration sub-skill. When fully built out, this sub-skill will provide the necessary script and instructions to send messages directly to the human user.*

## Intended Usage
The Telegram sub-skill is a narrow, high-priority communication channel. It is used **only** when the run needs a human decision or when critical information must be seen immediately. It is modularly called by the `orchestration/stop.md` protocol.

**Send Telegram For:**
- Intake blocked by missing information.
- Codebase conflict requiring a human decision.
- Task hits the 3-strike rule.
- Drift Detection is below 90% confident about original intent.
- Architecture Reviewer recommends a major refactor or pause.
- `/stop` is triggered.
- End-of-run approval or urgent triage item.

**Do Not Send Telegram For:**
- General logging (use `docs/autonomous_run.log` instead).
- Individual task completion.
- Normal reviewer pass/fail or adversarial iteration.
- Routine changelog updates.

## Message Preparation (Mocked)
If a Telegram adapter is available, send the prepared message through it.
If no adapter is available (current placeholder state), write the prepared message to `docs/DECISION_LOG.md` and pause the orchestrator when a human decision is required.
