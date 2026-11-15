# Gamerlogue

A game-library tracker built with Kotlin Multiplatform and Compose Multiplatform, for Android, desktop (JVM) and the
browser (JS). All UI and logic live in `:sharedUI`; `androidApp`, `desktopApp` and `webApp` are thin launchers.

## Requirements

- **JDK 21 or newer.** The JBR bundled with Android Studio works: point `JAVA_HOME` at it
  (`<Android Studio>/jbr`) if no other JDK is installed. CI builds with Temurin 25.
- **Android SDK** with the platform in `androidCompileSdk` (`gradle.properties`). Android Studio installs it and writes
  `sdk.dir` in `local.properties`.
- **Android Studio** or IntelliJ IDEA with the Android plugin. Android Studio 2026.2.2 breaks the Gradle sync: stay on
  2026.2.1 until a fix ships.
- **detekt** IDE plugin, for linting (see [Checks](#checks)).

## Setup

1. Clone the repository and open it in Android Studio; let the Gradle sync finish.
2. Create `local.properties` in the root if the IDE did not, and set the environment:

   ```properties
   sdk.dir=<path to the Android SDK>
   # LOCAL enables verbose logging (HTTP headers included) and the technical details in the error sheet.
   # PRODUCTION for release-like builds. Defaults to LOCAL.
   APP_ENV=LOCAL
   ```

3. *(Optional)* Set up Firebase for push notifications, see [below](#push-notifications-firebase). The app builds and
   runs without it, just without push.

## Backend

The app talks to the Gamerlogue backend (the `gamerlogue_web` repository) and to IGDB through it. Both URLs are runtime
settings, in **Settings → Advanced → Server**:

- Default: `http://10.0.2.2` on an Android emulator, `https://gamerlogue.maicol07.it` everywhere else.
- IGDB follows `<server>/api/igdb` unless set separately.

To develop against a local backend:

- **Android emulator:** `10.0.2.2` is the host machine. The backend's Caddy must serve it over plain HTTP: set
  `CADDY_HTTP_SERVER_ADDRESS` and `SSL_MODE: mixed` on the `laravel` service in `gamerlogue_web`, otherwise it
  redirects to HTTPS. From API 36 the app also needs `ACCESS_LOCAL_NETWORK`: the Gradle `install*Debug` tasks grant it
  over `adb` after installing, but a Run from Android Studio does not, so the app asks for it on start.
- **Desktop and web:** set the server to the backend's local address in the settings.

## Push notifications (Firebase)

Only the Android app receives push notifications, through Firebase Cloud Messaging.

1. In the [Firebase console](https://console.firebase.google.com/), create a project (or open the existing one).
2. Add two Android apps to it: `it.maicol07.gamerlogue` (release) and `it.maicol07.gamerlogue.dev` (debug builds).
   FCM needs no SHA-1 fingerprint.
3. Download `google-services.json` (from either app: it covers both) into `androidApp/`. It is not committed; the
   Google Services Gradle plugin is applied only when the file exists.
4. For CI, store the file's content in the `GOOGLE_SERVICES_JSON` repository secret: `build.yml` writes it back before
   the Android build.
5. For the backend, generate a service account key in **Project settings → Service accounts** and give it to
   `gamerlogue_web`, which sends the messages through the FCM HTTP v1 API. The device token endpoint is described under
   Notification → Push in [docs/CONTRACTS.md](docs/CONTRACTS.md).

To check the client alone, sign in on a device (the app asks for the notification permission on Android 13+), then send
a message from **Messaging → New campaign → Notifications** in the console, targeting the app. Tapping it opens the
notifications list.

## Run

| Target | Command |
|---|---|
| Android | Run the `androidApp` configuration, or `./gradlew :androidApp:installBetaDebug` |
| Desktop | `./gradlew :desktopApp:run` (hot reload: `./gradlew :desktopApp:hotRun --auto`) |
| Web (JS) | `./gradlew :webApp:jsBrowserDevelopmentRun` |

Android has two flavors, `beta` and `alpha`, which differ only in the launcher icon; debug builds get the `.dev`
application id, so they install next to a release build.

Packaging:

- Android APK: `./gradlew :androidApp:assembleBetaDebug` → `androidApp/build/outputs/apk/beta/debug/`
- Desktop installers (DMG, EXE, DEB, for the current OS): `./gradlew :desktopApp:packageDistributionForCurrentOS`
- Web: `./gradlew :webApp:jsBrowserDistribution`

## Checks

- Tests: `./gradlew :sharedUI:jvmTest`, or one class with `--tests "it.maicol07.gamerlogue.services.WebResultTest"`.
- Lint: detekt has no Gradle task. It runs through the IDE plugin against `detekt.yml` (or the detekt CLI 1.23.8 with
  the same config), so a green build does not mean clean files.

Project conventions, architecture notes and known issues are in [AGENTS.md](AGENTS.md); the backend API contract is in
[docs/CONTRACTS.md](docs/CONTRACTS.md).

## Release

Releases are cut by CI: every push to `main` opens or updates a release PR with the changelog, and merging it tags,
publishes the GitHub Release with the builds and deploys the web app. Details in the Release section of
[AGENTS.md](AGENTS.md). A signed Android release build reads the
keystore from `ANDROID_KEYSTORE_PATH` (default: `release.keystore` in the root) and its credentials from
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` and `ANDROID_KEY_PASSWORD`.

# Credits
## Icons
<a href="https://www.flaticon.com/free-icons/gaming" title="gaming icons">Gaming icons created by Flat Icons - Flaticon</a>
Icon generated with: https://icon.kitchen/i/H4sIAAAAAAAAA11Sy07DMBD8l-VaKiepE7c3QIITJ3qi6mETb9wIJw5xAq2q_jtr94EgjizP7Hr25SN8oZ3Iw-oItXly1g2wgjtVZLxgBuUvl2OmSTFXm_WhJ6aaFg1FpwthBtQNdWPkXq6Apf3o-hhDBKVClZUU7JQElGnM84SRmAux-GMX86w4M6JcyjQyi2UWs5GllDIy8j9WUTdPy4XMI7OUieJfiUQolasimBcJFrmA0wywM5azzxLJgGuh_TgNTBxhvB5hh7YeXReqra4NSVHqLAkKpXnbYeyA_5yQL8xg3FFLGlY1Wk-hZ6_oP26w948mSCS1TNNQe--fI1HHLxLrZgxpwQu2NFhnpiDLfu7c0hrbxh7Y_vBI9E4U80Bt6DYwkYYVB_ZWoW26EKHiidAQB_RKfheUetd0I09nc4Q9rMQ8lTNg4fPhVm2xrElmHOXiVVy9wuHmdRnmaXvidFqnJxve1oZ7rAfXaI7bOM_7N5W8t1hF1Fs83PMb4c5tTz8mXxgekwIAAA
