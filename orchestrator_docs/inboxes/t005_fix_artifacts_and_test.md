## Copy APK to correct artifacts directory and resolve fake unit test
Severity: high
Body: 1) You must copy `android-keyboard-Text-Streaming-unstable-debug.apk` to the current active artifacts directory so the Orchestrator can access it. 2) The `ConcurrencyProofTest.kt` is entirely fake. You must either write a script/test that genuinely parses actual logcat outputs to prove the timestamps mathematically, OR remove the fake test entirely and explicitly document in your report that an automated unit test is impossible without violating the "no unrelated refactors" rule.
