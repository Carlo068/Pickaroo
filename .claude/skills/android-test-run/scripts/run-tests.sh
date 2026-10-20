#!/usr/bin/env bash
# Run Pickaroo tests and print a short summary.
# Usage: run-tests.sh unit|instrumented|all [package-filter]
#   package-filter (instrumented only), e.g. com.example.pikaroo.ui.orders
# Env overrides: JAVA_HOME, ADB
set -u
cd "$(git rev-parse --show-toplevel)"
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Android/Android Studio/jbr}"
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
here="$(cd "$(dirname "$0")" && pwd)"
mode="${1:-all}"
package="${2:-}"
status=0
log="$(mktemp)"
trap 'rm -f "$log"' EXIT

run_unit() {
  rm -rf app/build/test-results/testDebugUnitTest   # never count stale results
  ./gradlew :app:testDebugUnitTest --console=plain -q >"$log" 2>&1
  grep -vE '^w:' "$log" | head -20
  python "$here/summarize_results.py" unit || status=1
}

run_instrumented() {
  if [ "$("$ADB" devices | tail -n +2 | grep -c 'device$')" -lt 1 ]; then
    echo "BLOCKED: no emulator running. Start Pixel_10 from Android Studio and retry."
    status=2
    return
  fi
  "$ADB" logcat -b all -c   # Gradle prints the device's *last* crash; clear it so it isn't stale
  args=()
  [ -n "$package" ] && args+=("-Pandroid.testInstrumentationRunnerArguments.package=$package")
  ./gradlew :app:connectedDebugAndroidTest ${args[@]+"${args[@]}"} --console=plain >"$log" 2>&1
  grep -E 'FAILED|BUILD|Finished|^e:|FileSystemException|Could not' "$log" | head -10
  if ! grep -q 'Finished [0-9]* tests' "$log"; then
    echo "RUN DID NOT FINISH: no 'Finished N tests' line, so any result files are stale. See the errors above."
    echo "If it says 'being used by another process', an old gradlew connectedDebugAndroidTest is still alive: see SKILL.md."
    status=1
    return
  fi
  python "$here/summarize_results.py" instrumented || status=1
}

case "$mode" in
  unit) run_unit ;;
  instrumented) run_instrumented ;;
  all) run_unit; run_instrumented ;;
  *) echo "usage: run-tests.sh unit|instrumented|all [package-filter]"; exit 64 ;;
esac
exit $status
