# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## What this is

Gamerlogue is a Kotlin Multiplatform + Compose Multiplatform game-library tracker. Targets: **Android** (`androidApp`), **JVM desktop** (`desktopApp`), and **JS browser** (`webApp`). All UI and logic live in the shared `:sharedUI` module; the platform modules are thin launchers. iOS targets are configured (Apple frameworks) but there is no `iosApp`.

## Build & run

`JAVA_HOME` is usually unset — run Gradle through PowerShell using the Android Studio JBR. The wrapper is `./gradlew` (`gradlew.bat` on Windows).

- Android APK: `./gradlew :androidApp:assembleDebug` → `androidApp/build/outputs/apk/debug/`
- Desktop: `./gradlew :desktopApp:run` — hot reload: `./gradlew :desktopApp:hotRun --auto`
- Web (JS): `./gradlew :webApp:jsBrowserDevelopmentRun`
- Lint: detekt runs through the IDE plugin against `detekt.yml`; there is no Gradle detekt task. So a green Gradle build does not mean a clean file: when AgentBridge is connected, run `get_problems` on every file you touched (it reports detekt plus the IDE inspections) and fix warnings and weak warnings before finishing. Information-level hints (smart casts, etc.) can stay.
- All tests: `./gradlew :sharedUI:jvmTest`
- Single test class: `./gradlew :sharedUI:jvmTest --tests "it.maicol07.gamerlogue.services.WebResultTest"`

Unit tests run on the JVM (Kotest + JUnit Platform in `sharedUI/src/jvmTest`) and shared logic tests live in `commonTest`; the JS test target is excluded. There are **no golden screenshot tests**: the official Compose Preview Screenshot Testing plugin does not support KMP modules, so goldens would need instrumented Android tests on controlled emulators — don't add a harness that cannot run here.

One version warning is expected and was left unresolved on purpose — do **not** add a `resolutionStrategy` for it:
- Skiko: Sketch 4.6.0 / ZoomImage 1.6.0 declare `0.144.6`, Compose resolves `0.150.1`. Their current releases still declare the old one and the next line is alpha, so a forced downgrade is riskier than the mismatch.

`local.properties` supplies `APP_ENV` (LOCAL/…) via the `buildConfig` plugin as `BuildConfig.APP_ENV`. The backend and IGDB URLs are runtime settings (Settings → Advanced → Server, `AppPreferences.serverUrl`/`igdbApiUrl`): the default is `http://10.0.2.2` on an Android emulator and `https://gamerlogue.maicol07.it` elsewhere, and IGDB follows `<server>/api/igdb` unless overridden. Read them per request — never cache them — since a change applies without a restart; `IgdbClient` is built against a placeholder host that the `igdbBaseUrl` plugin rewrites. SDK levels and `appPackageName` come from `gradle.properties`.

A local backend is reached from the emulator at `http://10.0.2.2`, the emulator default. Since API 36, Local Network Protections make every RFC 1918 destination time out unless the app holds `ACCESS_LOCAL_NETWORK`: the debug manifest declares it and the `grantLocalNetworkAccess` task grants it after each debug install. The web backend needs a plaintext site for that address — `CADDY_HTTP_SERVER_ADDRESS` plus `SSL_MODE: mixed` on the `laravel` service in the `gamerlogue_web` repo — otherwise its Caddy answers `308` to https for any host but its own.

## Release

Releases are cut by CI — publishing one from the GitHub UI triggers nothing (no build, notes or changelog). Run the "Multiplatform Build & Release" workflow with a `version` (and `prerelease` for the Play Store beta track). It commits the git-cliff (`cliff.toml`) `CHANGELOG.md` to the default branch as `docs(changelog)` (a scope left out of the changelog), tags that commit (app versions come from the tag via gitSemVer), creates the release with that version's notes and builds/publishes from the tag. So commit subjects and bodies are the release notes; don't edit `CHANGELOG.md` by hand, and pull after a release.

## Conventions (enforced)

- **detekt**: new constants use Kotlin's `UPPER_SNAKE_CASE`; `constantPattern` still accepts legacy PascalCase constants until they are migrated. Max line length 150. Comments in English only.
- **Kotlin 2.4**: use the current language features where they simplify the code — explicit backing fields (`val x: StateFlow<S> field = MutableStateFlow(…)`), guard conditions in `when`, `kotlin.time` `Clock`/`Instant` — rather than their older workarounds. Don't reach for them where they add nothing.
- **Single-expression functions** whenever the body is one expression, composables included (`@Composable fun Label(…) = Text(…)`). Keep a block body when the expression would not be `Unit` — e.g. a `when` with a `?.let { … }` branch returns `Unit?`, and a composable with a return value loses restart/skipping.
- Backend list endpoints are **page-based** (reject `page[offset]`): the page number goes in a plain `page=N` (spraypaintkt's `page()` sends `page[number]`, silently ignored — use `forEachPage`/`allPages`), the size is fixed at 30 (`page[size]` is ignored) and `meta.totalItems` is always present. JSON:API queries scoped to the user pass a `current_user=true`-style param via the `currentUserEntries()` extension.
- `library_entries` filters (mapped by `LibraryFilterState.applyFilter`): `filter[status]`/`filter[completion_status]` take one value (a list or an unknown value is a 422); booleans are `true`/`false`; dates are compared by day (`yyyy-MM-dd`) with `[gt|gte|lt|lte]` operators, like the `rating`/`played_time` ranges. `sort` takes snake_case attributes (`-updated_at`); unknown ones are silently ignored. Responses carry `start_date`/`end_date` with a time and offset (`2026-01-01T00:00:00+01:00`): read only the date part, since converting to UTC shifts the day.
- **Koin compiler plugin quirks**, both of which fail confusingly:
  - (seen on 1.1.0) It silently refuses to register a `KtorHttpClient` provider in a `@Module` — no diagnostic, the definition is just absent. `AppJsonApiConfig` builds that wrapper itself because of it; retry when the plugin is updated.
  - (1.2.x) A test compilation that calls `startKoin` gets call-site validation but cannot see main's definitions, so every `get<T>()` in tests fails with a false `KOIN-D002` ([koin-compiler-plugin#58](https://github.com/InsertKoinIO/koin-compiler-plugin/issues/58)). `sharedUI/build.gradle.kts` rewrites `compileSafety=false` on `compileTestKotlinJvm` only; main keeps the check. Remove it once #58 ships (milestone 1.2.2).

## Architecture

### State & DI
- **Koin** for DI, declared with Koin annotations (`@Module`/`@Single` on `AppModule`, `HttpModule`, and an `expect object PlatformModule` with one `actual` per target) and started by the `KoinApplication` composable in `App.kt`. ViewModels are registered with `viewModel { }` and parameterized factories (`viewModel { (id: Int) -> … }`).
- **`App.kt` must not read the active back stack.** `KoinApplication` rebuilds its configuration on every recomposition (`composeMultiplatformConfiguration` is not remembered), which re-provides the Koin composition locals and invalidates every `koinInject`/`koinViewModel` in the tree. Read current navigation only inside `AppScaffold`, `AppNavDisplay`, or another child restart scope. Keep new global state reads out of `App()` for the same reason. Moving the Koin bootstrap out of `App` was tried and reverted: `KoinApplication` ties the container's start/stop to the composition lifecycle, so a formal `onAbandoned → stopKoin()` edge stays open, but keeping the back stack out of `App()` removes it from the hot path. Reopen only if unexpected `stopKoin()` calls or invalidated injections show up.
- **ViewModels** extend `StateViewModel<S>(initial)` (in `core/`), which wraps a private `MutableStateFlow`. Read via `state`, mutate via `update { copy(...) }`. ViewModels hold **no navigation** — screens pass nav as callbacks.

### Navigation
- **Navigation 3** (`androidx.navigation3`) with **nav3ksp** codegen (`io.github.fopwoc:nav3ksp`, processor on `kspCommonMainMetadata`). Destinations are declared as `@Branch(RootTree::class, metadata = …)` composables beside their screens in `ui/views/<feature>/`; KSP generates `RootNavTree` (one `@Serializable` `NavKey` per branch), `RootNavTreeBuilder` (the entries) and `RootNavTreeLayout` (the serializers module) into `…ui.navigation.rootTree`.
- **Adding a destination = adding a `@Branch` composable.** Its key name is the function name minus the `View` suffix; its key arguments are the function's non-`ViewModel` parameters, so those must be `@Serializable` and **must not** include callbacks. Generated keys carry no default parameter values — construct them fully (`RootNavTree.GameList(null, null, null)`).
- Generated keys can only implement `NavKey`, so per-destination chrome lives in the branch composable: wrap in `ScreenScaffold(title = …)` (skip it for screens drawing their own bar, e.g. game detail). Navigation bar/rail visibility is derived centrally from the active key through `NavKey.showsNavigationSuite`.
- Destinations resolve navigation from `LocalNavigationState`. `rememberAppNavigationState()` creates one saveable stack per top-level destination via `RootNavTreeLayout.rememberTreeBackStack`; `NavigationSuiteScaffold` selects bar or rail from the window size.
- `AppNavDisplay` keeps the **androidx** `NavDisplay` + `rememberDecoratedNavEntries` and only feeds it `entryProvider { with(RootNavTreeBuilder) { buildTree() } }`. Do **not** switch to nav3ksp's own `NavDisplay` proxy: it takes no entry decorators (the per-entry `ViewModelStore` the game list bar depends on), no shared-transition scope, no transition specs and no `onBack`.
- Scene metadata is shared, not per destination: `ListPaneMetadata` / `DetailPaneMetadata` / `DiscoverPaneMetadata` in `ui/navigation/SceneMetadata.kt` (`ListDetailSceneStrategy.listPane()/detailPane()`). nav3ksp resolves `metadata =` by type, so each has to be a top-level object.
- The back stack restores through the generated polymorphic module, not closed polymorphism; `NavBackStackSerializationTest` (jvmTest) pins the round-trip for keys with arguments.
- `AppNavDisplay` keeps saveable-state and ViewModel decorators alive for every top-level stack, displaying only the selected stack. Do not decorate only the selected stack: changing tabs must not dispose another stack's state. UI regression check still pending: set list filters and scroll, switch tabs and return, verify state and ViewModel retention; repeat after state restoration.

### Data layer (two APIs)
- **Gamerlogue backend**: JSON:API via **SprayPaintKT**. Schemas are `@ResourceSchema` interfaces in `data/` (e.g. `LibraryEntrySchema`) annotated with `@Attr`/`@Relation`; KSP generates the concrete models (`LibraryEntry`, `User`). `AppJsonApiConfig` is the `@DefaultInstance`. Native auth uses a 15-minute Laravel Sanctum bearer plus a rotating refresh token obtained through PKCE S256; web auth uses the Laravel session cookie.
- **Auth wiring**: `AuthTokenProvider` (persistence, one `actual` per target: AccountManager / OS keyring + `Preferences` / nothing on web) and `AuthenticationHandler` (login flow) are both Koin singletons from `PlatformModule`; the handler resolves its qualified `AuthHttpClient` off the injected `Scope`, and `PlatformModuleBindingTest` pins that binding because the Koin compiler plugin drops qualified definitions silently. Always sign out through `AuthenticationHandler.logout()`, never `AuthTokenProvider.clearSession()` directly: the web session lives server-side and has to be dropped there too (`POST /logout`).
- **IGDB**: game metadata via `igdbclient` on its own Ktor client (separate so tests can swap just one). Has retry/backoff including explicit 429 handling.
- Wrap network calls in `safeRequest { }` (`core/SafeRequest.kt`): it returns a `kotlin-result` `Result`, rethrows `CancellationException` (including when a client library wrapped it), and reports **every** other failure to the global UI error state — timeouts and deserialization errors included, not just `IgdbException`/`JsonApiException`.
- General API clients share `ktorHttpClientConfig` in `HttpModule`: `HttpTimeout`, plus retry on 5xx, 429 (honouring `Retry-After`) and non-cancellation exceptions. The auth client retries 429 responses and one failed refresh transport, but never retries an exchange transport because authorization codes are one-use. `igdbclient` layers its own retry on top of the general policy.
- **Never install `HttpCache` on a user-authenticated client.** Ktor keys the cache on the URL and ignores `Authorization`, so a response fetched under one token is replayed under the next (`HttpCacheAuthTest` pins this). Only the IGDB client caches, since its credentials are the app's rather than the user's.
- Native session storage is per platform: Android `AccountManager`, desktop the OS credential store via `java-keyring`; both access and refresh expiries are persisted alongside the user id. Refresh is single-flight, starts 60 seconds before access expiry, and overwrites both tokens after every rotation. Web stores no credential in Web Storage and sends cookies with `credentials=include` plus `X-XSRF-TOKEN`.
- Auth deployment blockers: `assetlinks.json` currently returns 404 (runbook in `docs/auth-app-links.md`); production CORS/cookie scope does not yet admit `https://app.gamerlogue.maicol07.it` (`Access-Control-Allow-Origin: *` with no `Allow-Credentials`, cookies missing `Domain=.gamerlogue.maicol07.it`). Until the second is fixed the web cookie flow — including `POST /logout` — cannot be verified end to end.

### Linked services sync (`services/`)
Client-side library/wishlist sync with external stores (Steam, PlayStation, Xbox, GOG, Epic) driven by **WebView automation** — no official store APIs for most. The user logs into the store in a `ServiceWebView`; injected JavaScript reads/writes the authenticated same-origin session and delivers results through a JS bridge (`SyncScripts.wrap`). Each store is one `ServiceConnector` subclass overriding URLs + scripts; connectors are registered as a `Map<ExternalService, ServiceConnector>` in `appModule`. `GameMatcher` maps store refs to IGDB games; `LibrarySync` writes them as `LibraryEntry`s. PSN/Xbox additionally use off-WebView API clients (`PsnApi`, `XboxApi`) seeded with a credential grabbed from the WebView. Connector JS is best-effort and needs live tuning per store.

### UI
- Compose Multiplatform Material 3 Expressive. Theme in `ui/theme/` (MaterialKolor dynamic color). Icons are generated at build time by **SymbolCraft** (Material Symbols + external SVG sets) — see the `symbolCraft { }` block in `sharedUI/build.gradle.kts`; add icon names there, don't hand-write icon code.
- Android launcher icons (Icon Kitchen): each variant has plain `ic_launcher*` names in `androidApp/src/{main,alpha,beta,debug}/res`; the `debug` overlay wins over the flavor. Only `ic_launcher_background` lives in `main` alone and is shared. After regenerating, Icon Kitchen copies the raster foreground into `*_monochrome.png`: rebuild them with `python scripts/gen_monochrome_icons.py <res dirs…>`.
- Screens live in `ui/views/<feature>/`, shared widgets in `ui/components/`. Once a composable is used by more than one feature, move it to `ui/components/` (by domain, e.g. `components/event/`); likewise extensions on a model go to `extensions/` (`extensions/igdb/<Type>.kt` for IGDB models), not beside the first screen that needed them. `ui/components/` and `extensions/` must not import from `ui/views/`. Localized strings via Compose resources (`Res.string.*`); available languages are auto-derived from `composeResources/values-*` dirs.

#### UI conventions
- Prefer Segmented Lists to plain ones
- Design with **Material 3 Expressive** first: emphasized type (`titleLargeEmphasized`, …), `MaterialShapes` for decorative shapes, shape-morphing controls (`ButtonDefaults.shapes()`), `LoadingIndicator`/`ContainedLoadingIndicator` over circular spinners, and `MaterialTheme.motionScheme` specs for animations (they also snap when system animations are off). Keep at most one or two highlights per screen.

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
