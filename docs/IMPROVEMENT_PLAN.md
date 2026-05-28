# IMPROVEMENT_PLAN

**CRITICAL ARCHITECTURAL CONTEXT:** The current `AudioRecognizer` is strictly sequential. It records audio into a buffer (`floatSamples`), waits for the recording to finish, and *then* runs the inference model (`MultiModelRunner`) on the complete buffer. To achieve true live streaming, this architecture must be rewritten to support concurrency.

## Core Objectives
- **Re-architect for Concurrency**: Modify `AudioRecognizer` (and `MultiModelRunner` / JNI bindings if necessary) so that the recording thread can continuously feed chunks of audio (or a sliding window) to a background inference thread *while* the microphone is still recording.
- **Continuous Partial Results**: Ensure the inference model emits `partialResult` callbacks dynamically based on the live-streaming audio chunks, not just rapidly at the end of the recording.
- **Thread Safety**: Implement robust thread synchronization between the recording loop (writing to the audio buffer) and the inference loop (reading from the audio buffer).
- **Preserve Existing UI Wiring**: The `shouldStreamPartialText` toggle and the `ActionInputTransaction` cleanup logic were built in a previous run and are functionally sound. Ensure they are correctly hooked up to the new, truly concurrent partial results.
- **Preserve Final Transcription Behavior**: Final output must still commit exactly once through `commit()` without duplicating the partial text.

## Dependencies
- Concurrency changes depend on fully understanding the thread-safety guarantees of the underlying `WhisperGGML` / native engine.

## Excluded From This Run
- Qwen or LLM post-processing integration.
- Broad voice input redesign.
- Keyboard redesign.
- Analytics, telemetry, or tracking.
- Unrelated keyboard features.
- Dependency upgrades or additions unless required to unblock the feature.
