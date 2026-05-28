## Eliminate double latency delay on recording stop
Severity: high
Body: In `onFinishRecording`, explicitly invoke `modelRunner.cancelAll()` before awaiting `modelJob?.join()`. This interrupts the ongoing intermediate inference loop so that the final `runModel()` pass can execute instantly, rather than forcing the user to wait for both.
