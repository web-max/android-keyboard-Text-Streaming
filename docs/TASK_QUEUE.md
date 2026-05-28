# Task Queue

Run ID: 20260528-0024
Source plan: docs/IMPROVEMENT_PLAN.md

## Status Key

- pending
- active
- needs-fix
- review
- passed
- blocked

## Tasks

### T000 - Build APK and document emulator setup

Status: passed
Size: small
Risk: low
Depends on: none
Goal: Build an installable debug APK, verify it on an Android emulator, and document any model/emulator setup steps needed for repeatable verification.
Files likely touched: `README.md` or a new emulator setup markdown document.
Definition of done: A successful `./gradlew assembleUnstableDebug` (or equivalent) build. Documentation of any required emulator/model setup steps.
Verification: APK installs and runs on an Android emulator without crashing.
Artifact: Successful Gradle build output, the installable APK, and the setup documentation.
Notes: Emulator setup and APK build must happen first so that subsequent tasks can use the emulator for verification screenshots.

### T001 - Add text streaming setting

Status: passed
Size: small
Risk: low
Depends on: T000
Goal: Add or expose a voice input text streaming setting using the existing `SettingsKey` and settings page patterns.
Files likely touched: Settings UI files, `SettingsKey` or equivalent, DataStore integration files.
Definition of done: The settings menu exposes a user-facing toggle for text streaming that correctly persists its state.
Verification: Settings app builds and toggle persists across restarts.
Artifact: Screenshot of the affected settings screen.
Notes: Ensure the default state matches the current behavior (disabled). Preserve existing DataStore patterns.

### T002 - Wire setting into VoiceInputActionWindow

Status: passed
Size: small
Risk: low
Depends on: T001
Goal: Wire the new text streaming setting into `VoiceInputActionWindow` / `RecognizerViewSettings`.
Files likely touched: `VoiceInputActionWindow`, `RecognizerViewSettings` or related state files.
Definition of done: `VoiceInputActionWindow` correctly reads the streaming setting when a voice input session starts.
Verification: Log or debug output confirms the setting is read correctly during voice input initialization.
Artifact: Code change only.
Notes: No behavior change yet; just plumbing the setting through the action window state.

### T003 - Enable partial transcription streaming

Status: passed
Size: medium
Risk: high
Depends on: T002
Goal: Modify `VoiceInputActionWindow` to use the setting to conditionally stream partial transcription text via `ActionInputTransaction.updatePartial()`.
Files likely touched: `VoiceInputActionWindow`, usage of `ModelOutputSanitizer`.
Definition of done: When enabled, voice input streams partial text into the active field while speaking. When disabled, behaves as before. Final transcription commits exactly once. Canceling leaves no dangling composing span.
Verification: Manual test on emulator for both enabled and disabled states.
Artifact: Screen recording or screenshot sequence showing partial text streaming, and evidence of normal behavior when disabled.
Notes: Must marshal to `Dispatchers.Main`. Sanitize before every partial update and final commit using the original `textContext`. Do not refresh `textContext` mid-session. Zero or more `updatePartial()` calls followed by exactly one `commit()` or `cancel()`.

### T004 - Add focused tests for streaming logic

Status: needs-fix
Size: small
Risk: low
Depends on: T003
Goal: Add tests for the new settings, sanitizer-sensitive behavior, and transaction logic.
Files likely touched: `tests/src/...`
Definition of done: Focused tests exist to cover the new streaming conditional logic and transaction state machine.
Verification: `./gradlew test` passes.
Artifact: Gradle test execution output log.
Notes: Focus on unit testing the state machine changes and configuration wiring.
