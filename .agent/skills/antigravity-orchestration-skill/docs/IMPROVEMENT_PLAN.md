# IMPROVEMENT_PLAN

- Inspect the existing voice input partial-result path and confirm the current flow from `WhisperGGML` / `MultiModelRunner` through `AudioRecognizer`, `RecognizerView`, `VoiceInputActionWindow`, `ModelOutputSanitizer`, and `ActionInputTransaction.updatePartial()`.
- Add or expose a voice input text streaming setting using the existing `SettingsKey` and settings page patterns.
- Wire the setting into `VoiceInputActionWindow` / `RecognizerViewSettings` so inline partial transcription is enabled only when intended.
- Preserve final transcription behavior: partial updates must remain provisional composing text, and final output must still commit exactly once through `commit()`.
- Verify disabled behavior: when streaming is disabled, voice input should behave as it did before.
- Verify cancel and close behavior so no dangling composing span is left in the target app.
- Add focused tests where practical around settings, sanitizer-sensitive behavior, or transaction logic.
- Build an installable debug APK with `./gradlew assembleUnstableDebug`.
- Install and verify the APK on an Android emulator so the user can personally test the keyboard.
- Document any emulator/model setup steps needed for repeatable verification.

## Dependencies

- Settings work depends on confirming the existing partial-result and transaction path.
- Toggle-on/toggle-off behavior testing depends on the setting being wired into `RecognizerViewSettings` or the adjacent voice input flow.
- Emulator verification depends on a successful APK build and any required local voice model availability.
- Final acceptance depends on verifying both partial streaming behavior and existing non-streaming behavior.

## Excluded From This Run

- Qwen or LLM post-processing integration.
- Broad voice input redesign.
- Keyboard redesign.
- Analytics, telemetry, or tracking.
- Unrelated keyboard features.
- Broad refactors outside the voice input streaming path.
- Dependency upgrades or additions unless required to unblock the feature.
