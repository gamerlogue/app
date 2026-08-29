# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Gamerlogue is a Kotlin Multiplatform + Compose Multiplatform game-library tracker. Targets: **Android** (`androidApp`), **JVM desktop** (`desktopApp`), and **JS browser** (`webApp`). All UI and logic live in the shared `:sharedUI` module; the platform modules are thin launchers. iOS targets are configured (Apple frameworks) but there is no `iosApp`.

## Build & run

`JAVA_HOME` is usually unset — run Gradle through PowerShell using the Android Studio JBR. The wrapper is `./gradlew` (`gradlew.bat` on Windows).

- Android APK: `./gradlew :androidApp:assembleDebug` → `androidApp/build/outputs/apk/debug/`
- Desktop: `./gradlew :desktopApp:run` — hot reload: `./gradlew :desktopApp:hotRun --auto`
- Web (JS): `./gradlew :webApp:jsBrowserDevelopmentRun`
- Lint: detekt runs through the IDE plugin against `detekt.yml`; there is no Gradle detekt task.
- All tests: `./gradlew :sharedUI:jvmTest`
- Single test class: `./gradlew :sharedUI:jvmTest --tests "it.maicol07.gamerlogue.services.WebResultTest"`

Unit tests run on the JVM only (Kotest + JUnit Platform, in `sharedUI/src/jvmTest`); the JS target is excluded. `commonTest` is for Compose UI tests.

`local.properties` supplies build config via the `buildConfig` plugin: `APP_ENV` (LOCAL/…), `IGDB_API_URL`, `GAMERLOGUE_URL`. These surface as `BuildConfig.*`. SDK levels and `appPackageName` come from `gradle.properties`.

## Conventions (enforced)

- **detekt**: top-level constants use PascalCase, not `UPPER_SNAKE` (`TopLevelPropertyNaming` / `constantPattern`). Max line length 150. Comments in English only.
- Backend list endpoints are **page-based** (reject `page[offset]`); JSON:API queries scoped to the user pass a `current_user=true`-style param via the `currentUserEntries()` extension.
- **Koin compiler plugin (1.0.2) quirks**, both of which fail confusingly:
  - It silently refuses to register a `KtorHttpClient` provider in a `@Module` — no diagnostic, the definition is just absent. `AppJsonApiConfig` builds that wrapper itself because of it; retry when the plugin is updated.
  - Its compile-time checker ignores qualifiers, so `koin.get<T>(named(…))` in test code fails with a false `KOIN-D002`. Resolve off `scopeRegistry.rootScope` instead (needs `@OptIn(KoinInternalApi::class)`).

## Architecture

### State & DI
- **Koin** for DI, declared with Koin annotations (`@Module`/`@Single` on `AppModule`, `HttpModule`, and an `expect object PlatformModule` with one `actual` per target) and started by the `KoinApplication` composable in `App.kt`. ViewModels are registered with `viewModel { }` and parameterized factories (`viewModel { (id: Int) -> … }`).
- **`App.kt` must not read the back stack.** `KoinApplication` rebuilds its configuration on every recomposition (`composeMultiplatformConfiguration` is not remembered), which re-provides the Koin composition locals and invalidates every `koinInject`/`koinViewModel` in the tree. Reading `backStack.last()` in `App()` recomposed it on every push and pop, so the current destination is read where it is used instead: `AppNavigationBar`, the only global chrome that needs it. Keep new global state out of `App()` for the same reason.
- **ViewModels** extend `StateViewModel<S>(initial)` (in `core/`), which wraps a private `MutableStateFlow`. Read via `state`, mutate via `update { copy(...) }`. ViewModels hold **no navigation** — screens pass nav as callbacks.

### Navigation
- **Navigation 3** (`androidx.navigation3`), not the old Compose Navigation. Destinations are `@Serializable` objects/classes in `NavKeys` (top-level `NavKeys.kt`), implementing the sealed `AppNavKey` (`title`, `showBottomBar`). The back stack is created in `App.kt` with `rememberSerializable` and handed down the composition via `LocalNavBackStack` — it is not in DI.
- `AppNavDisplay` registers one entry per key and uses the **adaptive list-detail** scene strategy (`ListDetailSceneStrategy.listPane()/detailPane()`). The `screen<K>{}` helper wraps content in `ScreenScaffold`; use plain `entry<K>{}` for screens that draw their own bar (e.g. game detail).
- `AppNavKey` is sealed, so kotlinx.serialization resolves the back stack through closed polymorphism — there is no `SerializersModule` to register keys in. Overrides of `title`/`showBottomBar` **must** use custom getters (no backing field), or serialization pulls them in and demands a serializer for `StringResource`.
- A new destination needs its key **and** an entry in `NavEntries.kt`; `NavEntriesCoverageTest` (jvmTest) fails the build if one is missing.

### Data layer (two APIs)
- **Gamerlogue backend**: JSON:API via **SprayPaintKT**. Schemas are `@ResourceSchema` interfaces in `data/` (e.g. `LibraryEntrySchema`) annotated with `@Attr`/`@Relation`; KSP generates the concrete models (`LibraryEntry`, `User`). `AppJsonApiConfig` is the `@DefaultInstance`. Auth is bearer-token (Laravel Sanctum) via the `JsonApiHttpClient`-qualified Ktor client in `httpModule`.
- **IGDB**: game metadata via `igdbclient` on its own Ktor client (separate so tests can swap just one). Has retry/backoff including explicit 429 handling.
- Wrap network calls in `safeRequest { }` (`core/SafeRequest.kt`): it returns a `kotlin-result` `Result`, rethrows `CancellationException` (including when a client library wrapped it), and reports **every** other failure to the global UI error state — timeouts and deserialization errors included, not just `IgdbException`/`JsonApiException`.
- All Ktor clients share `ktorHttpClientConfig` in `HttpModule`: `HttpTimeout`, plus retry on 5xx, 429 (honouring `Retry-After`) and non-cancellation exceptions. `igdbclient` layers its own retry on top of this.
- **Never install `HttpCache` on a user-authenticated client.** Ktor keys the cache on the URL and ignores `Authorization`, so a response fetched under one token is replayed under the next (`HttpCacheAuthTest` pins this). Only the IGDB client caches, since its credentials are the app's rather than the user's.
- Session token storage is per platform: Android `AccountManager`, desktop the OS credential store via `java-keyring` (with a one-time migration off the old `java.util.prefs` value), web `localStorage`.

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
