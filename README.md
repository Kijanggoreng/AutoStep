# AutoStep

**A free, open-source macro / auto-clicker for Android.** Record taps, random taps, holds (even "forever"),
swipes and keyboard key presses, then replay them step by step on top of any app or game - with a floating
control panel. No root. No ads. No internet permission.

> **Heads-up:** automating games can break their rules and get your account banned. Use it on things you are
> allowed to automate, and at your own risk.

---

## Contents
- [Features](#features)
- [Install](#install)
- [If Google Play Protect blocks the install](#if-google-play-protect-blocks-the-install)
- [First-time setup (2 minutes)](#first-time-setup-2-minutes)
- [How to use](#how-to-use)
- [Step types](#step-types)
- [Keyboard input (optional)](#keyboard-input-optional)
- [Limitations](#limitations)
- [Troubleshooting](#troubleshooting)
- [Privacy and permissions](#privacy-and-permissions)
- [Build from source](#build-from-source)
- [Publishing a release (maintainers)](#publishing-a-release-maintainers)
- [Project structure](#project-structure)
- [Contributing](#contributing) / [License](#license)

---

## Features

| | |
|---|---|
| **Tap** | One exact spot, with optional random offset (+/- px) |
| **Random tap** | A random spot inside an area you draw on screen |
| **Hold** | Press and hold for a fixed time, a random range, or **forever** (until Stop) |
| **Swipe** | Drag from point A to point B, any speed |
| **Key press** | One key (letter, digit, symbol, ENTER, arrows, F1-F12...), **fixed or random** (letter / digit / symbol / any), can be **held** (e.g. hold `A` for 38 s) |
| **Type text** | Types a string character by character |
| **Wait** | Pause for a fixed or random time |
| **Per step** | Repeat N times, pause afterwards (fixed or random), enable / disable |
| **Scripts** | Multiple scripts, repeat once / N times / forever, start delay |
| **Floating panel** | Draggable Run / Stop button over any app, and add steps without leaving your game |
| **Coordinate picker** | Touch the screen to choose a point, draw an area, or drag a swipe - no typing coordinates |

---

## Install

1. Go to the **[Releases](../../releases)** page and download `AutoStep-vX.Y.Z.apk` on your phone.
   (Optional: compare its SHA-256 with `SHA256SUMS.txt` from the same release.)
2. Open the APK. Android asks you to allow installing from your browser / file manager - allow it.
3. Follow [First-time setup](#first-time-setup-2-minutes).

Requires **Android 8.0 (API 26) or newer**.

---

## If Google Play Protect blocks the install

AutoStep is installed from outside the Play Store and uses the Accessibility service (needed to tap and swipe for
you). Google Play Protect often shows **"Blocked by Play Protect"** or **"App not installed"** for this kind of
app, even though AutoStep contains no network code and no tracking. This is a Play Protect policy for unknown
apps that use accessibility - it can't be "fixed" from inside the app.

**Try this first:** on the Play Protect dialog tap **More details > Install anyway**.

**If there is no such button, temporarily turn Play Protect off:**

1. Open the **Google Play Store** app.
2. Tap your **profile picture** (top-right) > **Play Protect**.
3. Tap the **gear icon** (top-right) > turn **off** *Scan apps with Play Protect*.
4. Install the APK.
5. **Turn Play Protect back ON right after installing** (same steps, switch it back on), so your phone is back to its
   original protection level.

> **Do this at your own risk.** Turning Play Protect off lowers your phone's protection while it is off.
> Only install APKs you trust: download AutoStep **only from this repository's Releases page**, verify the checksum
> if you can, and **make sure you re-enable Play Protect when you are done.** The authors are not responsible for
> any damage, data loss or bans.

(Menu names differ slightly between phone brands and Play Store versions.)

---

## First-time setup (2 minutes)

### 1. Turn on the Accessibility service
1. Open AutoStep. The first card says **"Step 1: turn on the accessibility service"**.
2. Tap **Open Accessibility settings**, find **AutoStep** (often under *Downloaded apps* / *Installed apps*) and
   switch it **ON**.

### 1b. If the switch is greyed out or says "Restricted setting"
Android 13+ blocks accessibility for apps installed outside the Play Store until you allow it. Do this once:

1. In AutoStep, tap **Open Accessibility settings**, tap **AutoStep** and try to switch it on. It refuses - that's
   expected (this attempt is what makes the next menu item appear). Go back.
2. Open **App info** for AutoStep: long-press the AutoStep icon > **App info**
   (or tap **Open App info** in the AutoStep help card).
3. Tap the **three dots (top-right)** > **Allow restricted settings** > confirm.
4. Open AutoStep again > **Open Accessibility settings** > switch **AutoStep** ON as usual.

You can't "close" Settings in the middle of this - just use the Home / Back buttons and re-open things; the order
above is what matters.

### 2. (Optional) Enable the AutoStep keyboard
Only needed for **Key press** and **Type text** steps. See [Keyboard input](#keyboard-input-optional).

When accessibility is on, the **floating panel** appears over your screen.

---

## How to use

### Fastest way: record from your game
1. Open your game. On the floating panel tap **+ Steps** to expand it.
2. Tap **Tap**, **Random**, **Hold** or **Swipe**. The screen dims - touch where you want it (draw an area for
   *Random*, drag for *Swipe*). The step is added to your script.
3. Tap **Key**, **Text** or **Wait** to add those steps; a form opens on top of your game. Choose the options and tap
   **Add step**.
4. Made a mistake? Tap **Undo**.
5. Tap **Run**. You get a few seconds (start delay) to get ready. Tap **Stop** at any time.

### In the app
- **Steps list:** tap a step to edit, use the arrows to reorder, copy or delete it.
- **+ Add step:** pick a type and fill in the form. *Pick on screen* gives you 5 seconds to switch to your game, then
  lets you touch the exact spot.
- **Repeat:** run the script once, a number of times, or **Forever** (until you press Stop).
- **Scripts menu:** create, rename, delete, switch scripts and change the start delay.

### Example: auto-farming loop
1. Tap the attack button, then wait 0.2-0.4 s (random)
2. Random tap inside the enemy area, repeat 5 times
3. Hold the joystick (or key `A`) for 38 s
4. Swipe to turn the camera
5. Type `/loot`, press `ENTER`

Repeat: **Forever**.

---

## Step types

| Step | Options |
|---|---|
| Tap | X/Y (or pick), random offset |
| Random tap | Area (or draw it) |
| Hold | X/Y, seconds, "forever", random max |
| Swipe | From/to, seconds |
| Key press | Fixed key or random letter/digit/symbol/any, hold seconds, "forever", random max |
| Type text | Text |
| Wait | Seconds, random max |
| All steps | Wait afterwards (+ random max), repeat count, enabled |

Key names you can type: `ENTER`, `SPACE`, `BACKSPACE`, `TAB`, `ESC`, `UP`, `DOWN`, `LEFT`, `RIGHT`, `SHIFT`, `CTRL`,
`ALT`, `F1`..`F12`, or any single character (`a`, `A`, `5`, `@`).

---

## Keyboard input (optional)

Android does not let normal apps press keys in other apps. AutoStep works around this the same way keyboard apps
do: it ships its own **keyboard** (a normal QWERTY you can type with). While it is the selected keyboard, scripts can
send key presses and text through it to the app that is focused.

1. AutoStep > **Keyboard** card > **Enable** > switch on *AutoStep Keyboard*.
2. **Select** > choose *AutoStep Keyboard* (you can switch back any time with the globe key on the keyboard).
3. Tap a text field (or the game) so it is focused, then run your script.

**Important:** key presses only reach an app that has an input field focused or that reads key events. Most touch
games ignore them. For games with an on-screen joystick use **Hold** / **Swipe** on the joystick instead.
Key presses work well for chat boxes, console commands, games with keyboard support (emulators, Chromebooks, phones
with a keyboard), and web apps.

---

## Limitations
- Coordinates are screen pixels. If you rotate the phone or change display size, re-record the steps.
- The floating panel can cover things; drag it by the handle (the three-line icon).
- Some apps (banking, secure screens) block overlays / gestures by design.
- Anti-cheat systems can detect accessibility-based automation.
- Some phones (Xiaomi, Oppo, Vivo, Samsung, Tecno...) stop background services to save battery. Set AutoStep's
  battery usage to **Unrestricted** and lock it in the Recents screen.
- The screen stays on while the floating panel is visible.

## Troubleshooting

| Problem | Fix |
|---|---|
| Accessibility switch greyed out | Follow [1b](#1b-if-the-switch-is-greyed-out-or-says-restricted-setting) |
| Play Protect blocks install | See [this section](#if-google-play-protect-blocks-the-install) |
| Taps land in the wrong place | Re-record after rotating or changing display size |
| Key steps do nothing | Select *AutoStep Keyboard* and focus a text field first; see [Keyboard input](#keyboard-input-optional) |
| After "Pick on screen" the app doesn't come back | Tap **Open app** on the floating panel |
| Script stops by itself | Read the message that appears; disable battery restrictions for AutoStep |
| App crashes | An **AutoStep crashed** screen appears: tap **COPY ERROR** and include it in a [bug report](../../issues) |

---

## Privacy and permissions

- **No `INTERNET` permission** - the app cannot send data anywhere. Scripts are stored only on your phone
  (`scripts.json` in the app's private storage).
- **Accessibility service** - used only to perform the gestures you scripted and to show the floating panel. The code
  ignores all screen content and accessibility events (`onAccessibilityEvent` is empty).
- **Keyboard (optional)** - only sends what your script says. Do not use it for passwords; switch back to your usual
  keyboard when you are done.
- No analytics, ads, trackers or third-party SDKs (the only dependency is `androidx.core`).
- Audit it yourself: the whole app is ~2,000 lines of Kotlin in `app/src/main/java/com/autostep/app/`.

---

## Build from source

Requirements: **JDK 17**, **Android SDK** (platform 34), Gradle 8.7+ (or Android Studio).

### Android Studio
1. Install Android Studio (JDK 17 is bundled).
2. **File > Open** the project folder and let it **Sync**.
3. Plug in a phone with USB debugging, press **Run**, or **Build > Build APK(s)**.

### Command line
```bash
gradle wrapper --gradle-version 8.7   # once; generates ./gradlew (commit it if you like)
./gradlew assembleDebug               # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                # install on a connected phone
```

### GitHub Actions
Every push / pull request builds a debug APK (`.github/workflows/build.yml`); download it from the run's
**Artifacts**.

### After modifying the code
Edit files in `app/src/main/java/com/autostep/app/`, run `./gradlew assembleDebug` again and install the new APK
over the old one (same signing key = no uninstall needed). Bump `versionCode` / `versionName` in
`app/build.gradle.kts` for releases.

---

## Publishing a release (maintainers)

1. **(Recommended) Create a release keystore once** and keep it safe - losing it means users must uninstall to update:
   ```bash
   keytool -genkeypair -v -keystore release.jks -alias autostep -keyalg RSA -keysize 4096 -validity 36500
   base64 -w0 release.jks     # copy the output
   ```
2. In your GitHub repo: **Settings > Secrets and variables > Actions**, add `KEYSTORE_BASE64` (the base64 text),
   `KEYSTORE_PASSWORD`, `KEY_ALIAS` (`autostep`), `KEY_PASSWORD`.
   *(Skip this and the release APK is signed with the debug key - fine for testing.)*
3. Update `versionCode`, `versionName` and `CHANGELOG.md`, commit, then:
   ```bash
   git tag v1.1.0 && git push origin v1.1.0
   ```
4. The **Release** workflow builds the APK and attaches `AutoStep-v1.1.0.apk` + `SHA256SUMS.txt` to a new release.

---

## Project structure

```
AutoStep/
  README.md  CLAUDE.md  CHANGELOG.md  CONTRIBUTING.md  LICENSE
  settings.gradle.kts  build.gradle.kts  gradle.properties
  .github/workflows/build.yml      # debug APK on every push/PR
  .github/workflows/release.yml    # tag vX.Y.Z -> GitHub Release with APK
  app/
    build.gradle.kts               # minSdk 26, target/compile 34, Kotlin 1.9.24, AGP 8.5.2
    src/main/AndroidManifest.xml
    src/main/res/xml/accessibility_config.xml
    src/main/java/com/autostep/app/
      MainActivity.kt              # home screen, onboarding, help, step list
      StepEditor.kt                # step form (used in app dialog AND floating overlay)
      FloatingUi.kt                # floating panel, coordinate picker, overlay form
      AutoAccessibilityService.kt  # tap / hold / swipe via dispatchGesture
      AutoKeyboardService.kt       # QWERTY keyboard + key/text injection point
      Keys.kt                      # KeyInjector: random keys, held keys, shift/symbols
      Engine.kt                    # runs a script (loops, repeats, random delays)
      Model.kt  Store.kt           # data classes, JSON storage
      App.kt  CrashActivity.kt     # crash reporter screen
      Ui.kt                        # colours + view helpers
```

How it works: taps/holds/swipes use `AccessibilityService.dispatchGesture` (long holds are chained 1-second strokes
with `willContinue`, so the finger never lifts); the overlay uses `TYPE_ACCESSIBILITY_OVERLAY` (no "draw over
other apps" permission); key presses go through the keyboard's `InputConnection.sendKeyEvent` (DOWN, repeated DOWN
with increasing `repeatCount`, then UP).

---

## Contributing
Issues and pull requests are welcome - see [CONTRIBUTING.md](CONTRIBUTING.md).

## License
[MIT](LICENSE). AutoStep is not affiliated with or endorsed by Google. "Play Protect" and "Android" are trademarks of
Google LLC.

## Credits / inspiration
Open-source autoclickers and docs that informed the design: eci99/Clicker, Kiyoscripts/CosmoClicker, ryanuo/mdot,
kemw/android-mouse-cursor, and the Android "Create an input method" documentation.
