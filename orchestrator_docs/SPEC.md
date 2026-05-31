# SPEC

## Product North Star

This project modifies an existing Android phone keyboard repo, FUTO Keyboard, to fix severe thermal throttling and Wi-Fi/Bluetooth interference caused by the newly introduced text-streaming feature. The text-streaming implementation caused Whisper inference to run in a continuous, unthrottled loop on all CPU cores using an expensive decoding strategy. The core outcome is to throttle and optimize this loop, restore production-level thermals for users with streaming disabled, and make the streaming inference cheap enough to not cause Android's thermal governor to shut down device radios.

## User Intent

The goal is to stop the phone from overheating and burning the user's hands during continuous voice transcription, and to prevent Wi-Fi and Bluetooth drops caused by the thermal stress and audio routing. Instead of GPU offloading (which hit a structural roadblock), the focus is entirely on optimizing the CPU streaming inference loop and restoring correct SCO behavior.

## Done Criteria

- When streaming is disabled, the continuous `runModelLoop` is not started, restoring production-level thermals.
- When streaming is enabled, the loop is throttled/debounced (not running immediately back-to-back).
- Partial decodes use `DecodingMode.Greedy` instead of `BeamSearch5`.
- Partial decodes use a bounded window instead of the entire buffer.
- Native `n_threads` is capped to leave cores idle during partial passes.
- Buffer growth is bounded or capped.
- Wi-Fi and Bluetooth remain stable during dictation.

## Non-Negotiables

- No unrelated UX changes.
- The current keyboard must continue working as it does today.
- Existing voice input behavior must remain intact when streaming is disabled.
- Preserve the keyboard's local/offline privacy posture.
- Preserve the existing IME transaction and voice input architecture invariants documented in ARCHITECTURE.md.

## Out Of Scope

- GPU offloading (OpenCL / Vulkan) is currently out of scope due to structural dependencies.
- No analytics or tracking additions.
- No unrelated refactors or unrelated feature work.
- No keyboard redesign.

## Security And Data Expectations

- No analytics or telemetry.
- No logging of voice input or transcription content.
- Voice processing must remain fully local and offline.
- No external transmission of transcription, microphone, or keyboard input data.
- Existing privacy behavior must remain unchanged.

## Production Readiness Target

Internal tool

## Artifact Expectations

UI and behavior changes should be verified on an Android emulator. The final artifact should include successful Gradle build output, an installable APK, and verification that voice input can stream partial text with significantly reduced CPU usage, while preserving existing behavior when disabled.
