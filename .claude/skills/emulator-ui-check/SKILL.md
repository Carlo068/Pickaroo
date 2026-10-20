---
name: emulator-ui-check
description: Drive the installed Pickaroo app on the Android emulator with adb (fresh start, skip onboarding, log in as a mock user, switch tabs, screenshot) to check a screen by eye. Use after UI changes, before reporting a UI task done.
---

# Check the app on the emulator

The emulator must already be running (the user starts `Pixel_10` from Android Studio; never start or stop it). Install the current code first:

```bash
JAVA_HOME="C:/Program Files/Android/Android Studio/jbr" ./gradlew :app:installDebug --console=plain -q
```

Then source the helpers and drive the app:

```bash
. .claude/skills/emulator-ui-check/scripts/pk.sh
pk_ready || exit 1
pk_fresh                      # wipe data and launch (onboarding)
pk_skip_onboarding            # taps "Saltar" twice
pk_login customer1 demo1234   # mock users below
pk_tab usuario
pk_shot usuario               # prints the PNG path; view it with the Read tool
pk_combine flow login:0,0,1080,1100 usuario:0,0,1080,1100   # several steps in one image
```

`pk_launch` restarts keeping the session (to test "stays logged in"); `pk_swipe_up`, `pk_back`, `pk_tap X Y`, `pk_text WORD` are the building blocks.

## Mock users (password `demo1234`)

| username | starts in | notes |
|---|---|---|
| `customer1` | customer tabs | orders 1001, 1003, 1004, 1006, 1008, 1010 |
| `owner1` | shop home | also a customer; has "Cambiar a modo cliente"; orders 1002, 1005, 1007, 1009 (1009 is out for delivery) |
| `staff1` | shop home | |
| `driver1` | driver home | |

## Screen coordinates (1080x2424)

Skip onboarding (166, 2232) · username field (545, 1029) · password (545, 1301) · login button (539, 1616) · cart icon (974, 248) · tab bar y=2293 at x = 99, 319, 538, 759, 978 · Usuario tab: "Mis pedidos" card (440, 800) before scrolling, "Cerrar sesión" (539, 1938) after two `pk_swipe_up`.
Screenshots shown to you are scaled to 891x2000: multiply their coordinates by 1.21.

## Things that went wrong before

- **The first tap after a launch is often dropped.** The helpers tap twice with pauses. If a step seems ignored, take a screenshot before assuming the app is broken.
- Type with `pk_text` only plain characters; hide the keyboard (`keyevent 111`) before tapping the button underneath it.
- After `pm clear` the app starts on onboarding; after a normal `pk_launch` it keeps the session and lands on the user's home.
- Instrumented tests uninstall the app. Run `installDebug` again before any manual check.
- If taps are lost everywhere, the emulator is overloaded (check `adb shell uptime`; a load average above ~10 is bad): ask the user to restart it rather than retrying in a loop.
- Don't claim a screen "looks right" without opening the screenshot with Read.
