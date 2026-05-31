# T001: Native Thread Safety Investigation

## Findings

1. **Thread Safety**: `WhisperGGML.infer` is naturally thread-safe for concurrent calls because it is wrapped in `withContext(inferenceContext)`, where `inferenceContext` is a global `newSingleThreadContext("whisper-ggml-inference")`. This guarantees that all calls to `inferNative` are strictly serialized, perfectly satisfying the "one-GGML-inference-at-a-time" rule without needing additional locks.
2. **Concurrent Loops Strategy**:
   - We can implement true streaming by keeping the existing `recordingJob` running on `Dispatchers.Default`, appending audio to the `floatSamples` buffer.
   - We will introduce a concurrent `inferenceJob` (or similar loop) that periodically takes a snapshot of the current `floatSamples` buffer and calls `modelRunner.run()`.
   - Because `modelRunner.run()` suspends on the single-threaded inference context, this loop will naturally back-pressure and pace itself based on how long inference takes (e.g., if inference takes 800ms, the next inference will start immediately after, picking up the latest 800ms of new audio).
   - During the `inferNative` execution, the native `whisper_full` loop will repeatedly fire `invokePartialResult` callbacks back to Kotlin as tokens are decoded, enabling live UI updates.
3. **Cancellation**:
   - When the user stops recording, `modelRunner.cancelAll()` will set `cancel_flag = 1` in the native state. This gracefully and immediately aborts any mid-stream inference loop.
   - `onFinishRecording()` can then trigger a final `runModel()` with the complete audio buffer. The native code automatically resets `cancel_flag = 0` at the beginning of each `inferNative` call, so the final inference runs successfully.
4. **JNI Modifications**:
   - **No JNI modifications are necessary.** 
   - The native C++ state (`WhisperModelState`) holds `wstate->partial_results` (a `std::map`). While it is not explicitly cleared between `inferNative` calls, it is indexed by `whisper_full_n_segments_from_state`, which restarts at 0 for every new inference run. The native code safely overwrites the map entries for the current segment *before* they are read to construct the final string. Stale segments from previous runs are never accessed because the iteration limit `whisper_full_n_segments_from_state` restricts reading to only valid, freshly-overwritten segments.

## Conclusion

We can safely implement the concurrent streaming loops entirely in Kotlin within `AudioRecognizer.kt` and `MultiModelRunner.kt`, without modifying the JNI or native `whisper.cpp` layers.
