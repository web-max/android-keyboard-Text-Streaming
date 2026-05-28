# DECISIONS

Record durable technical decisions that later agents must respect.

### 2026-05-28 - Modify the Existing FUTO Keyboard Codebase

Decision: Build this project by modifying the existing Android keyboard repo rather than creating a new keyboard or separate voice input app.
Rationale: The repo already contains the keyboard, IME integration, local voice input pipeline, settings UI, and APK build system needed for the target feature.
Alternatives considered: Building a separate keyboard or standalone speech-to-text app was ruled out because it would duplicate existing IME, settings, and voice input infrastructure.
Applies to: All implementation planning and future feature work in this run.
Revisit when: Only if the existing repo proves unable to support the feature after direct investigation.

### 2026-05-28 - Preserve Local and Offline Voice Processing

Decision: Voice transcription and streaming must remain fully local and offline.
Rationale: The project goal depends on preserving the existing FUTO Keyboard privacy posture: no spying, no telemetry, no external transmission of microphone or transcription data.
Alternatives considered: External speech APIs, analytics, telemetry, and server-side transcription were ruled out for this run.
Applies to: Voice input, transcription, logging, settings, verification, and any future model-related changes.
Revisit when: Not expected to change for this project.

### 2026-05-28 - Use the Existing Partial-Result Pipeline

Decision: Implement text streaming by enabling and safely wiring the existing partial-result path rather than creating a parallel pipeline.
Rationale: The codebase already has partial-result callbacks through the voice recognition stack and `VoiceInputActionWindow.partialResult()` already sanitizes partial text and calls `inputTransaction.updatePartial()`.
Alternatives considered: A new parallel streaming mechanism was ruled out because it would increase duplication and risk violating IME transaction invariants.
Applies to: Voice streaming implementation, recognizer settings, settings UI, and verification.
Revisit when: Only if the existing pipeline is proven defective and cannot be fixed with scoped changes.

### 2026-05-28 - Preserve the IME Transaction Contract

Decision: Partial transcription must use composing text through `updatePartial()`, and final transcription must use committed text through `commit()`.
Rationale: Android IME behavior depends on keeping provisional text and final text distinct. Swapping these responsibilities can cause text duplication, lost text, or dangling composing spans.
Alternatives considered: Direct `InputConnection` writes and committing partial text were ruled out because they bypass the established action transaction contract.
Applies to: `ActionInputTransaction`, `VoiceInputActionWindow`, cancel/close behavior, and any future streaming code.
Revisit when: Not expected to change.

### 2026-05-28 - Keep Native and JNI Boundaries Fixed for This Run

Decision: Do not rewrite `WhisperGGML`, the JNI callback boundary, GGML execution, or whisper.cpp token loop for this project unless a narrowly scoped bug is proven.
Rationale: The target feature can be implemented at the settings/action/recognizer layer, while the native boundary has strict runtime invariants around callback names, `@Keep`, handle lifetime, cancellation sentinels, and single-inference execution.
Alternatives considered: Native token loop changes were ruled out as unnecessary and high-risk for the current goal.
Applies to: `WhisperGGML`, `MultiModelRunner`, native JNI code, and model inference behavior.
Revisit when: Only if end-to-end investigation proves native output is not emitting usable partial results.

### 2026-05-28 - Treat Debug/Unstable APK as the Primary Build Artifact

Decision: Use `./gradlew assembleUnstableDebug` as the primary build target for this internal-tool run.
Rationale: The user needs an installable APK for emulator and personal testing, and the unstable debug flavor is the practical target before release signing or Play Store packaging.
Alternatives considered: Stable release and Play Store bundle builds remain useful later but may require signing, bundle, or CI-specific setup that is not necessary for first verification.
Applies to: Builder verification, artifact expectations, and initial APK delivery.
Revisit when: The feature is ready for release candidate or production distribution testing.
