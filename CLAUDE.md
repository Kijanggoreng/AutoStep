# CLAUDE.md - AutoStep

Android macro/auto-clicker app. Kotlin, **no XML layouts** (all UI is built in code), no Compose,
only `androidx.core:core-ktx`. minSdk 26, targetSdk/compileSdk 34, AGP 8.5.2, Kotlin 1.9.24, JDK 17.

## Build / run
```bash
gradle wrapper --gradle-version 8.7     # once, if ./gradlew is missing (wrapper files are not committed)
./gradlew assembleDebug                 # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                  # install on connected device
adb logcat | grep -i autostep
```
CI: `.github/workflows/build.yml` builds the debug APK and uploads it as an artifact.
Android SDK platform 34 required. The code was written without a compile check in the authoring sandbox
(no SDK/network) - if the first build fails, fix compile errors first; they will be small.

## Architecture (package com.autostep.app)
- `Model.kt` - `Step`, `Script`, `StepType`, `KeyMode`, JSON (org.json). `Step.title()/detail()` = list text.
- `Store.kt` - singleton; scripts in `filesDir/scripts.json`; `Store.current()`; call `Store.save()` after edits.
- `Engine.kt` - singleton runner. Snapshot of enabled steps -> background thread. Loop, repeat, delays
  (fixed or random range), `stopRequested` flag, `status` string, listeners (posted to main thread).
- `AutoAccessibilityService.kt` - `instance` singleton. `tap/swipe/hold` use `dispatchGesture` and block
  the calling (worker) thread with a latch. `hold` chains 1s strokes with `willContinue` for infinite holds.
  Also owns `FloatingUi`.
- `FloatingUi.kt` - overlay panel (`TYPE_ACCESSIBILITY_OVERLAY`, no SYSTEM_ALERT_WINDOW needed) and
  `PickerView` (fullscreen picker: POINT / SWIPE / REGION, returns raw screen coords).
- `AutoKeyboardService.kt` - real QWERTY IME (letters/shift, numbers, symbols, enter, backspace); `instance`; `send(KeyEvent)`, `commitText()` used by scripts, posted to main thread.
- `Keys.kt` - `KeyInjector.press/typeText`; resolves random keys, `KeyCharacterMap` for shift/symbols;
  hold = DOWN + repeated DOWN(repeatCount++) every 50 ms + UP (UP always sent in `finally`).
- `MainActivity.kt` - home screen (script card, step list, setup cards, help/welcome dialogs, restricted-settings guide).
- `StepEditor.kt` - step form built only from plain views; used both in the app's AlertDialog and in the floating overlay (`FloatingUi.showForm`). Collapsed 'More options' = random ranges etc. Durations are edited in seconds.
- `App.kt` / `CrashActivity.kt` - uncaught-exception handler -> crash screen with COPY ERROR (separate process).
- `Ui.kt` - `Pal` colours and Context extensions `dp/rounded/label/pill/card`.

## Adding a new step type
1. Add enum value in `StepType` (Model.kt) + fields on `Step` + JSON in `toJson/fromJson` + `title()`.
2. Handle it in `Engine.execute` (exhaustive `when`).
3. Add a branch in `StepEditor.build` (`when (s.type)`) and `MainActivity.defaultStep`.
4. Optional: quick-add chip in `FloatingUi.build`.

## Conventions / gotchas
- Never block the main thread: gestures/keys are executed from the Engine thread.
- Coordinates are absolute screen pixels (rawX/rawY); gestures are clamped to the display.
- Step edits happen on a copy; saved into `Store.current().steps` on "Save".
- Key events only reach the focused app when AutoStep Keyboard is the active IME and an input connection exists
  (`AutoKeyboardService.hasInput()`); this is a platform limit, not a bug.
- Key events use `FLAG_SOFT_KEYBOARD | FLAG_KEEP_TOUCH_MODE` (see `Keys.ev`). If a game ignores them, try
  removing `FLAG_SOFT_KEYBOARD` there so they look like hardware keys.
- After `Pick`, `FloatingUi.openApp()` may be blocked by background-start rules; user can use panel "Open app".
- Keep README.md in sync when features/build steps change.

## Ideas for next steps
Import/export scripts, per-step "run until image/colour appears", conditional steps, repeat-block groups,
Shizuku mode for lower latency, Compose UI.

## Useful Claude skills (Claude Code)
Drjacky/claude-android-ninja, dpconde/claude-android-skill, chrisbanes/skills, Google android/skills
(Android CLI or `npx -y android-skills-mcp`). Install into `~/.claude/skills/` or `.claude/skills/`.

## Release / CI
- `build.yml`: debug APK on push/PR. `release.yml`: push tag `vX.Y.Z` -> release APK + SHA256SUMS on GitHub Releases.
- Release signing via env vars KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD (GitHub secrets KEYSTORE_BASE64 etc.);
  falls back to the debug key. Bump versionCode/versionName and CHANGELOG.md before tagging.
- Do not add the INTERNET permission or any analytics: "no network" is a promise in the README.
- Android 14 gotcha: `Settings.Secure.ENABLED_INPUT_METHODS` is not readable (SecurityException) - use `InputMethodManager.enabledInputMethodList`.
