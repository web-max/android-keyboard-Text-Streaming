# Emulator & Build Setup

To achieve a repeatable verification environment for the APK, you need to configure Java, the Android SDK, and an Android emulator.

## 1. Environment Setup

1. **Java Development Kit (JDK) 17**: Ensure Java 17 is installed and set as `JAVA_HOME`.
   ```bash
   export JAVA_HOME=/path/to/jdk-17
   ```
2. **Android SDK & NDK**: 
   - Install Android Command Line Tools (e.g., `brew install --cask android-commandlinetools` on macOS).
   - Use `sdkmanager` to install the required components. Accept licenses when prompted.
   ```bash
   sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0" "emulator" "system-images;android-35;google_apis;arm64-v8a" "ndk;28.2.13676358"
   ```
3. **Configure local.properties**:
   Create a `local.properties` file in the project root pointing to your SDK root.
   ```properties
   sdk.dir=/Users/<YourUser>/homebrew/share/android-commandlinetools
   ```

## 2. Build the APK

Run the Gradle build to generate the unstable debug APK:
```bash
./gradlew assembleUnstableDebug
```

## 3. Emulator Setup

1. **Create an Android Virtual Device (AVD)**:
   ```bash
   avdmanager create avd -n test_emulator -k "system-images;android-35;google_apis;arm64-v8a" --device "pixel_5"
   ```
2. **Start the Emulator**:
   ```bash
   emulator -avd test_emulator -no-window -no-snapshot -no-audio &
   ```
3. **Wait for Boot**:
   ```bash
   adb wait-for-device
   ```

## 4. Install & Verification

Install the built APK onto the running emulator:
```bash
adb install build/outputs/apk/unstable/debug/android-keyboard-Text-Streaming-unstable-debug.apk
```
*(Path may vary depending on the exact build variants directory structure.)*

Finally, launch the app via ADB to verify it doesn't crash on startup.
