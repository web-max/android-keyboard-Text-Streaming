# Architecture Issues Inbox

Out-of-scope architecture concerns go here.

*Writes: orchestrator only (from Adversarial / Architecture Reviewer routed-issue reports).*
*Prunes: orchestrator removes items mid-run when Architecture Reviewer returns them in `Resolved inbox items:`; End-of-Run Triage prompts final cleanup. Removed items remain in `docs/autonomous_run.log` (`## Inbox Prune` blocks) for history.*

**Urgent severity:** Use `urgent` only when continuing the next task without addressing this would compound damage. The orchestrator scans for `Severity: urgent` after each merge gate and spawns Architecture Reviewer out-of-cadence before starting the next task.

## Template

```markdown
### YYYY-MM-DD HH:mm - Short title

Found by:
Task:
Severity: low | medium | high | urgent
Issue:
Suggested owner:
Suggested timing: now | later | end-of-run
```
