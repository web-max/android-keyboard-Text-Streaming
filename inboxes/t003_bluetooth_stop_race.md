## Bluetooth swap race condition with recording stop
Severity: low
Body: If the user toggles the Bluetooth setting and immediately presses stop, `createRecorderAndJob` might respawn a `recorderJob` after `isRecording` was set to false by `onFinishRecording`. This is low severity as it self-heals via VAD or the next recording session, but should be tracked for future cleanup.
