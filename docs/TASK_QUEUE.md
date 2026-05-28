# Task Queue

Run ID: 20260528-1054
Source plan: docs/IMPROVEMENT_PLAN.md

## Status Key

- pending
- active
- needs-fix
- review
- passed
- blocked

## Tasks

### T001 - Investigate native thread safety
Status: passed
Size: small
Risk: low
Depends on: none
Goal: Analyze WhisperGGML and native JNI bindings to determine if inference can be run safely on a separate thread while the recording thread collects data.
Files likely touched: `voiceinput-shared/.../WhisperGGML.kt`, `native/jni/*`
Definition of done: Clear strategy documented on how to implement the concurrent loops and whether JNI modifications are necessary.
Verification: Code analysis and document review.
Artifact: Documented findings in a scratch file or DECISION_LOG.md.
Notes: Must respect the one-GGML-inference-at-a-time rule per `WhisperGGML` instance.

### T002 - Implement concurrent recording and inference loops
Status: passed
Size: medium
Risk: medium
Depends on: T001
Goal: Rewrite `AudioRecognizer` to use a thread-safe mechanism (like a shared buffer) where the recording thread writes audio and the background thread feeds it to `MultiModelRunner`.
Files likely touched: `voiceinput-shared/.../AudioRecognizer.kt`, `voiceinput-shared/.../MultiModelRunner.kt`
Definition of done: Audio recording does not block inference. Both loops run safely with proper synchronization.
Verification: `./gradlew lint` and `./gradlew test` pass.
Artifact: Modified `AudioRecognizer` and `MultiModelRunner` files.
Notes: Ensure thread-safety guarantees of the native engine are honored.

### T003 - Implement dynamic partial result emission and fix UI state conflicts
Status: passed
Size: medium
Risk: medium
Depends on: T002
Goal: Modify the inference model execution to emit `partialResult` callbacks continuously based on the incoming chunks. Ensure concurrent inference status updates do not break the `RecognizerView` state machine (e.g., preventing flickering between the recording bubble and the loading circle).
Files likely touched: `voiceinput-shared/.../AudioRecognizer.kt`, `voiceinput-shared/.../MultiModelRunner.kt`, `voiceinput-shared/.../RecognizerView.kt`, `native/jni/*`
Definition of done: Inference engine emits partial transcription strings as speech continues. The recording UI remains stable and does not flicker loading states while streaming.
Verification: `./gradlew lint` and `./gradlew test` pass.
Artifact: Updated code emitting continuous partial results with stable UI.
Notes: Modifying `WhisperGGML` JNI bindings is allowed if strictly necessary. Suppress `InferenceState` updates while actively recording to prevent UI flicker.

### T005 - Verify streaming behavior via Logging and Human-in-the-Loop
Status: passed
Size: small
Risk: low
Depends on: T003
Goal: Ensure the streaming feature works end-to-end without an emulator. First, add programmatic timestamp logging (`Log.d` for mic open, partial result received, and mic close) and run a headless unit test to mathematically prove concurrent execution. Second, build the `assembleUnstableDebug` APK and hand it off to the user for physical device verification.
Files likely touched: `voiceinput-shared/.../AudioRecognizer.kt`
Definition of done: Logcat proves `partialResult` fires between mic open and close. The assembled APK is given to the user and approved.
Verification: Run unit test and grep logcat. Then `assembleUnstableDebug`.
Artifact: The compiled APK saved to the artifacts directory.
Notes: The Orchestrator will pause and wait for the user to manually test the APK and provide the 'approved' human response.
