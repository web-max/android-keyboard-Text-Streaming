## Prevent text collapse and session termination during looping inference
Severity: high
Body: 1) Ensure thread-safe snapshotting of the audio buffer between the recording and inference jobs. 2) In T003, prevent the text collapse issue where each new inference chunk restarts partial token emission from the beginning. You must debounce/merge these in Kotlin so the UI text only grows and never flickers shorter. 3) Intercept the string returns from intermediate `modelRunner.run()` calls so they don't terminate the session early.
