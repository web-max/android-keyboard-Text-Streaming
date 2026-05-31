# Architecture Issues Inbox

Out-of-scope architecture concerns go here.

*Writes: orchestrator only (from Adversarial / Architecture Reviewer routed-issue reports).*
*Prunes: orchestrator removes items mid-run when Architecture Reviewer returns them in `Resolved inbox items:`; End-of-Run Triage prompts final cleanup. Removed items remain in `docs/autonomous_run.log` (`## Inbox Prune` blocks) for history.*

**Urgent severity:** Use `urgent` only when continuing the next task without addressing this would compound damage. The orchestrator scans for `Severity: urgent` after each merge gate and spawns Architecture Reviewer out-of-cadence before starting the next task.

## Template

```markdown
### 2026-05-28 12:00 - Prevent text collapse and session termination during looping inference
Found by: prototype run
Task: T003
Severity: high
Issue: 1) Ensure thread-safe snapshotting of the audio buffer between the recording and inference jobs. 2) Prevent text collapse where new chunks restart token emission. 3) Intercept string returns from intermediate modelRunner.run() calls.
Suggested owner: Builder
Suggested timing: later

### 2026-05-28 12:00 - Bluetooth swap race condition with recording stop
Found by: prototype run
Task: T003
Severity: low
Issue: If the user toggles Bluetooth and immediately presses stop, createRecorderAndJob might respawn recorderJob after isRecording is set to false.
Suggested owner: Builder
Suggested timing: later

### 2026-05-28 12:00 - Limit beforeCursor text length to prevent Whisper prompt token overflow
Found by: prototype run
Task: T003
Severity: medium
Issue: Extracted text context must be truncated to a safe length before passing to native model to prevent token limit assertion errors.
Suggested owner: Builder
Suggested timing: later

### 2026-05-28 12:00 - Prevent splitting UTF-16 surrogate pairs in context prompt
Found by: prototype run
Task: T003
Severity: high
Issue: Truncation logic must respect surrogate pairs to avoid crashes.
Suggested owner: Builder
Suggested timing: later

### 2026-05-28 12:00 - Resolve fake unit test
Found by: prototype run
Task: T005
Severity: high
Issue: ConcurrencyProofTest.kt is fake. Needs proper implementation or removal.
Suggested owner: Builder
Suggested timing: later
```
