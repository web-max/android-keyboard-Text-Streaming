# Wi-Fi / Bluetooth Interference — Root Cause and Fix Plan

> Companion to `wifi_bluetooth_issue.md`. This doc identifies the underlying cause and gives a
> step-by-step remediation plan for the builder agent.

## TL;DR

The text-streaming feature replaced a **single** end-of-utterance Whisper pass with a
**continuous re-transcription loop** that re-decodes the *entire growing audio buffer* with
**beam search 5** on **every CPU core**, back-to-back, for the whole duration of dictation. This
pegs the SoC at 100% on all cores and overheats the phone. Android's thermal governor responds by
throttling and powering down / de-prioritizing the shared 2.4 GHz Wi-Fi/Bluetooth radio. The
Bluetooth SCO + audio-focus path (already partially patched) is a secondary amplifier, not the
primary cause.

This is a **CPU/thermal regression**, not a network API bug.

## 1. Root Cause (Primary)

The regression was introduced in commit `d2e39cbe4` ("SUCCESS. Text streaming works!"). It added
`runModelLoop()` and wired it into `startRecording()`:

`voiceinput-shared/.../AudioRecognizer.kt`

```kotlin
// startRecording()  (~line 540)
modelJob = lifecycleScope.launch {
    withContext(Dispatchers.Default) {
        runModelLoop()          // <-- NEW: continuous inference
    }
}
```

```kotlin
// runModelLoop()  (~line 621-647)
while (isRecording) {
    val floatArray = getAudioSnapshot()        // ENTIRE buffer so far, from sample 0
    if (floatArray.size > 16000 * 0.5) {
        modelRunner.run(floatArray, ...)        // full Whisper decode, BeamSearch5
    } else {
        kotlinx.coroutines.delay(100)
    }
    yield()                                      // no throttle -> immediately re-decodes
}
```

### Why this is catastrophic

1. **Continuous, not one-shot.** Production (`commit 862411697^`) launched only `preloadModels()`
   in `startRecording()` and ran inference exactly **once** in `onFinishRecording()` →
   `runModel()`. The streaming branch runs inference **over and over** for the entire recording.

2. **Re-decodes the whole buffer every pass.** `getAudioSnapshot()`
   (`AudioRecognizer.kt:317`) returns the full buffer from sample 0 to the current position. Pass
   *N* re-transcribes everything pass *N-1* already did, so total work is roughly **O(n²)** in
   utterance length.

3. **Beam search 5, every pass.** `MultiModelRunner.run()` always uses
   `DecodingMode.BeamSearch5` (`MultiModelRunner.kt:84,102`). Beam-5 is ~5× the work of greedy and
   is being spent on *throwaway partial* results.

4. **All CPU cores.** Native sets `wparams.n_threads = (int)num_procs`
   (`native/jni/org_futo_voiceinput_WhisperGGML.cpp:90-99`, capped at 16). No thermal headroom is
   left for the rest of the system.

5. **Encoder cost grows as you talk.** `wparams.audio_ctx` scales with `num_samples`
   (`WhisperGGML.cpp:101`) up to 1500, so each successive pass also runs a larger encoder.

6. **Runs even when streaming is OFF.** `modelJob = runModelLoop()` is launched
   **unconditionally** in `startRecording()`. The `STREAM_PARTIAL_TEXT` setting only gates whether
   partials are *shown* (`VoiceInputAction.kt:299-305`, `partialResult` early-returns). So the
   expensive loop runs on **every dictation** regardless of the feature flag — its output is simply
   discarded when streaming is disabled. Users who never enabled streaming still pay the full
   thermal cost.

Net effect: from mic-open to mic-close, all cores run flat-out doing redundant beam-search decodes.
On a phone this is sustained max load → rapid heating → thermal mitigation → radio shutdown.

## 2. How This Explains Every Symptom

| Symptom (from `wifi_bluetooth_issue.md`) | Explanation |
| --- | --- |
| Overheating correlated with dev build | All-core, back-to-back, O(n²) beam-5 inference is the heat source. |
| Wi-Fi/BT drop | Android thermal governor throttles and powers down the shared 2.4 GHz radio under thermal stress. |
| "Constant, even when not transcribing" | (a) Thermal-recovery tail: the SoC stays hot and radios stay throttled for a while *after* compute stops; repeated dictations never let it cool. (b) The loop runs even when the streaming setting is off, so it fires on every dictation, not just when the user thinks streaming is active. |
| "Less aggressive" after `clearCommunicationDevice()` patch | Closing Bluetooth SCO removed the 2.4 GHz BT-vs-Wi-Fi contention amplifier, but left the primary thermal driver (the loop) untouched — exactly the partial improvement reported. |
| Uninstall + reboot fully resolves | Removes the heat source entirely; SoC cools and the thermal governor restores the radio. Reboot clears any latched thermal/radio state. |
| Production build unaffected | Production has no `runModelLoop()` — single pass at end of utterance only. |

## 3. Secondary Contributors (amplifiers, fix after the primary)

- **Bluetooth SCO + audio focus held for the entire (now very long) recording window.**
  `setCommunicationDevice()` may select `TYPE_BLUETOOTH_SCO` (`AudioRecognizer.kt:169-189`) and
  `focusAudio()` requests exclusive focus. The `clearCommunicationDevice()` patch (now in
  `onFinishRecording`, `reset`, error path) is correct, but the long recording duration is what
  made it painful. Shortening/avoiding the high-priority audio window helps Bluetooth media + Wi-Fi.

- **Unbounded buffer growth.** `canExpandSpace` lets `floatSamples` grow by 30 s chunks
  indefinitely (`AudioRecognizer.kt:303-315`). With VAD auto-stop disabled, a session left open
  records — and re-decodes — without bound.

## 4. Fix Plan (step by step)

### Step 0 — Reproduce & baseline (before changing code)
- Install the current dev build. Start dictation and hold it for ~30-60 s.
- Capture, during dictation:
  - `adb shell top -m 5 -d 1` (expect the IME process near 100% × core count)
  - `adb shell dumpsys thermalservice` (watch temperature / throttling status climb)
  - Wi-Fi health: `adb shell dumpsys wifi | grep -i rssi` or a continuous ping/iperf.
- Record time-to-throttle. This is the regression signature to beat.

### Step 1 — Stop the continuous loop when streaming is not in use (biggest, safest win)
Restore production behavior whenever partial streaming is disabled.

- Plumb the streaming flag into `AudioRecognizer` (it already exists as `STREAM_PARTIAL_TEXT` /
  `shouldStreamPartialText` in `VoiceInputAction.kt`; thread it through `RecognizerViewSettings` →
  `AudioRecognizerSettings.recordingConfiguration` or `modelRunConfiguration`).
- In `startRecording()` (`AudioRecognizer.kt:540`), launch `modelJob = runModelLoop()` **only if
  streaming is enabled**. Otherwise launch nothing here and rely on the existing single
  `runModel()` in `onFinishRecording()` (which already runs on stop).
- Verify `onFinishRecording()` still performs the final full-quality pass in both modes.

Expected result: users with streaming off get production-level thermals immediately.

### Step 2 — Make streaming itself cheap (for users who do enable it)
Apply all of these:

1. **Throttle / debounce the loop.** Only re-decode when there is enough *new* audio (e.g. ≥ 1.0-2.0 s
   added since last pass) or on VAD speech-segment boundaries. Add a real delay between passes
   instead of immediate re-entry. Target: a handful of partial decodes per utterance, not dozens.
2. **Decode a bounded window, not the whole buffer.** Replace `getAudioSnapshot()` (full buffer)
   for *partial* passes with a sliding window (e.g. last N seconds), or only the unfinalized tail.
   This removes the O(n²) blow-up. Keep the full-buffer pass for the *final* result only.
3. **Use greedy for partials, beam for final.** Pass `DecodingMode.Greedy` (best_of 1) for
   in-progress partial decodes; keep `BeamSearch5` only for the final `runModel()` pass. Requires a
   `decodingMode` parameter on `MultiModelRunner.run()` / `WhisperGGML.infer()` (the native side
   already accepts `decoding_mode`, `WhisperGGML.cpp:60,105-112`).
4. **Cap CPU threads for thermal headroom.** In `WhisperGGML.cpp:90-99`, cap `n_threads` to leave
   cores idle, e.g. `std::min(4L, num_procs / 2)` (tune on device). Sustained all-core load is the
   single biggest heat driver. Consider a lower cap specifically for partial (streaming) passes.

### Step 3 — Bound the radio-contention window (secondary)
- Keep the existing `clearCommunicationDevice()` patch.
- Only call `setCommunicationDevice(... BLUETOOTH_SCO)` when a Bluetooth mic is actually the chosen
  input; for the built-in mic, avoid opening the SCO/communication path at all so Bluetooth media
  audio and Wi-Fi keep the 2.4 GHz radio.
- Consider gating exclusive `focusAudio()` to only while actively recording (already roughly the
  case) and confirm `unfocusAudio()` runs on every exit path.

### Step 4 — Bound buffer growth (secondary, robustness)
- Enforce a hard cap on `floatSamples` expansion, or force a finalize after a max utterance length,
  so a left-open session can't record/decode unbounded.

## 5. Validation (after the fix)
Re-run the Step 0 measurements and compare:
- IME CPU during dictation should be well below all-core saturation (target: a few cores, with
  gaps between partial decodes — not a flat 100% wall).
- `dumpsys thermalservice` should stay out of throttling for a normal-length dictation.
- Wi-Fi RSSI / ping latency / Bluetooth media audio should remain stable *during* dictation, not
  just after.
- Functional check: streaming ON still shows live partials; streaming OFF still commits the final
  transcript correctly (single pass at stop).
- Regression check: confirm `modelJob` is cancelled and `isRecording=false` on **every** teardown
  path — `close()`, `cancel()`, `reset()`, and normal `finish()` — so no decode loop survives the
  window closing.

## 6. Key Code References
- `voiceinput-shared/src/main/java/org/futo/voiceinput/shared/AudioRecognizer.kt`
  - `startRecording()` launches the loop: ~`:540`
  - `runModelLoop()` (the regression): ~`:621-647`
  - `runModel()` (correct single final pass): ~`:583-619`
  - `getAudioSnapshot()` returns whole buffer: `:317`
  - SCO/comm device + focus: `:122-196`, `:476`
  - `onFinishRecording()` + clear patch: `:649-670`
- `voiceinput-shared/src/main/java/org/futo/voiceinput/shared/whisper/MultiModelRunner.kt`
  - Always BeamSearch5: `:84`, `:102`
- `native/jni/org_futo_voiceinput_WhisperGGML.cpp`
  - `n_threads = num_procs`: `:90-99`
  - `audio_ctx` grows with samples: `:101`
  - decoding mode wiring: `:105-112`
- `java/src/org/futo/inputmethod/latin/uix/actions/VoiceInputAction.kt`
  - streaming flag only gates UI, not the loop: `:135,148,299-305`
- Regression commit: `d2e39cbe4` ("SUCCESS. Text streaming works!"). Pre-regression baseline:
  `862411697^`.
