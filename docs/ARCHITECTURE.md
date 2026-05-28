# ARCHITECTURE

## Current System Shape

This is an Android IME based on FUTO Keyboard, a fork of LatinIME with significant Kotlin, Java, Compose, native, and voice input additions. The main app module owns keyboard UI, settings, actions, input transactions, resources, and APK packaging. The `voiceinput-shared` module owns local voice recognition UI/state, model management, audio recording, Whisper/GGML bridging, and partial-result callbacks. The native layer under `native/jni` supports inference and dictionary/runtime code. For this project, the key flow is: local voice inference emits partial results, `RecognizerView` forwards them to `VoiceInputActionWindow`, `ModelOutputSanitizer` cleans them using the session's cursor context, and `ActionInputTransaction.updatePartial()` writes provisional composing text into the active Android input field before one final `commit()`.

The streaming architecture requires concurrent processing. The work should focus on re-architecting `AudioRecognizer` and `MultiModelRunner` to feed audio to the inference model while recording, enabling true live streaming.

## Important Directories

- `java/src` - main Android app Kotlin/Java code for keyboard behavior, UI actions, settings, input handling, and app integration.
- `common/src` - shared LatinIME/common keyboard logic.
- `voiceinput-shared/src/main/java` - shared local voice input code, including recognizer UI, model management, callbacks, audio recognition, and Whisper/GGML wrappers.
- `voiceinput-shared/src/main/ml` - voice input model assets or ML-related module assets.
- `java/res` - main Android resources, strings, layouts, drawables, and XML resources.
- `java/res-large` - large-screen or alternate Android resources.
- `java/res-bundle` - bundled resource support used by playstore/release packaging.
- `java/assets` - keyboard assets including layouts, spacing/punctuation data, fonts, and themes.
- `java/stable` - stable flavor manifest/source overrides.
- `java/unstable` - unstable flavor resources and overrides.
- `java/playstore` - playstore flavor manifest/source/resource overrides.
- `native/jni` - native C/C++ JNI code, GGML/Whisper integration, dictionary, suggestion, and runtime support.
- `native/dicttoolkit` - native dictionary tooling and tests.
- `tests/src` - Android/instrumentation test sources.
- `tests/res` - test resources.
- `tools` - supporting scripts and tooling such as dictionary and locale generation.
- `translations` - generated and maintained translation resources consumed by Gradle tasks.
- `.agent/skills/antigravity-orchestration-skill/docs` - Antigravity project configuration files for autonomous runs.

## Patterns To Preserve

- One `ActionInputTransaction` per voice session, created when the voice action window opens and discarded when it closes.
- `updatePartial()` is for provisional composing text; `commit()` is for final committed text. Never swap these responsibilities.
- The transaction call order is zero or more `updatePartial()` calls followed by exactly one `commit()` or `cancel()`. Never both and never twice.
- `textContext` is a snapshot taken at window-open time and must not be refreshed mid-session.
- `ModelOutputSanitizer.sanitize(text, textContext)` must run before every `updatePartial()` and every `commit()`.
- Native/inference callbacks arrive off the main thread; all UI and input transaction work must marshal to `Dispatchers.Main`.
- Preserve the `@Keep` annotation and JNI callback shape for `WhisperGGML.invokePartialResult()`.
- Preserve `RecognizerView` state ordering: loading, permission if needed, recording started, zero or more partial results, then finished or cancelled.
- Keep the native runner callback alive for the full duration of `modelRunner.run()`.
- Preserve one GGML inference at a time per `WhisperGGML` instance.
- Use existing `SettingsKey` / DataStore-style settings patterns for new voice input settings.
- Use existing Compose settings page patterns for user-facing toggles.
- Preserve local/offline voice processing and privacy behavior.

## Patterns To Avoid

- Do not modify the `ActionInputTransaction` interface or central IME transaction contract.
- Do not modify the `KeyboardManagerForAction` public API unless absolutely necessary and explicitly justified.
- Do not bypass `ActionInputTransaction` by writing directly to `InputConnection` from voice input code.
- Do not bypass `ModelOutputSanitizer` before partial or final transcription output.
- Do not perform input transaction or UI work directly on GGML/native callback threads.
- Do not change `RichInputConnection` or OS-level input plumbing for this feature.
- Modifying `AudioRecognizer` threading and the `WhisperGGML` JNI boundary IS permitted for this run to support concurrent streaming.
- Do not use committed text for partial results.
- Do not introduce network calls, analytics, telemetry, or external transcription services.
- Do not make broad keyboard UI or architecture refactors unrelated to voice streaming.

## Integration Points

- Android InputConnection - OS-level destination for composing and committed text in the active app.
- RichInputConnection - keyboard-level input connection wrapper and cursor context cache; treat as fixed for this run.
- ActionInputTransaction - action-level transaction contract for partial updates, final commit, and cancel behavior.
- KeyboardManagerForAction - public API used by action windows, including voice input, to interact with the keyboard.
- VoiceInputActionWindow - app-level voice input action window that receives recognizer callbacks, sanitizes text, updates partial composing text, and commits final text.
- RecognizerView - shared voice recognition UI/state machine that emits partial and final callbacks.
- AudioRecognizer - audio recording and inference orchestration layer; preserve its threading model.
- WhisperGGML / MultiModelRunner - local Whisper/GGML model execution and JNI bridge.
- ModelOutputSanitizer - context-aware formatting and cleanup before text reaches the input transaction.
- SettingsKey / DataStore settings - persisted user settings for voice input behavior.
- Android emulator - primary verification environment for APK install, settings UI, and streaming behavior.

## Known Architecture Risks

- Partial transcription already exists in several layers, so duplicating the pipeline could cause conflicting updates, text duplication, or broken final commits.
- `ActionInputTransaction` is a strict state machine; incorrect ordering can duplicate, lose, or corrupt text.
- `textContext` is a window-open snapshot; refreshing it mid-session can break sanitizer behavior and final formatting.
- Native/inference callbacks arrive off the main thread; touching UI or input transactions without `Dispatchers.Main` can cause crashes or race conditions.
- JNI callback names and `@Keep` annotations are runtime-critical; R8/ProGuard changes can break native callbacks silently.
- Cancel behavior must not leave dangling composing spans in the target app.
- Emulator verification may require local voice model setup before end-to-end streaming can be tested.
- Release/stable builds may require signing or bundle-specific setup; debug/unstable APK builds are the practical initial target.
