---
name: android-test-run
description: Run the Pickaroo Android unit tests and/or the emulator (instrumented) tests and report pass/fail counts with failure reasons. Use after implementing a task and before committing, or whenever asked to "run the tests".
---

# Run the Pickaroo tests

```bash
.claude/skills/android-test-run/scripts/run-tests.sh unit
.claude/skills/android-test-run/scripts/run-tests.sh instrumented [package]   # needs the emulator
.claude/skills/android-test-run/scripts/run-tests.sh all
```

The script sets the JDK (Android Studio's), deletes old result files, runs Gradle, and prints
`unit: N/N passed` or `FAIL Class.test: reason`. Exit code: 0 ok, 1 failures, 2 no emulator.

## Rules that came from real failures

- **Never start or stop the emulator.** The user boots `Pixel_10` from Android Studio. On exit code 2 (no device), ask them to start it.
- **Filter while iterating.** `run-tests.sh instrumented com.example.pikaroo.ui.orders` runs one package (about 2 min instead of 5+).
- **"Test run failed ... Process crashed" is not always your code.**
  1. Look for a real exception in the output. The script clears logcat first, so any "Logcat of last crash" is fresh.
  2. A crash at `PikarooApp.<init>` or in `Application` means startup code ran too early (build things lazily; `Context` is null in the constructor).
  3. If there is no exception, the emulator probably killed the process. Rerun once; it passed on the second run every time this happened.
- **"FileSystemException ... being used by another process" on `androidTest-results`.** An old `gradlew ... connectedDebugAndroidTest` (from an interrupted run) is still alive and holds the logcat files. List Java processes with PowerShell `Get-CimInstance Win32_Process -Filter "Name='java.exe'"`, find the one whose command line contains `gradle-wrapper.jar ... connectedDebugAndroidTest` for this project, and stop only that process. Never stop Android Studio's own Gradle daemon.
- **Stale numbers lie.** Before trusting a count, make sure the run actually printed `Finished N tests`.
- Instrumented tests uninstall the app at the end. Reinstall with `./gradlew :app:installDebug` before checking by hand.
- Unit tests need `testOptions.unitTests.isReturnDefaultValues = true` (already set) because code calls `android.util.Log`.
- When a single test fails, fix the cause (or the test if its expectation was wrong) and rerun only the affected package.

## Reporting

One line per suite (`unit: 155/155`, `instrumented: 53/53`) plus each failure's class, test name and first message line. No stack traces.
