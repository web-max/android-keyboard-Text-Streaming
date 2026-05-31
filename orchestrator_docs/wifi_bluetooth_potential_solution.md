# Wi-Fi / Bluetooth Interference — Root Cause and Fix Plan

> Companion to `wifi_bluetooth_issue.md`. Handoff for the builder agent.
> **Revision 2** — updated after new field reports: device is a **Samsung Galaxy S21 Ultra**, and the
> Wi-Fi/Bluetooth *mutual exclusion* ("Wi-Fi drops when BT is on, and vice-versa") was observed
> **immediately after install, before voice input was ever activated, with no noticeable heat**, on a
> freshly rebooted clean device. This revision reconciles that report with what the code can and
> cannot do.

---

## 0. The single most important finding (read this first)

I diffed this branch against the exact commit it was forked from — the production-equivalent base
`4846d5d30` ("Disable Transformer on non QWERTY", = `origin/master`). **Every runtime code change in
this fork is in the voice-input path.** Nothing else changed.

Complete behavioral delta vs the known-good build:

| File | Change |
| --- | --- |
| `voiceinput-shared/.../AudioRecognizer.kt` (+101) | Added the continuous `runModelLoop()` streaming re-decode + `contextPrompt` + `clearCommunicationDevice()` patch |
| `.../uix/actions/VoiceInputAction.kt` (±12) | Wires streaming flag, GPU-accel flag, context prompt |
| `.../uix/VoiceInputSettingKeys.kt` (new) | New settings incl. `STREAM_PARTIAL_TEXT` (default **false**) and `USE_GPU_ACCELERATION` (default **true**) |
| `.../uix/settings/pages/VoiceInput.kt` (+7) | Settings-page UI for the above |
| `.../engine/IMEHelper.kt` (±8) | `class`→`open class`, `fun`→`open fun` only (to allow a test subclass). **Inert at runtime.** |
| `voiceinput-shared/build.gradle` (+1) | `testImplementation 'junit:junit:4.13.2'` — test only |

Everything else is byte-identical to production: `LatinIME.onCreate`, the `Application` class,
`SystemBroadcastReceiver`, the language-model/native code, the manifest, all services and workers.

What this means concretely:

- The app declares **no** `WIFI`/`BLUETOOTH`/`NEARBY`/`CHANGE_NETWORK` permissions and contains **no**
  `WifiManager`, `BluetoothAdapter`, `BluetoothLeScanner`, scan, or radio-toggle calls anywhere. It
  cannot turn a radio on or off through any API.
- `TrainingWorker` (the only wakelock + foreground-service + heavy-ML path) is **entirely commented
  out**; its schedulers are no-op stubs (`xlm/TrainingWorker.kt:338-339`). No training runs.
- `SystemBroadcastReceiver` (the only `MY_PACKAGE_REPLACED`/`BOOT_COMPLETED` receiver) does almost
  nothing — its process logic is commented out (`SystemBroadcastReceiver.java:68-86`).
- There is **no background service, no periodic job, no persistent thread, and no wakelock** that runs
  at install time or while the keyboard is idle.

Therefore: **there is no code in this build that can affect the Wi-Fi/Bluetooth radios except
indirectly, and only while voice input is actively running** (via CPU/thermal load or via the audio
communication-device / Bluetooth SCO path). A purely install-time, pre-voice, software-caused radio
effect is **not possible from this diff**. That tension is the key to diagnosing it (Section 3).

---

## 1. The two real code-side mechanisms (both voice-time)

### Mechanism A — Latched Bluetooth SCO / communication-audio state *(best fit for the persistent, "Wi-Fi vs BT" mutual-exclusion symptom)*

"Wi-Fi turns off when Bluetooth is on and vice-versa" is the textbook signature of a **forced
Bluetooth SCO (HFP) link** held open on the shared 2.4 GHz antenna. While an SCO voice link is up, the
BT radio runs a continuous synchronous-voice channel that starves Wi-Fi.

Where this build opens it:

- `AudioRecognizer.createRecorderAndJob()` → `setCommunicationDevice(preferBluetoothMic)`
  (`AudioRecognizer.kt:169-189, 476`). When a Bluetooth mic is available/preferred it selects
  `TYPE_BLUETOOTH_SCO`, which brings up the SCO/HFP link.
- `VoiceInputAction.recordingStarted()` **auto-sets `PREFER_BLUETOOTH = true` whenever a BT device is
  present** (`VoiceInputAction.kt:281-285`). So a single voice session with any BT audio device
  connected flips the preference on, and every later session opens SCO.

Why it looks "constant" and "before voice" and why **uninstall + reboot** (not just uninstall) fixes
it:

- The SCO/communication-device selection is **OS/audio-HAL state, not app state**. If teardown
  doesn't run (process killed while hot/heavy, crash, OOM, or an exit path that misses
  `clearCommunicationDevice()`), the link can stay **latched at the platform level** after the
  keyboard window is gone — so the radios stay degraded even when the user isn't dictating.
- A full **reboot** resets the Bluetooth/audio stack and clears the latch — which is exactly why the
  reported fix is *uninstall **and reboot***, not uninstall alone.
- The earlier `clearCommunicationDevice()` patch made it "less aggressive" because it clears SCO on the
  **normal** finish/reset paths — but not on crash/kill paths, and the streaming loop (Mechanism B)
  makes the app heavy and long-running, so abnormal termination is more likely.

Caveat: SCO can only latch if voice input ran **at least once** with a BT device available. If the
user is certain voice never ran at all, this mechanism is bypassed and the cause is environmental
(Section 3, hypothesis C).

### Mechanism B — Thermal 2.4 GHz coexistence collapse from the streaming loop *(fits the dictation-time drops + overheating)*

The text-streaming commit `d2e39cbe4` replaced the original **single** end-of-utterance Whisper pass
with a **continuous re-transcription loop**:

```kotlin
// AudioRecognizer.startRecording()  (~line 540) — launched UNCONDITIONALLY
modelJob = lifecycleScope.launch { withContext(Dispatchers.Default) { runModelLoop() } }

// runModelLoop()  (~line 621-647)
while (isRecording) {
    val floatArray = getAudioSnapshot()   // ENTIRE growing buffer, from sample 0
    if (floatArray.size > 16000 * 0.5) {
        modelRunner.run(floatArray, ...)   // full Whisper decode, BeamSearch5, every CPU core
    } else { delay(100) }
    yield()                                 // no real throttle -> immediately re-decodes
}
```

Cost drivers:
- **Continuous, not one-shot.** Production ran inference once in `onFinishRecording()`. This re-runs it
  back-to-back for the whole utterance.
- **Re-decodes the whole buffer each pass** (`getAudioSnapshot()` returns sample 0→position,
  `:317`) → ~O(n²) total work.
- **Beam search 5 every pass** (`MultiModelRunner.kt:84,102`) — ~5× greedy, spent on throwaway
  partials.
- **All CPU cores** (`WhisperGGML.cpp:90-99`, `n_threads = num_procs`).
- **Encoder grows with utterance** (`audio_ctx` scales with samples, `WhisperGGML.cpp:101`).
- **Runs even when streaming is disabled.** `STREAM_PARTIAL_TEXT` defaults **false**, but the loop is
  launched regardless; the flag only gates whether partials are *shown*
  (`VoiceInputAction.kt:299-305`). So every dictation pays the full cost and just discards the output.

Pegging all cores flat-out heats the SoC; Android's thermal governor then throttles and
de-prioritizes/powers down the shared 2.4 GHz Wi-Fi/BT radio, and the radio stays degraded through the
thermal-recovery tail. The new **`USE_GPU_ACCELERATION` default = true** compounds this by also
exercising the GPU during inference — a path production never enabled.

---

## 2. How the mechanisms map to each reported symptom

| Report | Most likely cause |
| --- | --- |
| "Wi-Fi off when BT on, and vice-versa" (mutual exclusion) | **A** — forced/latched Bluetooth SCO on the 2.4 GHz antenna. |
| Persists when **not** transcribing | **A** — latched OS/HAL audio-routing state outlives the keyboard window. |
| Overheating, drops while dictating | **B** — continuous all-core beam-search re-decode (+ GPU default). |
| "Less aggressive" after `clearCommunicationDevice()` patch | **A** — patch clears SCO on normal exit, not on crash/kill paths. |
| **Uninstall *and reboot*** required to fully fix | **A** — reboot resets the BT/audio HAL latch. |
| Production build unaffected | Only voice code changed; production has neither the loop, the GPU default, nor the auto-`PREFER_BLUETOOTH` + streaming-driven long sessions. |
| "Immediately on install, before any voice, no heat" | Cannot be produced by this diff (Section 0). Either voice actually ran once (→ A), or it is environmental (Section 3-C). **Must be confirmed on-device.** |

---

## 3. Diagnose on the device before committing to a fix

The symptom description and the code do not fully agree on *when* it triggers, and this cannot be
resolved by reading code — it must be measured on the actual S21 Ultra. Run this decision tree:

**Hypothesis A — latched Bluetooth SCO**
- With the app installed and the radios misbehaving (but **not** dictating), dump audio state:
  - `adb shell dumpsys audio | grep -iE "mode|communication|sco|bluetooth"`
  - Look for `mode=MODE_IN_COMMUNICATION`, a forced communication device of type `BLUETOOTH_SCO`, or
    `bluetoothSco ON`. `adb shell dumpsys bluetooth_manager | grep -iE "sco|hfp|headset"`.
  - If SCO/HFP shows active while idle → **Hypothesis A confirmed.**
- Quick toggles: turn Bluetooth fully off → does Wi-Fi recover? Disconnect/unpair the headset → does it
  recover? In keyboard Settings, set **Prefer Bluetooth = off**, then re-test.

**Hypothesis B — thermal**
- During a ~30-60 s dictation: `adb shell top -m 5 -d 1` (expect the IME process near 100% × cores),
  `adb shell dumpsys thermalservice` (watch temps/throttle climb). Correlate the Wi-Fi drop timing
  with throttle onset.

**Hypothesis C — environmental / device (no voice involved)**
- If voice input genuinely never ran since the clean install, A and B are impossible and the cause is
  outside this code: S21 Ultra 2.4 GHz Wi-Fi/BT coexistence, a specific paired BT device forcing HFP,
  or the router's 2.4 GHz channel. Test: same APK on a different phone; toggle the router to 5 GHz;
  remove the BT device. If the mutual-exclusion persists with the keyboard never invoked and survives
  these changes, it is not this app's software.

Capture which hypothesis reproduces — it determines which fixes below actually matter.

---

## 4. Fix plan (prioritized)

### P0 — Make Bluetooth SCO teardown bulletproof (addresses Mechanism A)
1. Call `clearCommunicationDevice()` (and abandon audio focus) on **every** exit path, including
   abnormal ones: `close()`, `cancel()`, `reset()`, `onDestroy`/window-dismiss, and an
   `onTaskRemoved`/`finally` safety net. Audit `VoiceInputAction.close()` (`:254-260`) and
   `AudioRecognizer.reset()` (`:221-236`) to guarantee it runs even if inference is mid-flight or the
   coroutine scope is cancelled.
2. Add a process-start safety clear: on keyboard `onCreate` (or first voice-window create), call
   `clearCommunicationDevice()` once to release any latch left by a previously killed session.
3. Reconsider the **auto-enable** of `PREFER_BLUETOOTH` in `recordingStarted()`
   (`VoiceInputAction.kt:281-285`) — silently turning on Bluetooth mic for every user with any BT
   device connected is what pulls them into the SCO path. Make it opt-in, or only select SCO when the
   user explicitly chose a BT mic.
4. For the built-in mic, prefer **not** forcing the communication device / SCO at all, so BT media and
   Wi-Fi keep the 2.4 GHz radio.

### P1 — Stop the continuous loop when streaming is off (addresses Mechanism B, biggest safe win)
- `STREAM_PARTIAL_TEXT` defaults **false**, yet `runModelLoop()` runs anyway. Thread the streaming flag
  into `AudioRecognizer` and **only launch `modelJob = runModelLoop()` when streaming is enabled**;
  otherwise keep the production single pass in `onFinishRecording()` → `runModel()`. This removes the
  thermal regression for the default configuration immediately.

### P2 — Make streaming itself cheap (for users who enable it)
1. **Throttle/debounce**: only re-decode after enough *new* audio (e.g. ≥1–2 s) or on VAD segment
   boundaries; add a real inter-pass delay.
2. **Bounded window, not whole buffer**: decode only the unfinalized tail / a sliding window for
   partials; full-buffer pass only for the final result. Kills the O(n²) blow-up.
3. **Greedy partials, beam final**: pass `DecodingMode.Greedy` for in-progress passes, keep
   `BeamSearch5` only for the final `runModel()` (native already accepts `decoding_mode`,
   `WhisperGGML.cpp:60,105-112`).
4. **Cap threads**: `n_threads = min(4, num_procs/2)` (`WhisperGGML.cpp:90-99`) to leave thermal
   headroom; consider an even lower cap for partial passes.

### P3 — Reconsider the new `USE_GPU_ACCELERATION` default
- It is a **new** setting defaulting **true**, diverging from production. Flip the default to **false**
  (or gate by device allow-list) until validated on the S21 Ultra; offer it as an opt-in. Quick test:
  toggle it off and re-measure thermals/stability.

### P4 — Bound buffer growth
- Cap `floatSamples` expansion / force finalize after a max utterance length
  (`AudioRecognizer.kt:303-315`) so a left-open session can't record/decode unbounded.

### Housekeeping (not a fix, but do it)
- Remove the accidentally-committed binaries and scratch files from the branch: `jdk.tar.gz` (~185 MB),
  the entire `jdk-17.0.14+7/` tree, `patch.kt`, `patch_audio_recognizer.sh`, `test.log`. They bloat the
  repo and obscure the real diff.

---

## 5. Validation (after fixes)
- **Mechanism A**: after a dictation that uses a BT mic, end/cancel/kill the keyboard, then
  `adb shell dumpsys audio | grep -i sco` — communication device/SCO must be cleared in **all** cases.
  Wi-Fi must stay up with BT on.
- **Mechanism B**: IME CPU during dictation well below all-core saturation; `dumpsys thermalservice`
  out of throttling for a normal utterance; Wi-Fi RSSI / ping / BT media stable *during* dictation.
- **Functional**: streaming ON still shows live partials; streaming OFF still commits the final
  transcript (single pass). Confirm `modelJob` is cancelled and `isRecording=false` on every teardown
  path so no decode loop survives the window closing.

---

## 6. Key code references
- `voiceinput-shared/.../AudioRecognizer.kt` — `runModelLoop()` `:621-647`; launch `:540`;
  `getAudioSnapshot()` `:317`; SCO/comm device + focus `:122-196, 476`; `onFinishRecording()` +
  clear patch `:649-670`; `reset()` `:221-236`.
- `.../whisper/MultiModelRunner.kt` — always BeamSearch5 `:84,102`.
- `native/jni/org_futo_voiceinput_WhisperGGML.cpp` — `n_threads=num_procs` `:90-99`; `audio_ctx`
  grows `:101`; decoding-mode wiring `:105-112`.
- `.../uix/actions/VoiceInputAction.kt` — streaming flag gates UI only `:135,148,299-305`;
  auto-enables `PREFER_BLUETOOTH` `:281-285`; `close()` `:254-260`.
- `.../uix/VoiceInputSettingKeys.kt` — `STREAM_PARTIAL_TEXT` default false; `USE_GPU_ACCELERATION`
  default true.
- Regression commit `d2e39cbe4` ("SUCCESS. Text streaming works!"). Production base `4846d5d30`
  (`origin/master`). Full delta confirmed voice-only.
