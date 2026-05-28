# CODE_RULES

## Local Conventions

- Follow existing codebase patterns before inventing new ones.
- Keep edits scoped to the active task.
- Prefer small, reversible changes.
- Do not add dependencies without explicit rationale.
- Prefer the existing FUTO Keyboard action, settings, Compose UI, and DataStore patterns.
- Keep voice streaming work scoped to the existing voice input pipeline.
- Preserve offline/local privacy behavior.
- Preserve the `ActionInputTransaction` state machine: zero or more `updatePartial()` calls followed by exactly one `commit()` or `cancel()`.
- Use `ModelOutputSanitizer.sanitize(text, textContext)` before every partial update and final commit.
- Marshal all UI and input transaction work to `Dispatchers.Main` when coming from voice recognition callbacks.
- Do not modify native/JNI or OS input plumbing unless a narrowly scoped bug requires it and the rationale is recorded.
- Avoid dependency additions or upgrades unless required to unblock build or verification.

## Verification Commands

```bash
# test
./gradlew test

# lint
./gradlew lint

# format/check
# No formatter configured

# build
./gradlew assembleUnstableDebug
```

## Artifact Rules

For UI changes, provide a screenshot or emulator capture of the affected settings screen. For voice streaming behavior, provide emulator evidence showing partial transcription appearing in the active input field while speaking, preferably as a short screen recording or screenshot sequence. For disabled behavior, provide evidence that existing voice input still waits for final transcription and commits as before. For build work, provide Gradle output showing a successful `assembleUnstableDebug` build and identify the generated APK path. For emulator verification, provide evidence that the APK installs, launches, and is present for hands-on testing. For data or configuration changes, provide before/after setting behavior and note any required model or emulator setup.
