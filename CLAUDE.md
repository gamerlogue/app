# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Gamerlogue is a Kotlin Multiplatform + Compose Multiplatform game-library tracker. Targets: **Android** (`androidApp`), **JVM desktop** (`desktopApp`), and **JS browser** (`webApp`). All UI and logic live in the shared `:sharedUI` module; the platform modules are thin launchers. iOS targets are configured (Apple frameworks) but there is no `iosApp`.

## Build & run

`JAVA_HOME` is usually unset — run Gradle through PowerShell using the Android Studio JBR. The wrapper is `./gradlew` (`gradlew.bat` on Windows).

- Android APK: `./gradlew :androidApp:assembleDebug` → `androidApp/build/outputs/apk/debug/`
- Desktop: `./gradlew :desktopApp:run` — hot reload: `./gradlew :desktopApp:hotRun --auto`
- Web (JS): `./gradlew :webApp:jsBrowserDevelopmentRun`
- Lint (detekt): `./gradlew detekt`
- All tests: `./gradlew :sharedUI:jvmTest`
- Single test class: `./gradlew :sharedUI:jvmTest --tests "it.maicol07.gamerlogue.services.WebResultTest"`

Unit tests run on the JVM only (Kotest + JUnit Platform, in `sharedUI/src/jvmTest`); the JS target is excluded. `commonTest` is for Compose UI tests.

`local.properties` supplies build config via the `buildConfig` plugin: `APP_ENV` (LOCAL/…), `IGDB_API_URL`, `GAMERLOGUE_URL`. These surface as `BuildConfig.*`. SDK levels and `appPackageName` come from `gradle.properties`.

## Conventions (enforced)

- **detekt**: top-level constants use PascalCase, not `UPPER_SNAKE` (`TopLevelPropertyNaming` / `constantPattern`). Max line length 150. Comments in English only.
- Backend list endpoints are **page-based** (reject `page[offset]`); JSON:API queries scoped to the user pass a `current_user=true`-style param via the `currentUserEntries()` extension.

## Architecture

### State & DI
- **Koin** for DI, started in `App.kt` with `appModule`, `httpModule`, and an `expect val platformModule` (one `actual` per target). ViewModels are registered with `viewModel { }` and parameterized factories (`viewModel { (id: Int) -> … }`).
- **ViewModels** extend `StateViewModel<S>(initial)` (in `core/`), which wraps a private `MutableStateFlow`. Read via `state`, mutate via `update { copy(...) }`. ViewModels hold **no navigation** — screens pass nav as callbacks.

### Navigation
- **Navigation 3** (`androidx.navigation3`), not the old Compose Navigation. Destinations are `@Serializable` objects/classes in `NavKeys` (top-level `NavKeys.kt`), extending `NavKeyWithMeta` (`title`, `showBottomBar`). The back stack is a single Koin-provided `NavBackStack` singleton.
- `AppNavDisplay` registers one entry per key and uses the **adaptive list-detail** scene strategy (`ListDetailSceneStrategy.listPane()/detailPane()`). The `screen<K>{}` helper wraps content in `ScreenScaffold`; use plain `entry<K>{}` for screens that draw their own bar (e.g. game detail).
- New `@Serializable` keys with backing-field properties must register a `subclass(...)` in `NavKeys.savedStateConfiguration`. Use custom getters (no backing field) for derived properties so only data fields serialize (see `GameList`, `LibraryImportPreview`).

### Data layer (two APIs)
- **Gamerlogue backend**: JSON:API via **SprayPaintKT**. Schemas are `@ResourceSchema` interfaces in `data/` (e.g. `LibraryEntrySchema`) annotated with `@Attr`/`@Relation`; KSP generates the concrete models (`LibraryEntry`, `User`). `AppJsonApiConfig` is the `@DefaultInstance`. Auth is bearer-token (Laravel Sanctum) via the `JsonApiHttpClient`-qualified Ktor client in `httpModule`.
- **IGDB**: game metadata via `igdbclient` on its own Ktor client (separate so tests can swap just one). Has retry/backoff including explicit 429 handling.
- Wrap network calls in `safeRequest { }` (top-level `utils.kt`): it returns a `kotlin-result` `Result`, reports `IgdbException`/`JsonApiException` to the global UI error state, and swallows `CancellationException`.

### Linked services sync (`services/`)
Client-side library/wishlist sync with external stores (Steam, PlayStation, Xbox, GOG, Epic) driven by **WebView automation** — no official store APIs for most. The user logs into the store in a `ServiceWebView`; injected JavaScript reads/writes the authenticated same-origin session and delivers results through a JS bridge (`SyncScripts.wrap`). Each store is one `ServiceConnector` subclass overriding URLs + scripts; connectors are registered as a `Map<ExternalService, ServiceConnector>` in `appModule`. `GameMatcher` maps store refs to IGDB games; `LibrarySync` writes them as `LibraryEntry`s. PSN/Xbox additionally use off-WebView API clients (`PsnApi`, `XboxApi`) seeded with a credential grabbed from the WebView. Connector JS is best-effort and needs live tuning per store.

### UI
- Compose Multiplatform Material 3 Expressive. Theme in `ui/theme/` (MaterialKolor dynamic color). Icons are generated at build time by **SymbolCraft** (Material Symbols + external SVG sets) — see the `symbolCraft { }` block in `sharedUI/build.gradle.kts`; add icon names there, don't hand-write icon code.
- Screens live in `ui/views/<feature>/`, shared widgets in `ui/components/`. Localized strings via Compose resources (`Res.string.*`); available languages are auto-derived from `composeResources/values-*` dirs.

#### UI conventions
- Prefer Segmented Lists to plain ones

# IDE
If the AgentBridge MCP exists and is connected, you are running inside an IntelliJ IDEA plugin with IDE tools accessible via MCP. Follow the following best practices:

## BEST PRACTICES

1. **TRUST TOOL OUTPUTS.** MCP tools return data directly. Don't read temp files or invent processing tools.

2. **WORKSPACE.** For temporary files, notes, and plans use `create_scratch_file` — it lives in the IDE scratch area and does not pollute the project. NEVER write to `/tmp/`, the home directory, or outside the project.

3. **MULTIPLE SEQUENTIAL EDITS.** Set `auto_format_and_optimize_imports=false` to prevent reformatting between edits. After all edits, call `format_code` and `optimize_imports` ONCE. `auto_format_and_optimize_imports` includes `optimize_imports` which REMOVES imports it considers unused — if you add imports in one edit and code using them later, combine them in ONE edit or set the flag to false. If auto-format damages a file, use `undo` to revert (each write+format = 2 undo steps).

4. **BEFORE EDITING UNFAMILIAR FILES.** If `edit_text` fails on an `old_str` match, call `format_code` first to normalize whitespace, then re-read.

5. **GIT.** Use the `git_*` tools exclusively. NEVER use `run_command` (or any shell) for git — shell git bypasses the IDE's VCS layer and causes editor buffer desync.

6. **FILE REFERENCES.** Use `FileName.ext:123-456` (colon format) — it creates clickable links in the UI. Don't say "lines 123-456".

7. **GRAMMAR FIXES.** `GrazieInspection` does not support `apply_quickfix` — use `edit_text` (or `write_file`) instead.

8. **VERIFICATION HIERARCHY** (use the lightest tool that suffices):
   a) Auto-highlights returned from a write — after EACH edit. Instant.
   b) `get_compilation_errors` — after editing multiple files.
   c) `build_project` — full incremental compilation. If "Build already in progress", wait and retry.
