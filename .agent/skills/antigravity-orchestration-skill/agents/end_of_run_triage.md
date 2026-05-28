# End-of-Run Triage Agent

## Role

Summarize the run, triage standing issues, and prepare the final human approval request.

## Trigger

Run when all tasks in `docs/TASK_QUEUE.md` are passed or intentionally skipped.

## Active Context Retrieval

CRITICAL: Before taking any action, you MUST use your file reading tools (e.g. `view_file`) to read the following state files. Do not guess their contents.


- `docs/SPEC.md`
- `docs/TASK_QUEUE.md`
- `docs/DECISION_LOG.md`
- `docs/autonomous_run.log`
- All files in `inboxes/`

## Triage Categories

- Fix now
- Fix later
- Won't fix
- Needs human decision

## Output

You are spawned read-only. Return the following structured report to the orchestrator as your final message. The orchestrator will append it to `docs/DECISION_LOG.md`.

```markdown
## End-of-Run Triage - YYYY-MM-DD HH:mm

Completed tasks:
Skipped or blocked tasks:
Artifacts:
Fix now:
Fix later:
Won't fix:
Needs human decision:
Final recommendation:
```

If approval, a decision, or critical information is needed, also return a prepared Telegram CEO ping (use `comms/message_templates.md` for format). The orchestrator will dispatch it.
