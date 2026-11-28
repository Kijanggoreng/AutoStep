# Contributing

Thanks for helping! Quick guide:

1. Fork, create a branch, make your change.
2. Build locally: `gradle wrapper --gradle-version 8.7` (once), then `./gradlew assembleDebug`.
3. Test on a real phone (accessibility gestures do not work well on every emulator).
4. Keep the app dependency-light: no network permission, no analytics, no tracking. PRs adding these will not be merged.
5. Update `README.md` / `CHANGELOG.md` if behaviour changes, then open a pull request.

See `CLAUDE.md` for an architecture tour (also useful for humans).
Bug reports: use the template and paste the text from the in-app crash screen if there was one.
