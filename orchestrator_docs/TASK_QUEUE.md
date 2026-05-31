# Task Queue

Run ID: thermal-fix-run-01
Source plan: docs/IMPROVEMENT_PLAN.md

## Status Key

- pending
- active
- needs-fix
- review
- passed
- blocked
- skipped

## Tasks

- [active] T001: Implement thermal regression fixes across Kotlin and JNI.
  - Plumb streaming flag into AudioRecognizer so runModelLoop only runs when streaming is enabled.
  - Throttle loop with `kotlinx.coroutines.delay` or by tracking added sample size.
  - Expose `decodingMode` to JNI (`WhisperGGML.inferNative`) and Kotlin `infer()` so partials can use `Greedy` and final can use `BeamSearch5`.
  - Cap `n_threads` in JNI to leave cores idle during partials.
  - Ensure sliding window or bounded input for partials to avoid O(n^2).
  - Conditionally call `setCommunicationDevice(... BLUETOOTH_SCO)` only for Bluetooth mics.
  - Build the UnstableDebug APK.
