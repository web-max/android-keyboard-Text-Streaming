# Future Upgrades

Do not load this file during a normal run. Use it only when the user asks to upgrade the skill architecture.

## Everything Claude Code Agent Definitions

Adapt stronger role definitions from Everything Claude Code into:

- `agents/builder.md`
- `agents/reviewer.md`
- `agents/adversarial_reviewer.md`
- `agents/architecture_reviewer.md`
- `agents/drift_detector.md`

- tighter agent personas
- richer review checklists
- stricter output schemas
- better failure-mode handling
- better security and architecture instincts

## Error Handling Escalation (Codex / Claude / Cursor)

Upgrade `orchestration/error_handling.md` to automatically escalate to different AI models when tasks fail.
- For example, if a task triggers the 3-Strike rule, spawn a sub-agent using **Claude** or **Codex** for a fresh perspective, or escalate to **Cursor** for deep-dive codebase debugging before doing a hard stop.

## Hermes Telegram Adapter

Adapt Hermes source into `scripts/telegram_adapter_stub.py`.

- real Telegram delivery
- buttons for retry, skip, defer, approve
- run id threading
- persisted human response capture

## Codex Review Gate

Use Codex sparingly as an adversarial review before big features are accepted at the merge gate.

Use only when the feature is expensive to unwind or when Gemini reviewers disagree.

## Claude Architecture Review

Use Claude for periodic architecture review and major refactor recommendations.
