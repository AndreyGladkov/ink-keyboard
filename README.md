# Ink Keyboard

An Android keyboard for e-ink screens. Ink Keyboard is a fork of [HeliBoard](https://github.com/HeliBorg/HeliBoard), which is itself based on the AOSP keyboard (LatinIME).

Regular keyboards are designed for LCD and OLED screens. On e-ink, their gray fills, shadows, key popups and animations cause ghosting and input lag. Ink Keyboard keeps the familiar AOSP keyboard and changes how it looks and behaves on e-ink panels.

## Features

- **"E-ink" style.** Keys are drawn as an outline and a label only. There are no fills, shadows or gradients.
  - Two schemes: black on white, and inverted (white on black).
  - Adjustable border width, font size and weight, and corner radius.
- **Pure black and white rendering.** Antialiasing is off by default, so fast refresh modes don't turn grey edges into dither noise. It can be turned back on.
- **No animations or key preview popups.** Long-press popups, the emoji panel, the clipboard and the suggestion list follow the same style and appear instantly.
- **Haptic feedback instead of visual.** Inverting the pressed key is available as an option.
- **Black and white emoji** on the emoji panel, using [Noto Emoji](https://github.com/google/fonts/tree/main/ofl/notoemoji).
- **Hisense A9 integration (optional).** A full screen refresh when the keyboard hides. It is sent through the broadcast API of [A9PatchingService](https://github.com/AndreyGladkov/A9PatchingService).
- **Gesture typing (planned).** It will use an open source decoder and draw no trail.
- **Offline only.** The app has no internet permission, see the [privacy policy](PRIVACY.md).

Typing, dictionaries, layouts and the rest of the settings are HeliBoard's. Its documentation applies: [layouts.md](layouts.md) and the [HeliBoard wiki](https://github.com/HeliBorg/HeliBoard/wiki).

## Building

You need JDK 21 or newer, the Android SDK, and NDK `28.0.13004108`.

```sh
./gradlew testRunTestsUnitTest   # unit tests
./gradlew assembleRelease        # F-Droid / GitHub build: lets the user load Google's gesture library
./gradlew assembleNouserlib      # Google Play build: the user library is disabled
./gradlew assembleDebug          # debug build, installs next to the release build
```

Builds use the application id `io.github.andreygladkov.inkkeyboard`, so Ink Keyboard can be installed alongside HeliBoard.

## Releasing

Release builds are signed only when `local.properties` names the keystores; CI builds stay unsigned.

```properties
signing.github.storeFile=/path/to/ink-keyboard-github.jks
signing.github.storePassword=...
signing.github.keyAlias=github
signing.github.keyPassword=...
signing.playUpload.storeFile=/path/to/ink-keyboard-play-upload.jks
signing.playUpload.storePassword=...
signing.playUpload.keyAlias=playUpload
signing.playUpload.keyPassword=...
```

- `./gradlew assembleRelease` produces the APK for GitHub Releases, signed with the `github` key.
- `./gradlew bundleNouserlib` produces the bundle for Google Play, signed with the `playUpload` key.

Store texts and screenshots live in `fastlane/metadata/android`. Changelogs are named after the `versionCode`, e.g. `changelogs/4201.txt`.

### Version codes

`versionCode` is Ink Keyboard's own and only goes up: raise it for every release (4201 was the first after 4.2-ink.1, then 4202, 4203, …). Changelogs are named after it.

`HELIBOARD_VERSION_CODE` in `app/build.gradle.kts` is the `versionCode` of the HeliBoard release the fork is based on. Settings migrations in `AppUpgrade.kt` are inherited from HeliBoard and run as `if (oldVersion <= N)` with HeliBoard codes, so they compare against this value, which the settings store as `heliboard_version_code`.

- When merging a HeliBoard release, set `HELIBOARD_VERSION_CODE` to its `versionCode`. The new migrations then run once for Ink Keyboard users, whether or not `versionCode` changes in the same build.
- Merge HeliBoard releases, not commits between them. Upstream gates a new migration on the previous release's code while `versionCode` is already past it (the `oldVersion <= 4005` block landed with `versionCode = 4006`), so settings stored with a mid-cycle code would skip the migrations added later in that cycle.
- Settings without `heliboard_version_code` come either from a HeliBoard backup, where `version_code` is a HeliBoard code, or from 4.2-ink.1, which stored `version_code = 1` and is treated as HeliBoard 4201.

## Relationship to HeliBoard

Ink Keyboard is a soft fork. Changes are kept small and isolated so upstream releases can be merged regularly. Improvements that aren't specific to e-ink are proposed upstream, see [HeliBoard#969](https://github.com/HeliBorg/HeliBoard/issues/969). Please report issues in the Ink Keyboard repository first, not to HeliBoard.

## License

Ink Keyboard is licensed under the [GNU General Public License v3.0](LICENSE), like HeliBoard and OpenBoard.

The app is based on the Apache 2.0 licensed AOSP Keyboard, so an [Apache 2.0](LICENSE-Apache-2.0) license file is included.

## Credits

- [HeliBoard](https://github.com/HeliBorg/HeliBoard) and its [contributors](https://github.com/HeliBorg/HeliBoard/graphs/contributors)
- [OpenBoard](https://github.com/openboard-team/openboard)
- [AOSP Keyboard](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/)
- [LineageOS](https://review.lineageos.org/admin/repos/LineageOS/android_packages_inputmethods_LatinIME)
- [Simple Keyboard](https://github.com/rkkr/simple-keyboard)
- [Indic Keyboard](https://gitlab.com/indicproject/indic-keyboard)
- [FlorisBoard](https://github.com/florisboard/florisboard/)
- Black and white emoji: [Noto Emoji](https://github.com/google/fonts/tree/main/ofl/notoemoji) by Google, [SIL Open Font License 1.1](app/src/main/assets/fonts/NotoEmoji-OFL.txt)
