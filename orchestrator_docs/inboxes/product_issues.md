# Product Issues Inbox

Out-of-scope product, scope, and user-intent concerns go here.

*Writes: orchestrator only (from subagent routed-issue reports).*
*Prunes: orchestrator removes items mid-run from Architecture Reviewer's `Resolved inbox items:` list; End-of-Run Triage prompts final cleanup. Removed items remain in `docs/autonomous_run.log` for history.*

### 2026-05-28 12:00 - Emulator boot failure may block subsequent tasks
Found by: prototype run
Task: T000
Severity: medium
Issue: Headless emulator hung during boot. EMULATOR_SETUP.md needs refinement for CI environments.
Suggested owner: Builder
Suggested timing: later

### 2026-05-28 12:00 - Eliminate double latency delay on recording stop
Found by: prototype run
Task: T003
Severity: high
Issue: modelRunner.cancelAll() must be invoked before awaiting modelJob join to prevent double latency when stopping.
Suggested owner: Builder
Suggested timing: later

### 2026-05-28 12:00 - Suppress continuous LoadingModel status emissions and handle Bluetooth toggle race condition
Found by: prototype run
Task: T003
Severity: medium
Issue: 1) Suppress intermediate status updates while recording. 2) Ensure inference loop survives Bluetooth toggles.
Suggested owner: Builder
Suggested timing: later
