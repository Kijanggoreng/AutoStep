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
- `AutoKeyboardService.kt` - IME; `instance`; `send(KeyEvent)`, `commitText()` posted to main thread.
- `Keys.kt` - `KeyInjector.press/typeText`; resolves random keys, `KeyCharacterMap` for shift/symbols;
  hold = DOWN + repeated DOWN(repeatCount++) every 50 ms + UP (UP always sent in `finally`).
- `MainActivity.kt` - setup status, script card, step list, step editor dialog (`showEditor`).
- `Ui.kt` - `Pal` colours and Context extensions `dp/rounded/label/pill/card`.

## Adding a new step type
1. Add enum value in `StepType` (Model.kt) + fields on `Step` + JSON in `toJson/fromJson` + `title()`.
2. Handle it in `Engine.execute` (exhaustive `when`).
3. Add a branch in `MainActivity.showEditor` (`when (s.type)`) and `defaultStep`.
4. Optional: quick-add button in `FloatingUi.build`.

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
