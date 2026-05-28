# SPEC

## Product North Star

This project modifies an existing Android phone keyboard repo, FUTO Keyboard, to enable progressive text streaming during built-in voice transcription. The keyboard already has much of the local voice recognition and partial-result pipeline built, but the user-facing streaming behavior needs to be safely enabled, wired into settings, verified, and packaged into an installable APK. The core outcome is an APK where voice input can stream partial transcription text into the active input field reliably while preserving the keyboard's existing offline-first behavior.

## User Intent

The goal is to make voice input more user-friendly by showing what the voice recognition model is picking up while the user is still speaking. Streaming partial text lets the user catch recognition errors earlier instead of speaking for a long time, waiting for a large block of text, and then having to review and fix a wall of text afterward.

## Done Criteria

- APK builds successfully with no compile errors.
- APK installs and runs on an Android emulator.
- The settings menu exposes a text streaming toggle or equivalent user-facing control for the feature.
- With the streaming behavior enabled, voice input streams partial transcription text into the active input field while speaking.
- With the streaming behavior disabled, existing voice input behavior remains unchanged.
- Final transcription still commits once and does not duplicate, lose, or corrupt text after partial updates.
- Canceling or closing voice input does not leave dangling composing text in the active app.

## Non-Negotiables

- No unrelated UX changes.
- The current keyboard must continue working as it does today.
- Existing voice input behavior must remain intact when streaming is disabled.
- Avoid dependency changes unless they are truly necessary to complete the work.
- Preserve the keyboard's local/offline privacy posture.
- Preserve the existing IME transaction and voice input architecture invariants documented in ARCHITECTURE.md.

## Out Of Scope

- No analytics or tracking additions.
- No unrelated refactors or unrelated feature work.
- No keyboard redesign.
- No Qwen or LLM post-processing integration during this run.
- No experimental neural correction pipeline work during this run.
- No broad dependency modernization.
- No native whisper.cpp or GGML rewrite unless a narrowly scoped bug fix is proven necessary.

## Security And Data Expectations

- No analytics or telemetry.
- No logging of voice input or transcription content.
- Voice processing must remain fully local and offline.
- No external transmission of transcription, microphone, or keyboard input data.
- Existing privacy behavior must remain unchanged.
- Do not add network dependence to the voice transcription flow.

## Production Readiness Target

Internal tool

## Artifact Expectations

UI and behavior changes should be verified on an Android emulator. The final artifact should include successful Gradle build output, an installable APK, emulator evidence that the app installs and is present, and emulator verification that voice input can stream partial text when enabled while preserving existing behavior when disabled. Relevant tests, lint/build checks, and screenshots or a short screen recording should be provided when available.
