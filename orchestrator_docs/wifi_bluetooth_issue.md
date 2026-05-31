# Wi-Fi and Bluetooth Interference Issue

## 1. Problem Description
Users of the current development build of the keyboard app are experiencing severe interference with their device's Wi-Fi and Bluetooth radios. The connection drops out or becomes highly unstable. 

**Key Symptoms:**
* The interference happens **all the time**, constantly running in the background, not just when the user is actively transcribing text with the microphone.
* As soon as the app is uninstalled (and the phone is rebooted), normal Wi-Fi and Bluetooth behavior resumes.
* Recently, the problem was described as "coming back a little bit now, but a little bit less aggressive" after our initial patches.

## 2. Observations and Context
* **Branch Isolation:** The *default deployed build* (which this dev branch was forked from) does **NOT** trigger this behavior. This confirms that a recent code change in the development build—likely related to text streaming or background model initialization—is the culprit.
* **Thermal Correlation:** The device also experiences significant overheating (CPU thermal throttling), which is correlated with this dev build. When Android OS experiences extreme thermal stress, it is known to panic or shut down hardware radios (like Wi-Fi/Bluetooth) to preserve the device. 

## 3. What Has Been Ruled Out
* **Hardware Failure:** Ruled out because uninstalling the app immediately resolves the network/Bluetooth drops.
* **Solely Transcription-Bound:** Ruled out because the user explicitly noted the drops happen *constantly*, even when the user is not actively pressing the microphone to transcribe.
* **Base Keyboard Architecture:** Ruled out because the default deployed production build does not exhibit this behavior.

## 4. Solutions Applied So Far
* **`clearCommunicationDevice()` Patch:** 
  * *What we did:* We modified `AudioRecognizer.kt` so that when `onFinishRecording()` is called, the app explicitly calls `audioManager.clearCommunicationDevice()`.
  * *Why we did it:* The app was requesting a high-priority "Call/Communication" audio mode (Bluetooth SCO / HFP profile) to capture microphone audio. When Bluetooth SCO is active, the device radio aggressively prioritizes Bluetooth packets over Wi-Fi to prevent "call" dropouts. Since Wi-Fi and Bluetooth share the same 2.4GHz antenna on most phones, keeping SCO open completely starves the Wi-Fi connection and ruins Bluetooth media audio quality.
  * *Result:* The user reported that the issue became "less aggressive," meaning the patch helped mitigate the severity when closing the mic, but the root cause of the *constant background* interference still exists.

## 5. Next Steps / Theories for the Next Engineer
1. **InputMethodService Lifecycle Leak:** Android Keyboards (`InputMethodService`) are kept alive in the background by the OS almost constantly. If `AudioRecognizer`, `ModelManager`, or `AudioManager` is requesting `MODE_IN_COMMUNICATION` or starting Bluetooth SCO during the *initialization* of the keyboard (rather than strictly when the microphone button is pressed), the phone will suffer from Wi-Fi starvation *all the time*. We must audit the `init {}` blocks and constructor calls in the audio pipeline.
2. **Thermal Radio Shutdown:** The dev build still causes extreme CPU overheating due to the Whisper inference. The Wi-Fi/Bluetooth disconnects might not be a software API lock at all, but rather the Android OS physically powering down the radio chips to save the phone from thermal damage. We must profile CPU usage when the keyboard is idle.
3. **Audio Focus / SCO Teardown:** Check if `audioManager.stopBluetoothSco()` or `audioManager.abandonAudioFocus()` needs to be explicitly called. Sometimes `clearCommunicationDevice()` isn't enough on certain Android OEM skins.
