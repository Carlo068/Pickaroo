#!/usr/bin/env bash
# Helpers to drive the Pickaroo app on the emulator with adb. Source it:
#   . .claude/skills/emulator-ui-check/scripts/pk.sh
# Prefer waiting for text and tapping by text: they survive a slow emulator, fixed sleeps and blind
# coordinates do not.
ADB="${ADB:-$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe}"
PK=com.example.pikaroo
SHOTS="${SHOTS:-$(cygpath -m "${TMPDIR:-/tmp}")/pickaroo-shots}"
mkdir -p "$SHOTS"
PK_HERE="$(cygpath -m "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)")"

pk_ready() {    # fails if there is no emulator
  [ "$("$ADB" devices | tail -n +2 | grep -c 'device$')" -ge 1 ] || { echo "no emulator running"; return 1; }
}
pk_tap()  { "$ADB" shell input tap "$1" "$2"; }
pk_text() { "$ADB" shell input text "$1"; }                 # plain characters only; %s is a space
pk_swipe_up() { "$ADB" shell input swipe 540 1900 540 700 400; sleep 2; }
pk_back() { "$ADB" shell input keyevent 4; sleep 2; }

pk_dump() {     # the visible accessibility tree as XML
  # MSYS_NO_PATHCONV: Git Bash would otherwise rewrite /sdcard/... into a Windows path. Only here, not globally.
  MSYS_NO_PATHCONV=1 "$ADB" shell uiautomator dump /sdcard/pk_ui.xml >/dev/null 2>&1
  MSYS_NO_PATHCONV=1 "$ADB" exec-out cat /sdcard/pk_ui.xml
}
pk_find() {     # pk_find TEXT -> "x y" of the first match; fails if absent
  pk_dump | python "$PK_HERE/find_node.py" "$@"
}
pk_wait_text() {  # pk_wait_text TEXT [seconds=40]; no TEXT waits for any text on screen
  local end=$((SECONDS + ${2:-40}))
  while [ "$SECONDS" -lt "$end" ]; do
    if [ -n "${1:-}" ]; then pk_find "$1" >/dev/null && return 0; else pk_find >/dev/null && return 0; fi
    sleep 2
  done
  echo "timed out waiting for '${1:-any text}'"
  return 1
}
pk_tap_text() {   # pk_tap_text TEXT [seconds=40]: wait for it, then tap its centre
  pk_wait_text "$1" "${2:-40}" || return 1
  local position
  position="$(pk_find "$1")" || return 1
  pk_tap ${position}
  sleep 2
}

pk_launch() {   # restart the app keeping its data (session, onboarding) and wait for the first screen
  "$ADB" shell am force-stop "$PK"
  "$ADB" shell am start -n "$PK/.MainActivity" >/dev/null
  pk_wait_text "" 60
}
pk_fresh() {    # wipe data (logged out, onboarding shown) and start
  "$ADB" shell pm clear "$PK" >/dev/null
  pk_launch
}
pk_skip_onboarding() {   # from onboarding to the login screen
  pk_tap_text "Saltar" && pk_wait_text "Iniciar Sesión" 40
}
pk_login() {    # pk_login USER PASSWORD (login screen showing)
  pk_wait_text "Iniciar Sesión" 20 || return 1
  pk_tap 545 1029; sleep 1; pk_text "$1"      # the fields keep the same place on every phone size used here
  pk_tap 545 1301; sleep 1; pk_text "$2"
  "$ADB" shell input keyevent 111; sleep 1    # hide the keyboard
  pk_tap 539 1616                             # the button; the caller then waits for the next screen's text
  sleep 4
}
pk_tab() {      # pk_tab Inicio|Ofertas|Ordenar|Productos|Usuario
  pk_tap_text "$1" 30
}
pk_shot() {     # pk_shot NAME -> prints the PNG path
  "$ADB" exec-out screencap -p > "$SHOTS/$1.png"
  echo "$SHOTS/$1.png"
}
pk_combine() {  # pk_combine OUT NAME[:x0,y0,x1,y1] ... -> side-by-side image; open it with the Read tool
  local out="$1"; shift
  python "$PK_HERE/combine.py" "$SHOTS/$out.png" "$SHOTS" "$@"
}
