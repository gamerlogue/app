# Journeys

Natural-language E2E tests for the Android app, in the `android` CLI journey format
(`<journey><actions><action>…`). Each `<action>` is performed/verified in order against
the running app; the journey fails if any action can't be performed or a `verify`/`check`
expectation is false.

## Running

Needs an emulator/device with the app installed (`:androidApp:installBetaDebug`) and the
`android-cli` agent skill (or Gemini in Android Studio). Run a journey via the skill, e.g.
"run the journey `androidApp/src/journeys/discover-open-game.xml`".

These run logged out, so they cover only flows reachable without OAuth (Discover, game
detail, the Library login prompt, Settings/Appearance). Login-gated flows (add-to-library,
library contents) need an authenticated session.

## Files
- `discover-open-game.xml` — Discover loads sections; open a game and go back.
- `library-login-prompt.xml` — Library shows the login prompt when logged out.
- `settings-change-language.xml` — switch language in Appearance and verify the UI updates.
