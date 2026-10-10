# Changelog

<a name="0.6.0"></a>
## 0.6.0

> Released on October 10, 2026

### ✨ Features

- [`81f503a`](https://github.com/gamerlogue/app/commit/81f503aeffab95870c19122bfae4e1d9461d6786) 🎉 Initial commit
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a70e1232efbe002a23c38d107f2ad547e220cf8d"><code>a70e123</code></a> 🔁 Replace versioning plugin</summary>

  > Replaces the `com.gladed.androidgitversion` Gradle plugin with `io.github.andreabrighi.android-git-sensitive-semantic-versioning-gradle-plugin`.
  >
  > This change updates the versioning mechanism to use the new plugin for computing the application's `versionCode` and `versionName`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ce3fa1f594a49cd5c9701e410dfbf55f83759c6a"><code>ce3fa1f</code></a> 🌐 Open links in Custom Tabs on Android</summary>

  > Adds the ability to open URLs in a Chrome Custom Tab on Android for a better user experience, instead of launching an external browser. On other platforms, it maintains the default behavior.
  >
  > This is achieved by creating a `UriHandler.openURL()` expect/actual function. The Android implementation uses the `androidx.browser` library and reflection to access the context from `AndroidUriHandler` and launch a `CustomTabsIntent`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/30ab8542eb70a651ffdb15c44e09a1abace09951"><code>30ab854</code></a> <b>desktop:</b> 🖥️ Add Compose for Desktop target</summary>

  > Adds a new Compose for Desktop (`jvm`) target to the Kotlin Multiplatform project.
  >
  > This includes:
  > - A new `main.kt` entry point for the desktop application.
  > - Configuration for native distributions (DMG, MSI, DEB) with package details and application icons.
  > - Addition of desktop-specific dependencies like `compose.desktop.currentOs`, `kotlinx-coroutines-swing`, and `platformtools`.
  > - Implementation of `UriHandler` and a no-op `SystemBarsVisible` for the JVM target.
  > - Integration of the Compose Hot Reload plugin for faster development.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e2af5723ff19644215c4d77ea128517e25a8f9e0"><code>e2af572</code></a> ✨ Snackbar support</summary>

  > Adds a `SnackbarHost` to the main `AppScaffold` to allow for displaying snackbars throughout the application.
  >
  > This is achieved by:
  > - Introducing a `LocalSnackbarHostState` CompositionLocal.
  > - Providing a `SnackbarHostState` instance through this local.
  > - Adding the `SnackbarHost` component to the `Scaffold`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f554080c76aec652176b58b8484c2876bdeb85c0"><code>f554080</code></a> 📱 Hide bottom navigation bar on GameDetail screen</summary>

  > Hides the bottom navigation bar when navigating to the `GameDetail` screen for a more focused view.
  >
  > This is achieved by:
  > - Converting `NavKeyWithMeta` from an `interface` to an `abstract class`.
  > - Adding a `showBottomBar` property to `NavKeyWithMeta`, defaulting to `true`.
  > - Overriding `showBottomBar` to `false` in the `GameDetail` navigation key.
  > - Wrapping the `NavigationBar` in an `AnimatedVisibility` composable that checks this new property to control its visibility with a slide animation.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f80ed68c5aa4dbc8584258ea9c46fcc634c08590"><code>f80ed68</code></a> <b>auth:</b> 🔐 Implement multiplatform authentication and library management</summary>

  > This commit introduces a comprehensive authentication system across Android, Desktop (JVM), and Web (WASM) platforms, along with the foundational features for managing a user's game library.
  >
  > ### Authentication
  > - **Multiplatform Auth Handling**:
  >   - An `expect`/`actual` `AuthTokenProvider` interface is implemented for each platform to securely store and retrieve authentication tokens (Android `AccountManager`, JVM Preferences, WASM in-memory).
  >   - A `rememberAuthenticationHandler` composable provides a platform-specific way to initiate the login flow, which opens the Gamerlogue website for authentication.
  >   - Deep linking on Android and a local callback server on JVM handle the token reception after a successful login.
  > - **Global Auth State**: A common `AuthState` object now holds the user's token and profile information, making it accessible throughout the app.
  > - **Login/Logout Flow**:
  >   - The Library, Calendar, and Profile screens are now protected and will show a `LoginView` if the user is not authenticated.
  >   - A basic logout button has been added to the profile screen.
  > - **API Integration**: The Ktor HTTP client is now configured with an `Auth` plugin to automatically attach the bearer token to all API requests.
  >
  > ### Library & Data
  > - **SpraypaintKT Integration**: Adds `spraypaint-kt` for JSON:API communication with the backend.
  > - **Data Models**: New `UserSchema` and `LibraryEntrySchema` are defined for handling user and library data from the API.
  > - **User Persistence**: The current user's data is now fetched after login and persisted locally using `multiplatform-settings`.
  > - **Library Management**:
  >   - A new `LibraryViewModel` handles fetching and managing the user's library entries.
  >   - The `GameDetailScreen` now displays library status and provides actions to add/edit/remove games from "Playing" or "Backlog".
  >   - A full "Add/Edit to Library" bottom sheet (`AddToLibrarySheet`) has been created, allowing users to modify all details of a library entry, such as status, platforms, dates, rating, and review.
  > - **New UI Components**: Adds several new and reusable UI components, including `ButtonProgress`, `NumericField`, `DatePickerFieldDialog`, and `ConnectedButtonGroup`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4cec7bf719eb93e22018e6b9075070500fd82e0f"><code>4cec7bf</code></a> ⚙️ Implement Settings and app preferences</summary>

  > Introduces a comprehensive Settings section, allowing users to customize appearance, manage linked services, and persist preferences across sessions.
  >
  > **New Features:**
  > - Added **Settings**, **Appearance**, and **Linked Services** screens.
  > - Implemented persistent preference storage using `multiplatform-settings`.
  > - Added support for manual theme selection (System, Light, Dark) and Dynamic Colors (Android 12+).
  > - Introduced per-app language selection with platform-specific display name resolution for Android, JVM, and Web.
  > - Created a Linked Services UI with brand icons for Steam, PlayStation, Xbox, GOG, and Epic Games.
  >
  > **Architecture & Infrastructure:**
  > - Added `SettingsViewModel` to manage preference state and authentication logout.
  > - Updated `AppTheme` to observe and react to persistent theme settings.
  > - Expanded the navigation graph with `NavKeys` for the new settings screens and sub-pages.
  > - Added `appLanguageSettingsOpener` to handle system-level language settings on Android.
  >
  > **Dependency Updates:**
  > - Added `compose-settings` (UI, Extended, Expressive) for settings components.
  > - Added `multiplatform-settings` (Coroutines, Observable) for reactive preference management.
  > - Added `flagpack-compose` and `countries-core` for locale-related UI elements.
  >
  > **Other Changes:**
  > - Expanded `symbolCraft` configuration with new Material symbols and brand logos (Simple Icons, SVGL).
  > - Added a JS test workaround for Okio in the build script.
  > - Configured `AVAILABLE_LANGUAGES` via `BuildConfig` based on project resources.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/32a25fc8df850bf42d914fbe6d02d0c8aa8d34c6"><code>32a25fc</code></a> 👤 Add Profile screen</summary>

  > Introduces the `ProfileScreen` component, which manages the user's profile view and authentication state.
  >
  > - Shows `LoginView` if the user is not authenticated.
  > - Displays the profile content when a valid access token is present.
  > - Adds a "Settings" action button to the global top bar using `LocalTopBarState`.
  > - Integrates with Navigation3 using `NavKey` for screen transitions.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f5432d5f3e83bf807f87e2c701061de2cb1bf948"><code>f5432d5</code></a> 🌐 Provide skeletal AuthenticationHandler for Web</summary>

  > Replaces the `TODO` placeholder in `rememberAuthenticationHandler` with a stub implementation of `AuthenticationHandler` and `AuthTokenProvider` for the Web target.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/15399db6ac1e2c51cdbbb0f166bbf0a731f3c06c"><code>15399db</code></a> 🏗️ Add support for custom actions in TopBar</summary>

  > Updates the `TopBar` component and its associated state to allow for custom composable actions to be displayed in the app bar.
  >
  > - Adds a `customActions` property to `TopBarState` to hold an optional composable lambda.
  > - Updates `TopBar.kt` to invoke `customActions` within the `actions` block of the top app bar.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4512ca8deb31be3d92d74f14d65821240d4e5d5d"><code>4512ca8</code></a> <b>i18n:</b> 🇮🇹 Add Italian localization</summary>

  > Adds localized strings for the Italian (`it`) locale in the shared UI module.
  >
  > **Translations included for:**
  > - **Navigation:** Discover, Library, Calendar, Profile, and Settings.
  > - **Game Metadata:** Extensive lists for regions, genres, and themes.
  > - **Library Management:** Section titles (Backlog, Playing, Completed), completion statuses, and entry editing fields.
  > - **Authentication:** Login/logout flows and status feedback.
  > - **Settings:** Appearance, theme selection, and linked services (Steam, Xbox, PlayStation, etc.).
  > - **Common UI:** Error handling, technical details, and general action buttons.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c2b434aee339d8c3b9edc4fa30f718f89905ccad"><code>c2b434a</code></a> <b>skills:</b> 🤖 Add agent skill documentation and reference guides</summary>

  > Adds a comprehensive set of AI agent skill definitions and associated Android developer reference documentation to the `.agents/` and `.claude/` directories. These files provide contextual guidance and code patterns for modern Android development.
  >
  > **New Skills:**
  > - **Adaptive**: Documentation for `FlexBox`, `Grid`, and `MediaQuery` layouts.
  > - **AppFunctions**: Instructions for feature discovery, implementation, and testing of agent-exposed workflows.
  > - **Edge-to-Edge**: Support for drawing behind system bars and handling window insets.
  > - **Navigation 3**: Migration guides and implementation recipes for animations, bottom sheets, and multi-pane layouts.
  > - **R8 Analyzer**: Heuristic and quantitative analysis for ProGuard keep rules and app size optimization.
  > - **Styles**: Guidance for the experimental Compose Styles API and custom design systems.
  > - **Testing Setup**: Harnesses for unit, behavior UI, and screenshot testing (including Compose Preview and Dropshots).
  > - **Verified Email**: Retrieval of cryptographically verified email addresses via Credential Manager.
  >
  > **Other Changes:**
  > - Adds `skills-lock.json` to track versions and hashes of the installed skill sets.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c88d2492bbf4b1364bb99c63ce8b25b54fe747ba"><code>c88d249</code></a> ✨ Reactivate the GameList screen for discover sections</summary>

  > Rework GameListViewModel/Screen around DiscoverSection with pagination,
  > wire the navigation entry and Discover's "see all", and extract the
  > Navigation 3 host into ui/navigation/AppNavDisplay so App.kt is bootstrap only.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/0d35b1d79483926163d1dc9422db611c43b4ac84"><code>0d35b1d</code></a> ✨ Implement shared element transitions for game images</summary>

  > Implements shared element transitions for game covers and hero banners when navigating from lists to the detail screen. This provides a seamless visual morphing effect for images across screen transitions.
  >
  > **Shared Transitions:**
  > - Added a `sharedGameElement` modifier to handle `sharedElement` registration within the navigation scope.
  > - Applied shared element keys to `CoverImage`, `Artwork.Image`, and `Screenshot.Image`.
  > - Updated `DiscoverScreen` and `GameHeader` to use consistent shared keys for banner images.
  >
  > **Navigation & Handoff:**
  > - Introduced `GameHandoff`, an in-memory utility to pass `Game` data between screens during navigation. This allows the detail screen to render the transition target instantly without waiting for a network request.
  > - Updated `GameDetailViewModel` to seed its initial state from `GameHandoff`.
  > - Configured `NavDisplay` with `LocalSharedTransitionScope` and custom fade transition specifications.
  >
  > **UI Adjustments:**
  > - Modified `GameDetailScreen` to prioritize rendering the game header if partial data is available from the handoff, even while full details are still loading.
  > - Wrapped navigation entries in a `CompositionLocalProvider` to expose the `SharedTransitionScope`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/13a7348f54a5500e06d6d562b50621469dbf9461"><code>13a7348</code></a> ✨ Stream library entries with page-based pagination</summary>

  > Load library entries page by page (forEachPage/allPages) and merge them
  > into the UI as they arrive, instead of a single capped request. Switch the
  > JSON:API pagination strategy to PAGE_BASED to match the backend, which was
  > ignoring offset params and capping results at 10.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/b5b94ccd5ec7647357b221258e3567f5b1f01d00"><code>b5b94cc</code></a> ✨ Add client-side store sync engine for linked services</summary>

  > Connect Steam/PSN/Xbox/GOG/Epic and sync owned library + wishlist entirely
  > client-side (no backend, no API keys): the user logs into the official store
  > in a WebView and injected same-origin JS reads/writes the data.
  >
  > - ServiceConnector: one abstract per-service object holding identity, IGDB
  >   mapping metadata and the WebView automation recipe; connectors/* override
  >   only URLs + scripts. Per-operation API-vs-JS path (PSN reads owned via the
  >   trophy API, the rest via JS).
  > - GameMatcher: maps store games to IGDB via external_games + websites URL,
  >   with a throttled multiquery name fallback (429-safe).
  > - LibrarySync: importOwned/importWishlist, pullWishlist and the outgoing
  >   push computation (backlog -> store wishlist), platform-family aware.
  > - PsnApi: npsso -> token -> trophy titles (covers PS3/Vita/disc games).
  > - isServiceSyncSupported expect/actual (disabled on web).

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/69a35d3632dd52218b84642fc7be516c72ee5a28"><code>69a35d3</code></a> ✨ Add linked services screen with import &amp; wishlist previews</summary>

  > Wire the Linked Services UI on top of the sync engine.
  >
  > - ServiceWebView: drives one store automation flow in a WebView (shown only
  >   for login, then runs injected scripts via a JS bridge so reads work on
  >   desktop/CEF too) and hosts the outgoing wishlist push checklist, with
  >   matched / unmatched / off-platform rows.
  > - Library import: manual action opening an editable IGDB-mapping preview
  >   (LibraryImportViewModel/Screen, OWNED/WISHLIST modes), streaming matches
  >   with a progress bar.
  > - Wishlist: per-service toggle (automatic background sync) plus a manual
  >   sync-now action previewing both directions.
  > - Navigation, DI factories and strings for the above; sync icon in build.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/436cae61c9f5b094b66066909806b3b1e2bd4afd"><code>436cae6</code></a> 🤖 Implement Android platform version retrieval</summary>

  > Adds the platform-specific implementation for retrieving the Android SDK version in the `sharedUI` module.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/07c82a5ed2262410080f481e615712ef53680d2b"><code>07c82a5</code></a> 🎮 Implement Xbox Live API integration and enhance wishlist syncing</summary>

  > Replaces the scaffolded Xbox connector with a robust implementation that uses the Xbox Live API to fetch owned titles and improved DOM scraping for Microsoft Store wishlists.
  >
  > **Xbox Live API & Authentication:**
  > - Introduced `XboxApi` to handle the Xbox Live authentication chain (MSA `access_token` → User Token → XSTS Token).
  > - Implements `ownedGames` fetching via the Xbox Title Hub title history service.
  > - Configured a dedicated `HttpClient` in `HttpModule` for Xbox API requests with logging.
  >
  > **Xbox Connector Improvements:**
  > - Updated `XboxConnector` to use the new `XboxApi` for retrieving owned games.
  > - Implemented `credentialStep` to extract MSA tokens from the `login.live.com` redirect fragment.
  > - Enhanced `readWishlist` with a polling mechanism to wait for React-rendered product anchors on the Microsoft Store.
  > - Implemented `wishlistPushStep` with retry logic and state verification to handle asynchronous UI updates when adding games to the wishlist.
  >
  > **WebView & Infrastructure:**
  > - Added `storeLoginUrl` to `ServiceConnector` to handle services where the authentication provider and the store live on different origins.
  > - Updated `ServiceWebView` and `WebSession` to support an "await store login" flow, displaying a "Continue" button when manual interaction is required to set first-party cookies on a secondary domain.
  > - Refactored `LinkedServicesViewModel` to trigger the secondary store login flow during wishlist synchronization and previews.
  >
  > **Dependency Injection:**
  > - Updated `AppModule` to provide the `XboxApi` dependency to `XboxConnector`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2bed5dfd1a242208d62b8c2437acde61c4903b68"><code>2bed5df</code></a> 🎮 Improve GOG synchronization and library matching</summary>

  > Enhances the GOG service connector to support full library synchronization and improves game matching logic for services with slug-based URLs.
  >
  > **GOG Connector:**
  > - **Pagination support:** Updated `readOwned` and `readWishlist` to iterate through all result pages, ensuring large libraries are fully synchronized.
  > - **Request headers:** Added `X-Requested-With: XMLHttpRequest` to fetch calls to match site behavior.
  > - **URL recognition:** Implemented `uidFromUrl` using a regex to extract game slugs from GOG store URLs, enabling better matching with IGDB website metadata.
  >
  > **Matching Logic:**
  > - **UID-based matching:** Introduced `idMatchesUid` in `ServiceConnector` to support services like GOG where the store ID matches the IGDB `external_games.uid` directly, rather than a predictable URL template.
  > - **GameMatcher updates:**
  >     - Added a `matchByUid` routine to perform bulk IGDB lookups using numeric UIDs.
  >     - Included the `uid` field in external game metadata queries.
  >
  > **Base Service:**
  > - Added `idMatchesUid` property to `ServiceConnector` to allow sub-classes to opt into UID-based IGDB matching.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e8ef798fa7c6e79b528994f1a7e0846797a973bb"><code>e8ef798</code></a> 📋 Implement clipboard copy for exception details</summary>

  > Introduces a platform-agnostic way to copy text to the system clipboard and enables the "Copy" action within the global exception bottom sheet.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d1a3b34216c51d5d9c208edde0ab246ad91376b1"><code>d1a3b34</code></a> ✨ Auto-dismiss exception bottom sheet on successful request</summary>

  > Implements logic to automatically close the error bottom sheet when a request succeeds, improving the user experience during retries.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/33eb2512b2fec8eaf06e7c8e979d2bc751add6fd"><code>33eb251</code></a> ✨ Implement session clearing on service disconnect</summary>

  > Refactors service connectors to define session-related origins and clears their associated WebView cookies when a service is disconnected. This ensures that subsequent connection attempts start from a logged-out state and improves privacy when unlinking accounts.
  >
  > **Session Management:**
  > - **ServiceConnector**: Introduced a `sessionUrls()` method to identify all host origins that carry authenticated session cookies for a specific service.
  > - **LinkedServicesViewModel**: Integrated `PlatformCookieManager` to remove cookies for all defined session URLs during the `disconnect` flow.
  > - **Error Handling**: Implemented best-effort cookie removal with logging to handle platform-specific limitations (e.g., JCEF or JS browser environments).
  >
  > **Connector Updates:**
  > - **Steam**: Included both `store.steampowered.com` and `steamcommunity.com` to cover the full login session.
  > - **PSN**: Added library, store, and various Sony account origins (`ca.account.sony.com`, `my.account.sony.com`) where SSO cookies are stored.
  > - **Xbox**: Mapped `login.live.com`, Microsoft account, and Xbox/Store origins.
  > - **Epic**: Included the main domain and the store-specific subdomain.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5d5c909cb49acd9c9f7d061e0eb710bb00dfc067"><code>5d5c909</code></a> 👤 Implement user profile syncing for linked services</summary>

  > Introduces the ability to fetch and display account identity information—including usernames, avatars, and profile URLs—for all supported external services (Steam, Xbox, PlayStation, Epic Games, and GOG).
  >
  > **Service Connectors:**
  > - **Web-based (Steam, GOG, Epic):** Added `readProfile` implementations using injected JavaScript to scrape profile metadata directly from the store's DOM or internal APIs.
  > - **API-based (Xbox, PSN):** Implemented `apiProfile` methods to fetch gamertags and avatars using authenticated REST API calls.
  > - **Persistence:** Profiles are now serialized and stored in settings, ensuring account details remain visible across sessions.
  >
  > **UI & Interaction:**
  > - **Linked Services Screen:** Updated the service list items to display the connected account's avatar and username.
  > - **Profile Navigation:** Headers are now clickable, allowing users to open their public profile pages in a browser (where supported).
  > - **Manual Refresh:** Added a refresh button to each connected service to re-sync profile metadata without needing to reconnect.
  > - **Localization:** Added localized strings and a new `refresh` icon for the profile update action.
  >
  > **Technical Refinement:**
  > - **WebView Bridge:** Refactored `ServiceWebView` and `WebSession` to support generic JSON results from the JavaScript bridge, moving beyond only supporting game reference lists.
  > - **Data Model:** Introduced a shared `ServiceProfile` data class to standardize identity data across different platforms.
  > - **Error Handling:** Added logging and `runCatching` blocks for profile fetching to ensure that a failed profile sync doesn't break the overall service connection.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/9e552a549ed22d8809fe53779f8fbe7b810cfd19"><code>9e552a5</code></a> 🎮 Enhance Epic Games Store integration and implement EpicApi</summary>

  > Refactors the Epic Games Store connector to use a dedicated Kotlin-based API client for library synchronization and improves wishlist management through automated browser interactions.
  >
  > **Epic API Integration:**
  > - Introduced `EpicApi` to handle OAuth token exchange, library fetching, and catalog resolution off-WebView, bypassing browser CORS restrictions.
  > - Uses the public launcher client credentials for token exchange and library access.
  > - Implements logic to filter library items, ensuring only games and DLC are imported while excluding software and Unreal Engine assets.
  >
  > **EpicConnector Improvements:**
  > - **Authentication:** Added `loginTriggerScript` to automatically click the "Continue with my account" button and updated `credentialStep` to extract authorization codes from the redirect API.
  > - **Library Sync:** Switched to `apiOwned` and `apiProfile` using the new `EpicApi` for more reliable data fetching.
  > - **Wishlist Management:**
  >     - Updated `readWishlist` to use same-origin GraphQL queries.
  >     - Implemented `wishlistPushStep` to automate adding games to the wishlist by detecting and clicking the bookmark button on product pages.
  > - **URL Handling:** Added `uidFromUrl` to match IGDB website URLs and `normalizePushUrl` to strip locale segments for consistent redirection.
  >
  > **Dependency Injection:**
  > - Updated `AppModule` and `HttpModule` to provide and inject `EpicApi` into the `EpicConnector`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ede1d193e78b3434d6c1e80d817aa1cdafd61a6e"><code>ede1d19</code></a> ✨ add ServiceSync navigation key and ServiceSyncAction enum</summary>

  > Create a new NavKey for store-sync flows that require login/loading with WebView:
  > - ServiceSyncAction enum: CONNECT, REFRESH_PROFILE, SYNC_WISHLIST, PREVIEW_WISHLIST, IMPORT_LIBRARY
  > - ServiceSync(service, action) data class, serializable for save/restore
  > - Register ServiceSync in savedStateConfiguration for persistence
  >
  > This replaces the inline WebView in LinkedServicesScreen with dedicated screen
  > navigation, enabling better UI separation and persistent WebView state in a
  > bottom sheet while the sync is running.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4ac77edf779d6378244158db4aede61e8e8e153a"><code>4ac77ed</code></a> ✨ create ServiceSyncScreen with persistent bottom sheet WebView</summary>

  > New dedicated screen hosting store-sync flows. Features:
  > - BottomSheetScaffold with persistent WebView in sheetContent
  >   - Expanded to full screen when login required (interactive)
  >   - Collapsed to peek height when working (non-interactive via overlay)
  > - Body shows: loading log (SyncPhase), outgoing push checklist, or completion
  > - Supports all sync flows: connect, refresh, wishlist sync, import/preview
  > - Import/preview flows navigate away, then push LibraryImportPreview screen
  >
  > TopAppBar with close button, manual Continue button for second-origin login.
  > Checklist is segmented-list selectable (no checkboxes, expressive M3 styling).

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8878771c12340037962559ef8faf3b7b58d57bc5"><code>8878771</code></a> 🔌 wire ServiceSync entry in AppNavDisplay</summary>

  > Register ServiceSync screen in the navigation display:
  > - Uses plain entry (not screen) because it draws its own chrome (BottomSheetScaffold)
  > - Maps to detailPane for list-detail strategy
  > - onFinish pops the sync screen
  > - navigateToImportPreview pops sync, then pushes LibraryImportPreview
  >
  > LinkedServicesScreen now navigates to sync via:
  >   navigateToSync: (service, action) -> backStack.add(NavKeys.ServiceSync(...))
  >
  > This separates the service list (clean, no inline WebView) from sync flows
  > (dedicated screen with persistent WebView state in bottom sheet).

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c804f6f1f52aefed412928f4a0c35711591347a7"><code>c804f6f</code></a> ✨ convert import preview to segmented selectable list with grouping</summary>

  > Replace flat LazyColumn checkbox list with three segmented-list groups:
  > 1. Da importare (confident & new): selectable rows, select-all controls apply here
  > 2. Da rivedere (no match or low confidence): read-only, tap opens match editor
  > 3. Già presenti: read-only informational
  >
  > Per-row changes:
  > - SegmentedListItem(selected=row.included, onClick=toggle) replaces Checkbox
  >   → Tap row = select/deselect, expressive M3 styling (no checkbox control)
  > - Add edit icon button for every row → opens MatchSearchDialog to change match
  > - Trailing: open-store IconButton + edit IconButton (for both, match editable)
  >
  > Helper composables:
  > - importGroup: renders a group by titleRes, returns early if empty
  > - GroupHeader: title with count
  > - ImportRow: SegmentedListItem with selection logic or edit-on-click

  </details>
- [`641bd2f`](https://github.com/gamerlogue/app/commit/641bd2f20f3e3610482ef6172fb8ab9c9675e371) **services:** ✨ add Nintendo service connector and wishlist resolution support
- <details><summary><a href="https://github.com/gamerlogue/app/commit/51b5b0b9bb47d219428fd34d2e8b48687fd625b2"><code>51b5b0b</code></a> ✨ add Ubisoft Connect integration and search-by-name wishlist write strategy</summary>

  > Add support for Ubisoft Connect as an external service:
  > - `UbisoftApi`: REST/GraphQL client for fetching owned PC game entitlements and resolving details without CORS issues.
  > - `UbisoftConnector`: handles authentication credential extraction from session, profile scraping, owned games loading, and wishlist read/write.
  > - `WishlistWrite.SearchByName`: new wishlist push strategy for services without fixed IGDB store URLs, searching store titles by game name.
  > - IGDB publisher verification: fetch `involved_companies` in `GameMatcher` and check `matchesPublisher` on `ServiceConnector` to prevent pushing unrelated backlog games to unsupported stores.
  >
  > UI & DI changes:
  > - Register `UbisoftApi` in `HttpModule` and `UbisoftConnector` in `AppModule`.
  > - Add Ubisoft Connect icon from simple-icons and label strings with "PC only" platform note badge.
  > - Update `PushChecklist` on `ServiceSyncScreen` to handle push eligibility for search-by-name connectors.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/84b4fe8810213ef0e56ea38f3d658e2a428bdcb5"><code>84b4fe8</code></a> ✨ support multiple game editions and auto-fill platforms on import</summary>

  > Schema & ViewModel changes:
  > - Replace single `editionId: Int?` with `editionsIds: List<Int>` in `LibraryEntrySchema`
  > - Load game editions asynchronously from IGDB in `AddToLibrarySheetViewModel`
  > - Support multi-edition selection (`selectedEditions`) and safely guard legacy entry attribute reads against missing fields
  >
  > UI updates:
  > - Render edition selection in `AddToLibrarySheet` as a horizontal cover list with selection borders and badges
  > - Convert platform selection in `AddToLibrarySheet` from `FlowRow` filter chips to `SegmentedListLayout` with `SegmentedListItem` checkboxes
  > - Add `library__standard_edition` string localization
  >
  > Store import & Platform matching:
  > - Include `platforms.id` and `platforms.platform_family` in `GameMatcher` IGDB queries
  > - Add `ServiceConnector.platformIdsFor(game)` helper mapping IGDB platforms by platform family or PC store allowlist
  > - Pass derived platform IDs in `LibrarySync` (`importOwned` / `importWishlist`) via `quickDraft` to auto-set platforms for newly imported entries without overwriting existing user selections

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8c8ef19e474c8d4d8f5535a86dc7faa9fe23341a"><code>8c8ef19</code></a> <b>platforms:</b> ✨ add vector icons for multiple gaming platforms</summary>

  > - Introduced vector icons for various platforms including PlayStation, Xbox, Windows, and more.
  > - Enhanced the `Platform.Image` composable to utilize vector icons when available, improving UI consistency and performance.
  > - Added support for additional platforms in the brand icons list.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e6a43a642e32fa0d38f4b088384449e52fb37bd7"><code>e6a43a6</code></a> ✨ add section descriptions and supporting texts to AddToLibrarySheet fields</summary>

  > - Add `supportingText` parameter support to `NumericField` composable
  > - Add string resources in English and Italian for library field descriptions (status, completion status, owned, edition, platforms, dates, played time, rating, and review)
  > - Update `AddToLibrarySheet` sections to display titles alongside descriptive secondary text headers
  > - Set supporting text on the played time numeric field and review text field
  > - Increase review text field height from 120.dp to 140.dp

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/6f5b5f23a43e6c34fb50ab1716f8b2b0301b4fb0"><code>6f5b5f2</code></a> ✨ create SingleSelectConnectedButtonGroup and update completion status selection</summary>

  > ConnectedButtonGroup changes:
  > - Add customizable `containerColor` parameter (defaults to `surfaceContainerHighest`)
  > - Remove hardcoded horizontal padding from `FlowRow`
  > - Add `SingleSelectConnectedButtonGroup` helper function for managing single-selection states with optional deselection
  >
  > AddToLibrarySheet changes:
  > - Replace `FilterChip` status flow with `SingleSelectConnectedButtonGroup` using `surfaceVariant` container color

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/1eae4bbc1477a83e6acf162c17b9a76522948f1a"><code>1eae4bb</code></a> ✨ expand GameDetailScreen with rich metadata, media, and related game carousels</summary>

  > - **IGDB Queries**: Expand `GameDetailViewModel` to fetch additional game metadata including age ratings, storylines, videos, websites, languages, game modes, perspectives, multiplayer details, and related game relationships.
  > - **UI & Layout Enhancements**:
  >   - Add sections for age ratings, multiplayer details, keywords, websites/links, and storylines.
  >   - Support video playback links with YouTube preview thumbnails in the media carousel.
  >   - Add horizontal carousels for related games (DLCs, expansions, parent games, remakes/remasters, similar games, and collections).
  >   - Add bottom sheets for alternative names, franchises, and language support details.
  >   - Adapt `GameDetailsList` to a multi-column responsive layout on wider screens.
  > - **Extensions & Assets**:
  >   - Add IGDB enum mappers for `AgeRating`, `GameCategory`, `GameMode`, `GameStatus`, and `PlayerPerspective`.
  >   - Refactor `Locale.getFlag()` to improve language-to-country code resolution and support region codes.
  >   - Include additional social and platform brand icons in `sharedUI/build.gradle.kts`.
  >   - Add localized string resources in English and Italian for all game details.
  > - **Navigation**: Pass `onGameClick` handler from `AppNavDisplay` to allow navigating directly to related games.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7442c9f3087650b19dc12e06a342f2e80948121c"><code>7442c9f</code></a> ✨ add Time to Beat section to game details screen</summary>

  > - Update IGDB request in `GameDetailViewModel` to use `multiquery` for fetching game details and `GameTimeToBeat` data simultaneously
  > - Add `GameTimeToBeatSection` and `TimeToBeatCard` components to display main, completionist, and rushed times in `GameDetailContent`
  > - Add localized strings for Time to Beat categories in English and Italian
  > - Include `schedule` and `timer` material symbols in `sharedUI/build.gradle.kts`
  > - Refactor library entry save and remove operations in `GameDetailViewModel`

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7bd5db79e7ad93db1ffe1939de6b0b2a3d7c8554"><code>7bd5db7</code></a> <b>ui:</b> ✨ let ConnectedButtonGroup show arbitrary leading content</summary>

  > `toggleButtonIcon` can only carry an ImageVector, so options backed by a
  > remote logo had no way to show it. Adds an optional composable slot that
  > takes precedence over the icon; the existing parameter is untouched so
  > the current call sites keep working.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/17b90a0a4e0a5a1101c133ea58374832cf4c7162"><code>17b90a0</code></a> <b>ui:</b> ✨ let a screen replace the ScreenScaffold top bar</summary>

  > Optional `topBar` slot, so a destination can host something other than
  > AppTopBar without dropping out of the shared scaffold.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/9912d766e764df4df3c892a0226186a613ba5836"><code>9912d76</code></a> <b>search:</b> ✨ browse and filter games from a full-screen search bar</summary>

  > Replaces the GameList destination with a search bar hosted in Discover's
  > top bar. Its expanded pane is the paginated cover grid, so "see all" on a
  > carousel and a typed query narrow the same list; NavKeys.GameList and its
  > serializer registration are gone.
  >
  > The bar is a plain SearchBar rather than AppBarWithSearch, which clamps
  > to 720.dp and centres, and every affordance sits inside the input field
  > because ExpandedFullScreenSearchBar renders only that. The expanded pane
  > is its own dialog, outside the SharedTransitionLayout hierarchy, so the
  > shared-element scope is nulled there — leaving it in place crashed with
  > "layouts are not part of the same hierarchy".

  </details>
- [`37fa634`](https://github.com/gamerlogue/app/commit/37fa63479f94ad32568c02e5b4454346a2008448) **ui:** ✨ add cross-platform AppVerticalScrollbar component
- [`87da061`](https://github.com/gamerlogue/app/commit/87da061603fb927d284a5e26964b898c74091a91) **search:** ✨ extract GameSearchButton and GameListSearchBar components
- <details><summary><a href="https://github.com/gamerlogue/app/commit/04cf04c6fb9c30411528fa73823f55749ea73882"><code>04cf04c</code></a> <b>events:</b> ✨ add IGDB events support with upcoming and past event lists</summary>

  > Introduced events as a new feature, including:
  > - Upcoming and past event lists.
  > - Event details with logos, descriptions, live stream links, and networks.
  > - Integration with game list scoped to an event's games.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/42a2cc40af775270add8d4ec132ae3138f9cdca5"><code>42a2cc4</code></a> <b>ui/android:</b> ✨ enable dynamic navigation bar contrast adjustment</summary>

  > - Added `LaunchedEffect` to toggle navigation bar contrast based on bottom bar visibility in `App`.
  > - Updated `AppActivity` to dynamically manage `isNavigationBarContrastEnforced` on supported Android versions.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/51d4411e4cea2c422d4af076e5d1cbb50eca2e20"><code>51d4411</code></a> <b>desktop:</b> ✨ version distributions from git and fix window setup</summary>

  > The desktop package version was pinned to 1.0.0 while Android already
  > derived its version from git tags. Apply the base git-sensitive semantic
  > versioning plugin (the Android fork already depends on it) and feed
  > jpackage the numeric part of the computed version; macOS additionally
  > rejects a major of 0, so 0.x builds ship as 1.x until the first stable
  > tag.
  >
  > Window setup ran inside the composable content lambda, so the minimum
  > size was reassigned on every recomposition. Only setWindowsAdaptiveTitleBar
  > is composable and stays there. The window also had no icon outside the
  > packaged distributions, and the default height exceeded a 1080p screen.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4cb88d5e0a06bfe7ba0c01c2f9e9c7ce2738f5dc"><code>4cb88d5</code></a> ✨ preserve game cover transition during detail loading</summary>

  > - Add `coverImageId` and `gameName` parameters to `NavKeys.GameDetail` to support preview and shared element transitions before full game details load
  > - Remove `GameHandoff` usage in `AppNavDisplay` when navigating to game details
  > - Extract standalone `GameCoverImage` composable that accepts raw image and game IDs instead of requiring a full `Game` object
  > - Add `GameDetailLoadingCover` component to render the cover placeholder in `GameDetailScreen` during loading state

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/398696e56a86d0e1468f806f78f3770873124570"><code>398696e</code></a> ✨ implement debounced search-as-you-type in game list</summary>

  > - Add `onQueryChange` callback to `GameListSearchBar` to support real-time query updates
  > - Improve `GameListSearchBar` internal state management to prevent external query updates from overriding active typing
  > - Implement debouncing in `GameListViewModel.setSearchQuery` to reduce API requests during active typing
  > - Add `GameListViewModel.submitSearchQuery` for immediate search execution on explicit actions (e.g., IME search)
  > - Update navigation entry to wire search-as-you-type and search submission to the ViewModel

  </details>
- [`57843d2`](https://github.com/gamerlogue/app/commit/57843d26133c9f05cf09c770805ca12c1c3c1fb9) **app:** ✨ apply navigation and authentication audit
- <details><summary><a href="https://github.com/gamerlogue/app/commit/617d631a188cf57073923b58deeae3e9a21f6c91"><code>617d631</code></a> <b>navigation:</b> ✨ implement navigation structure with nav3ksp codegen</summary>

  > - Introduced a new navigation system using Navigation 3 and nav3ksp for code generation.
  > - Added `RootTree` and corresponding navigation keys for various screens.
  > - Updated composables to utilize the new navigation structure, ensuring a more modular and maintainable codebase.
  > - Enhanced the navigation experience by integrating `ScreenScaffold` for consistent UI across screens.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4ec5986653e130eff2b4d93ce4c59d4f2c12fdde"><code>4ec5986</code></a> <b>icons:</b> ✨ update app icons and credits across platforms</summary>

  > - Added separate app icons for `dev` build variant.
  > - Updated and optimized web favicon to use `favicon.ico` and removed obsolete favicon sizes.
  > - Integrated proper attribution for icons in README.
  > - Refreshed Android adaptive icons and added new `dev` variant resources.
  > - Updated desktop app icons for Compose and added platform-specific adjustments.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8853d90e326c07779772432fac821335edcf81bd"><code>8853d90</code></a> <b>auth:</b> 🔐 implement rotating refresh tokens and Koin-managed authentication</summary>

  > - Add rotating refresh token support with single-flight refresh and revocation on logout
  > - Introduce NativeAuthenticationHandler, SanctumTokenClient, and multiplatform Pkce helpers
  > - Migrate session persistence to track access and refresh tokens across Android, JVM, and Web
  > - Register AuthenticationHandler in Koin PlatformModule and configure bearer auth in HttpModule
  > - Update Login, Profile, and Settings screens to resolve AuthenticationHandler from Koin
  > - Extract LoopbackAuthServer for desktop login flow
  > - Add PlatformModuleBindingTest and expand AuthenticationHandlerTest

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/fbf739b31227cd8d34e01870eb3dc5e6dce40380"><code>fbf739b</code></a> <b>ui:</b> ✨ add dismiss action to GlobalExceptionBottomSheet</summary>

  > - Introduced "dismiss" action alongside the existing "close" action in the bottom sheet.
  > - Updated styles and button designs to differentiate actions.
  > - Enhanced error handling logic with `hideThen()` functionality for smoother animations.
  > - Localized dismiss button text for English and Italian.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d603e485da66565941882ca4d081986f77c3e4ff"><code>d603e48</code></a> <b>errors:</b> ✨ show every reported failure instead of only the last</summary>

  > report() overwrote whatever was on screen, so a retrying request erased the
  > failure the user was reading. It now appends, capped at ten, and only the
  > first report opens the sheet — later ones do not reopen a sheet that was
  > closed. The state stays null rather than holding an empty list, which keeps
  > "is there something to show" a single null check.
  >
  > In practice the list is the same exception repeated, so identical failures
  > are grouped with a count and the network hint is shown once for the whole
  > list rather than under each entry.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f89a9b4608a734f145d31046282ed87f04022311"><code>f89a9b4</code></a> <b>settings:</b> ✨ apply the in-app language instead of only storing it</summary>

  > LANGUAGE was written by the picker and never read: nothing applied it, and
  > the dialog marked the current selection from Locale.current rather than from
  > the stored value. Compose resources resolve strings against Locale.current,
  > which is platform state and not a composition local, so the override has to
  > be pushed down per platform.
  >
  > applyAppLanguage() does that: Android goes through AppCompatDelegate, which
  > covers both sides of API 33 — forwarding to the system picker above it and
  > applying and persisting the override below, where the system has none. JVM
  > sets the default Locale, keeping the one the OS reported so null can restore
  > it. Web is a no-op; the browser owns the language.
  >
  > That path needs an AppCompatActivity, which in turn refuses to start unless
  > the theme descends from AppCompat, so androidApp moves off the framework
  > Theme.Material parents. The windowBackground tuned against the cold-start
  > flash and NoActionBar are both kept, and the app draws no framework views
  > whose styling would change.
  >
  > App keys the content on the selected language so the tree recomposes and
  > re-reads the strings; the back stacks live above the key and survive.
  > Verified on desktop; the Android path compiles but is untested on a device.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/012c2aa1093e7beb567e95f9d6b183ee0989eab6"><code>012c2aa</code></a> <b>discover:</b> ✨ show failed sections and stop waiting on popularity</summary>

  > Sections now carry an error flag and say so instead of looking empty. A
  > failed popularity request used to fall back to POPULAR's bare query, i.e.
  > arbitrary games under a "Popular" title; it now marks the section failed.
  > An empty id list is skipped rather than sent as `id = ()`, an IGDB syntax
  > error that failed the whole multiquery and every section with it.
  >
  > Sections that do not rank by popularity no longer wait for that round trip:
  > they load in their own multiquery, in parallel.
  >
  > The card title threshold compared carouselItemDrawInfo.size, in pixels,
  > against a bare 200, so the title showed or hid depending on screen density.
  > It is now 100.dp converted with the local density.
  >
  > EventsViewModel takes its page size as a parameter: the Discover preview
  > asks for 20 events instead of the full list's 100.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5de50e5a02e85e58412fdb861e00106047f3097c"><code>5de50e5</code></a> <b>android:</b> ✨ request local network access at launch in debug builds</summary>

  > From API 37 ACCESS_LOCAL_NETWORK is a runtime permission, and without it
  > every request to the backend on 10.0.2.2 times out. The grantLocalNetworkAccess
  > task only runs after `install*Debug`, which Android Studio's Run button
  > skips, so AppActivity now asks for it on debuggable builds. Release builds
  > do not declare the permission and are left alone.
  >
  > Requests fired while the dialog is open still time out; reopening the app
  > after the first grant recovers. The Gradle task stays so Gradle installs and
  > UI tests are not interrupted by the dialog. Verified on an API 37 emulator.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/92a286918b6b5c84f50eaf3f57988c53887ca805"><code>92a2869</code></a> <b>discover:</b> 🎨 redesign discover screen with expressive M3 layout and hero component</summary>

  > - Introduce `ImmersiveHero` pager component with top scrims and page segment indicators
  > - Add `RankedCarousel`, `FeaturedEvent` component, and interactive release date countdown pills
  > - Support custom `InteractionSource` and press-morphing shapes in `EventCard` and `GameCoverCard`
  > - Implement floating search bar over the hero artwork that turns solid when scrolled
  > - Extract carousel and hero layouts into dedicated `DiscoverCarousels` and `DiscoverHero` files
  > - Refactor section headers, loading states, and simplify section labels across localized strings

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/489730c3d62daaec2c5c236ea8d71a953055d65b"><code>489730c</code></a> <b>icons:</b> 🎨 add script to generate monochrome layers from full-color icons</summary>

  > - Introduced a new script to derive adaptive-icon monochrome layers from full-color foreground layers.
  > - This change addresses the issue of Icon Kitchen copying raster foregrounds verbatim into the monochrome layer.
  > - The script utilizes the Pillow library for image processing and provides a method for handling solid and hole colors.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/6a638d4c0d7ef86a77b072df8fcaebbaeab45cbc"><code>6a638d4</code></a> <b>events:</b> ✨ handle error states, retries, and pagination in event views</summary>

  > - Add localized string resources for event load error and retry actions
  > - Add `error` and `pastPageError` state tracking and safe null response handling in `EventsViewModel`
  > - Display error components with retry buttons in `DiscoverScreen` and `EventListScreen`
  > - Freeze epoch timestamp cutoff during `load()` to prevent event shifting during offset pagination
  > - Improve last visible item index computation in `EventListScreen` and `GameListResults`
  > - Render a tonal `Surface` background for missing event logos in `EventThumb`

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/621b2d9c81d8dfada8ed4ce85329e248eca8d5ab"><code>621b2d9</code></a> <b>events:</b> ✨ add event status pills and redesign event list layout</summary>

  > - Introduce EventStatusPill component to show "Live now" status and upcoming event countdown badges
  > - Display status pills on featured events in DiscoverScreen and event items in EventListScreen
  > - Group past events by start year under sticky headers in EventListScreen
  > - Update EventListScreen with expressive section headers, enlarged thumbnails, and refreshed empty and error states
  > - Add localized events__live_now string resources in English and Italian

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ddedaacc0b17c69f900a1621307c876ce36a5f5c"><code>ddedaac</code></a> <b>gamelist:</b> ✨ add active filter chips and redesign filter sheet UI</summary>

  > - Introduce ActiveFilterChips row to display active filters with individual chip removal and full reset
  > - Redesign GameListFilterSheet with card-based section containers and expressive Material 3 styling
  > - Update FilterButton to use IconToggleButton checked states instead of a badge dot
  > - Enhance empty state in GameListResults with expressive icon container and detailed hint message
  > - Support item animations in the results grid and use ContainedLoadingIndicator for initial load state
  > - Add search_off icon and new strings for empty filter states and chip removal actions

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7cf1468c071b679df40f3bb6176284aba9424372"><code>7cf1468</code></a> <b>auth:</b> ✨ add standalone login destination</summary>

  > Login screen reachable via RootNavTree.Login from screens outside the
  > auth-gated tabs; it pops itself once the session is authenticated.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7d5e4c9178c0d22dc4274f50479d7a9ab33093c5"><code>7d5e4c9</code></a> <b>navigation:</b> ✨ keep the navigation rail on game detail</summary>

  > The game detail hides the navigation only with a bottom bar, where its
  > floating toolbar would stack on it; on larger windows the rail stays so
  > list-detail panes keep their navigation.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8163159f2bdedf0f8dbcd5ba41f10876a7bc1de1"><code>8163159</code></a> <b>game:</b> 💄 redesign game detail in Material 3 Expressive style</summary>

  > - Header: taller banner, large centered cover across its edge, emphasized
  >   title, release date and developer, outlined platforms pill with chevron.
  >   Banner and cover open the fullscreen image viewer.
  > - Shared SectionHeader/GameSection with icon in an expressive shape;
  >   section spacing carried by sections so empty ones leave no gap.
  > - Ratings as wavy rings in a primary container card, with one accessible
  >   description per gauge; time to beat as connected tiles.
  > - Read-only info chips instead of no-op assist chips; expandable
  >   description and storyline cards.
  > - Toolbar: larger add/edit button with animated icon, primary toggles
  >   with filled icons when checked.
  > - Websites as connected link buttons instead of toggles.
  > - Expressive loading, error and not-found states.
  > - Simplify multiplayer details (non-null IGDB fields).

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/95f04c4adc70d8cbcc45154b120b763ed412f6d0"><code>95f04c4</code></a> <b>gamelist:</b> ✨ open the game list pre-filtered on a metadata value</summary>

  > GameListView takes a serializable GameListPreset (type, id, name) in its
  > nav key: the view model starts with that filter applied, the name labels
  > the search bar and the filter chip, including platforms outside the
  > popular list.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/75a2c8f08169aad224e3c6b0fe0579637431a77b"><code>75a2c8f</code></a> <b>game:</b> ✨ clickable metadata, IGDB menu and richer image viewer</summary>

  > - Metadata opens the game list filtered on it: genres, themes, modes,
  >   perspectives, keywords (chips), developers, publishers, engines,
  >   franchises (detail rows) and platforms (release dates sheet). Rows with
  >   several values open a chooser sheet, replacing the franchises sheet.
  >   Game type/status stay plain: the list filters on IGDB's deprecated
  >   category/status ids. Collections have no list filter.
  > - Top bar overflow menu to open or edit the game on IGDB, plus a sheet
  >   explaining the data comes from IGDB and can be corrected there.
  > - Image viewer: per-page zoom (a zoomed page no longer leaves the next one
  >   zoomed), position counter, overflow menu to open, copy, share (where
  >   supported) or save the image, arrow keys and Escape, snackbar feedback.
  >   Platform share/save in ImageActions for Android, desktop and web.
  > - Chip rows spaced by the touch target instead of an extra gap.

  </details>
- [`42869bd`](https://github.com/gamerlogue/app/commit/42869bde69b4615362ae9e9ff08f43ee020a3efb) **settings:** ✨ confirm logout with a dialog
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f4f7a484fe5f3fc8be3eeed3e14bacbdb15b5b6e"><code>f4f7a48</code></a> <b>settings:</b> 💄 redesign settings home in Material 3 Expressive style</summary>

  > - Entries grouped under "App" and "Account" headers.
  > - Leading icons in expressive shapes with tonal containers, summaries
  >   under navigation entries and a trailing chevron.
  > - Logout isolated in its own group with an error-tinted icon.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/909eadf2c448151d6cb08dd178dc57b25bc33344"><code>909eadf</code></a> <b>settings:</b> 💄 redesign appearance settings in Material 3 Expressive style</summary>

  > - Leading icons in expressive shapes with tonal containers.
  > - Language row shows the current language as its summary.
  > - Segment shapes follow the visible rows: without device colors the
  >   language row was still shaped as a middle item.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/916d9ef90282d834f0eb46bbb91a6ae3691e6fd4"><code>916d9ef</code></a> <b>settings:</b> 💄 redesign linked services in Material 3 Expressive style</summary>

  > - Services form a top-level segmented list; a connected service's
  >   wishlist and import actions are an indented second-level list that
  >   closes the top-level list, which then reopens below with some spacing.
  > - Rows are segmented list items: the service row connects or opens the
  >   linked profile, the whole wishlist row toggles auto-sync and the import
  >   row navigates with a chevron.
  > - Service logo in an expressive shape that turns primary when connected,
  >   emphasized service name and morphing connect/disconnect buttons.
  > - Disclaimer card with an info icon and larger corners.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/290ab68ff1ecb58b206e4ec18b3981cf92168732"><code>290ab68</code></a> <b>settings:</b> 💄 expressive states for service sync and library import</summary>

  > - Contained loading indicators and an emphasized working title.
  > - Sync error and import completion use the shared StatusMessage.
  > - Wavy matching progress, morphing buttons and shared group headers.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e92371ac3cb9c415143b0b2264219284519d786a"><code>e92371a</code></a> <b>game:</b> 🎨 add content scale option for cover images</summary>

  > - Introduced a `contentScale` parameter to the `CoverImage` composable for better image handling.
  > - Updated related image rendering functions to support the new parameter, enhancing flexibility in image display.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/b6d3db627c8e872e7e64ca6cc9c1591cffec9954"><code>b6d3db6</code></a> <b>server:</b> ✨ make the backend and IGDB URLs runtime preferences</summary>

  > The Gamerlogue and IGDB URLs were compile-time values from local.properties,
  > so switching instance needed an edit and a rebuild. They now live in
  > AppPreferences: the default is http://10.0.2.2 on an Android emulator and
  > the official instance elsewhere, and IGDB follows <server>/api/igdb unless
  > overridden.
  >
  > Every consumer reads the value per request so a change applies at once:
  > AppJsonApiConfig.baseUrl is a getter, the auth handlers take a serverUrl
  > provider, and IgdbClient is built against a placeholder host that the
  > igdbBaseUrl plugin rewrites to the current endpoint.
  >
  > The GAMERLOGUE_URL and IGDB_API_URL build config fields are removed.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/10e1fb6b2aba1c7f9b5b41bdf1ebf35bc9d6fa5f"><code>10e1fb6</code></a> <b>settings:</b> ✨ add advanced server settings screen</summary>

  > Settings → Advanced → Server edits the Gamerlogue instance and the IGDB
  > endpoint (following the server or overridden), behind a warning that it is
  > meant for advanced users. The fields are a draft until saved; "Restore
  > defaults" clears both preferences.
  >
  > Switching instance while signed in asks for confirmation and signs out on
  > the old instance first; if that fails the instance is left unchanged, so
  > old tokens never reach the new server.
  >
  > URLs are checked for an http(s) scheme and host, then probed with a
  > separate client (not the JSON:API one, which drops the session on a 401):
  > the server must publish an OpenAPI document titled "Gamerlogue API", IGDB
  > must answer a minimal games query with a JSON array. The probe is advisory
  > and never blocks saving.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/0d658a9ca070c37fbb4e41a3227f579812717d6e"><code>0d658a9</code></a> <b>library:</b> ✨ redesign the library screen in Material 3 Expressive</summary>

  > Align the screen with the expressive vocabulary the rest of the app already
  > uses (Discover, GameList, event sections):
  >
  > - Hero empty state: the status icon set in a MaterialShapes silhouette on a
  >   tertiary container, with a titleLargeEmphasized message instead of a line
  >   of body text.
  > - The "all" view now renders one section per status, each with an expressive
  >   header (shaped icon, emphasized title, game count) spanning the grid, so the
  >   statuses read by silhouette as well as by label.
  > - Prominent ContainedLoadingIndicator while the first load runs.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/b9735ce495c0b886e28c18582cebaefb138a8042"><code>b9735ce</code></a> <b>gamelist:</b> ✨ scope the game list to a library status</summary>

  > A library status is one more id source for GameList: the user's entries in
  > that status are fetched once and paged client-side, like an event's games,
  > and each card shows the entry's rating and play time. The list therefore
  > gets search, filters and grid columns for free; as in event scope, the
  > time-to-beat filter and the sort order do not apply.
  >
  > GameListResults now takes the card metadata as a slot instead of a
  > DiscoverSection, and the nav key gains a libraryStatus argument (covered
  > by the back stack serialization test).

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a8f99c71a391ff320be7c70782fa897cb10d5df0"><code>a8f99c7</code></a> <b>library:</b> ✨ show the library as Discover-style status sections</summary>

  > Each status gets a section header with its shaped icon, the number of games
  > in it and "see all", which opens the status in GameList; below it, a cover
  > carousel previews the first games. Empty statuses are hidden, and the hero
  > empty state shows only when the whole library is empty.
  >
  > The view model now loads just the first page of each status, in parallel,
  > instead of the whole library, and reads the total from the page meta.
  > This replaces the status filter, the in-grid section headers and the
  > per-status empty messages.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/60d49712924f66aa1ade6b26f58c9a3e4c57bb3a"><code>60d4971</code></a> <b>http:</b> ✨ add logging for HTTP client requests</summary>

  > - Implemented a logging feature for the HTTP client to enhance debugging capabilities.
  > - Configured logging level to show headers only in local builds for security.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/46cfdc48ea3e2796ea853ad6d6f349f09b74a429"><code>46cfdc4</code></a> <b>gamelist:</b> ✨ filter and sort a library status by its entries</summary>

  > In library scope the filter sheet swaps the IGDB sort, which the library
  > list ignores, for the backend's library sort (last updated by default,
  > date added, rating, played time, start and end date) and adds the entry
  > filters: completion status, owned, rating range, played time bounds (two
  > open-ended fields, as play time has no natural maximum) and start/end date
  > spans from a date range picker. The time-to-beat filter is hidden there.
  >
  > LibraryFilterState.applyFilter maps them to filter[...]/sort parameters
  > (dates as yyyy-MM-dd, ranges as [gte]/[lte]); the entries are refetched on
  > every filter change and each one gets a removable chip. The sort direction
  > toggle is shared with the IGDB sort section.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/83774aa9ccfb77aebd311319f2acd56ecc8affe5"><code>83774aa</code></a> <b>gamelist:</b> ✨ group the library filters apart from the game filters</summary>

  > In library scope the filter sheet titles two groups, "Your library" for the
  > entry sort and filters and "Game details" for the IGDB ones, since they act
  > on different data.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/622c73801e0f485ad2bd6527686eb16b2fe2fa44"><code>622c738</code></a> <b>library:</b> ✨ allow only the backlog for unreleased games</summary>

  > A game without a past release date can only be backlogged and cannot be
  > marked as owned, both in the library sheet and in the detail toolbar. An
  > existing entry keeps its current status and owned flag editable.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d6d8c050601fa36e2ab3e3f8ac27491773ad0b4c"><code>d6d8c05</code></a> <b>services:</b> ✨ report what a wishlist sync actually did</summary>

  > The sync result was packed into a "wishlist:added:pushed" string that
  > nothing ever parsed — the screen only compared the field to "error" — so
  > LibrarySync.pullWishlist computed `added` for nobody and the user was never
  > told anything had happened. consumeMessage() had no callers either.
  >
  > UiState.message becomes a typed SyncOutcome (WishlistSynced(added, pushed)
  > or Failed) and the sync screen now ends on a summary when either side
  > changed, still popping straight back when the two libraries already agreed.
  >
  > pushWishlist returns how many games it sent, which is the count the user
  > cares about: the old string reported the push *candidates*, including the
  > ones already wishlisted or off-platform that it then skipped.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5f428357ad7434208c1954ae029b7ea460738ec0"><code>5f42835</code></a> <b>services:</b> 💄 redesign the store sync screen in M3 Expressive</summary>

  > Phase timeline while working, select-all and a connected medium
  > button pair on the push checklist, stat tiles on the wishlist summary,
  > a flexible top bar with the service as subtitle, and a top-aligned
  > busy notice that stays inside the collapsed WebView peek.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c468f117e1ab5e1074fd44f864678103a27b45dd"><code>c468f11</code></a> <b>list:</b> ✨ show only base games, not their editions</summary>

  > Editions are IGDB games with a `version_parent`: lists and the Discover
  > carousels now skip them, leaving them to their base game's page. The
  > library scope keeps them, so entries saved against an edition stay
  > visible.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/797b0acb9e4ccf975ae1bca86e2cfac300c3531a"><code>797b0ac</code></a> <b>list:</b> ✨ hide bundles and minor add-ons from game lists</summary>

  > Bundles, DLCs, packs and updates belong on their game's page; expansions
  > stay since they are played and tracked on their own. Picking a category
  > in the filter sheet lifts the exclusion.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d6b188db81a179bec68014bca7228c7157e1f388"><code>d6b188d</code></a> <b>game:</b> ✨ list a game&#x27;s editions on its page</summary>

  > The detail multiquery also fetches the games whose `version_parent` is
  > this one and shows them in an "Editions" carousel, now that lists no
  > longer surface them.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/3fb291e4f2adbf6679d0f64c460c3f35c2db4624"><code>3fb291e</code></a> <b>library:</b> ✨ add an edition as its base game with that edition selected</summary>

  > Library entries are keyed by the base game: from an edition's page the
  > quick status buttons and the add sheet save the base game's entry with
  > the edition in `editionsIds`, and the page reads that entry.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/aa26fbda14f297544dc5d8ebf516b2b7d87b2e8d"><code>aa26fbd</code></a> <b>game:</b> ✨ show each collection in its own carousel inside a card</summary>

  > The single carousel mixing every collection's games gives way to one
  > tonal card, like the events block on Discover, listing each collection
  > with its name, localized type and games in release order, the current
  > game left out.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4986d9682d1a96a696d7d351269aa2eba5453c22"><code>4986d96</code></a> <b>game:</b> ✨ skip editions, bundles and minor add-ons in collections</summary>

  > Collections list every related release (108 for Assassin's Creed). The
  > excluded game types now live in BUNDLE_OR_ADDON_GAME_TYPES, shared by the
  > list queries and the new client-side Game.isBaseGame check.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/79b7aa260f89823382ec5e7a022a665194aa5137"><code>79b7aa2</code></a> <b>game:</b> ✨ split the links into stores and resources on one row each</summary>

  > Each group is a single connected row; the links that don't fit move to
  > a menu behind a tonal overflow button. ButtonGroup must not get a
  > minimum width: it keeps it while measuring the overflow button and
  > crashes with maxWidth < minWidth.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/939a7540f8412b03f7d6b98a59b94c09e136f4f0"><code>939a754</code></a> <b>game:</b> ✨ leave bare game logos out of the media and the heroes</summary>

  > igdbclient 0.8 does not model artwork_type, but Wire keeps it among the
  > unknown fields, so the type is requested as a raw field and decoded
  > from there. Applies to the detail media and hero and to the Discover
  > popular carousel.

  </details>
- [`f7c8f97`](https://github.com/gamerlogue/app/commit/f7c8f975471227e3e78848ce13fb97dad960d192) **ui:** ✨ overlay the carousel arrows on hover, also on the hero and the ranking
- [`73d4c45`](https://github.com/gamerlogue/app/commit/73d4c4535331a669db6f18cf461e129b64888515) **discover:** ✨ pin the hero section title at the top

### 🐛 Bug Fixes

- <details><summary><a href="https://github.com/gamerlogue/app/commit/ae7fb342f81d7abceedcec2d21324713a9623d14"><code>ae7fb34</code></a> 🔧 Specify anchor type for library sheet dropdown</summary>

  > Updates the `ExposedDropdownMenuBox` implementation in `AddToLibrarySheet.kt` to explicitly define the anchor type. This ensures the dropdown behaves correctly when used with an editable text field.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/58ce4124c7db863627720d40be9b691f1a0de437"><code>58ce412</code></a> 🛠️ Improve PSN wishlist synchronization reliability</summary>

  > Refactors the PlayStation Network (PSN) connector logic to handle React-based rendering delays and enhances the wishlist sync UI to identify games already present on the store.
  >
  > **PSN Connector:**
  > - **Robust Add-to-Wishlist:** Replaced the single-click logic with a retry-and-verify loop. This ensures the action is successful by waiting for React to bind event handlers and verifying the button state changes to "removeFromWishlist".
  > - **Polling for Tiles:** Updated the wishlist reader to poll for product tiles instead of performing a single DOM read, preventing empty results caused by late-rendering elements.
  > - **Visibility Filtering:** Added checks to ignore hidden edition-picker buttons when searching for the wishlist toggle.
  >
  > **Library Sync:**
  > - **Wishlist State Tracking:** Added an `alreadyOnWishlist` flag to `OutgoingGame` to track if a backlog item is already present on the target service.
  > - **Comprehensive Previews:** Modified `computeWishlistPush` to include all backlog games in the sync preview, allowing the UI to show why certain items are excluded from the push (e.g., already wishlisted or off-platform).
  >
  > **UI & Resources:**
  > - **Improved Sync Feedback:** Updated `ServiceWebView` to disable and deselect games already on the service's wishlist, adding a specific status subtitle.
  > - **Localization:** Added `settings__wishlist_already_present` strings in English and Italian.
  >
  > **Logic Adjustments:**
  > - Updated `LinkedServicesViewModel` to filter out games already on the wishlist before initiating a push session.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f6670c4dcb41d94795b383ee0086a07665eaf987"><code>f6670c4</code></a> <b>steam:</b> 🎮 Update wishlist retrieval to use WebAPI</summary>

  > Updates the `SteamConnector` to fetch wishlists using official WebAPI endpoints, as the previous `dynamicstore` method is no longer reliable.
  >
  > **Wishlist Retrieval:**
  > - Replaced the deprecated `dynamicstore/userdata` call with `IWishlistService/GetWishlist` to retrieve app IDs.
  > - Implemented bulk name resolution using `IStoreBrowseService/GetItems`, processing IDs in chunks of 100 to populate game titles.
  >
  > **Authentication & Identity:**
  > - Reuses the store's WebAPI token retrieved from `pointssummary/ajaxgetasyncconfig`.
  > - Decodes the user's `steamid` from the JWT token's `sub` claim to authorize API requests.
  >
  > **Internal Improvements:**
  > - Added debug logging for token acquisition, API response statuses, and item counts.
  > - Improved error handling during token extraction and name resolution phases.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5541b9d2868e88b8a15a12ba5f6969b953dad596"><code>5541b9d</code></a> 🍪 Enable third-party cookies in service WebView for Android</summary>

  > Configures the service-sync WebView to accept third-party cookies on Android. This resolves authentication failures in store OAuth flows (such as PlayStation Network) that require posting across origins, which previously surfaced as CORS errors due to Android's default security settings.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/245e037ee92bbd041fd346d22093a5de5bdb337b"><code>245e037</code></a> 🔗 Improve service synchronization and GOG login detection</summary>

  > Refines the JavaScript bridge initialization and data handling to improve reliability across different service providers, particularly for GOG.
  >
  > **GOG Connector:**
  > - Updated `isLoggedIn` to check for the `openlogin` string in the URL. GOG uses a hash-based login modal (e.g., `#openlogin`) rather than a dedicated `/login` path, which previously caused false positives for authentication status.
  >
  > **JavaScript Bridge & Injection:**
  > - **Polling Initialization:** Replaced the bridge-ready event listener with a polling mechanism (100ms intervals, up to 20s) in the injection script. This ensures the bridge is detected even if the initialization event fired before the script executed or if the bridge is injected late by the site.
  > - **Serialization Fix:** Removed manual `JSON.stringify` calls within the injected script to prevent double-encoding, as the native bridge handles its own serialization.
  >
  > **WebView Integration:**
  > - Updated `ServiceWebView` to register the `RESULT_METHOD` using `JsonElement` instead of a typed `String`. This prevents decoding errors when connectors return complex objects or arrays and ensures data is correctly re-serialized for the session deliverer.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/443d2fe7077d1ed7816a471ddcf2dae0317c36cf"><code>443d2fe</code></a> 🛠️ Fix GOG avatar image loading</summary>

  > Updates the GOG connector to correctly handle avatar URLs that lack file extensions, preventing 404 errors when fetching user profile pictures.
  >
  > **GOG Connector:**
  > - Modified the JavaScript injection to detect bare image hashes in the `userData.json` response.
  > - Appends a `.png` extension to the avatar URL if a standard image format extension (png, jpg, jpeg, or webp) is not already present.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2787b50472f7de3cd874e488a6683ecf7ff513c6"><code>2787b50</code></a> <b>psn:</b> 🎮 Fix profile fetching and secure avatar URLs</summary>

  > Corrects the PSN profile retrieval process to avoid API errors and ensures avatar images load correctly on Android.
  >
  > **PSN API Changes:**
  > - **Account Lookup:** Updated the `profile` method to first fetch the numeric `accountId` from the PlayStation account service, as the profile endpoint rejects the `me` alias with a 400 error.
  > - **Endpoint Constants:** Introduced `MY_ACCOUNT` and `PROFILE_BASE` constants to support the new two-step lookup flow.
  >
  > **UI & Security:**
  > - **HTTPS Enforcement:** Added a transformation to upgrade avatar URLs from `http` to `https`. This resolves issues on Android where cleartext traffic is blocked by default.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/844a5093d53fd3316b4254297ba30cb09fd4bd89"><code>844a509</code></a> 🎮 Update GOG wishlist search query</summary>

  > Updates the query parameters used for fetching the GOG wishlist in `GogConnector`.

  </details>
- [`e77555d`](https://github.com/gamerlogue/app/commit/e77555dfdf45897bface6e3c1821175c3ea057d5) **ui:** 🎨 improve service webview login flow, nested scrolling, and sync screen UX
- <details><summary><a href="https://github.com/gamerlogue/app/commit/1d931ed7c9fc113a38c4c9116058de91e2a35be7"><code>1d931ed</code></a> 🔧 update hourglass icon reference in AddToLibrarySheetSections</summary>

  > - Replaced HourglassTopW500Rounded with HourglassW500Rounded for consistency in icon usage.
  > - Ensured the leading icon in the NumericField reflects the correct design.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/3d60c1d66fbd1365c0338b4c75435892fb51e48c"><code>3d60c1d</code></a> <b>RemoteImage:</b> 🔧 disable result cache policy to avoid NoSuchMethodError</summary>

  > - Updated the image request to disable the result cache policy.
  > - This change addresses a known issue with the Sketch library.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a127cd46a08683184255a161f625a5627e40c973"><code>a127cd4</code></a> <b>igdb:</b> 🐛 emit valid apicalypse from the where builder</summary>

  > Two shapes were rejected by IGDB:
  >
  > - An empty `where { }` produced a bare `where ;`, answered with
  >   400 Syntax Error. The clause is now only emitted when non-empty.
  > - The string operators interpolated the operand unquoted, so
  >   `name ~ *foo*` failed with "Expecting a STRING as input". They now
  >   quote it, which is what makes name lookups work at all.
  >
  > Also adds `raw` for clauses the operators cannot express, such as an
  > OR group.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8b632401c187992813375dc65af9434638f3a817"><code>8b63240</code></a> <b>ui:</b> 🐛 let the parent size a game cover</summary>

  > CoverImage forced 150x200 after applying the caller's modifier, so a
  > grid cell could not narrow it and the overlaid title spilled past the
  > cover instead of being ellipsised. Both CoverImage and GameCoverCard now
  > take a `sizeModifier`, defaulting to the previous intrinsic size.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/86b16d43e1caf4d5c35669996d300e6c4ee1a46c"><code>86b16d4</code></a> <b>i18n:</b> 🔧 escape apostrophes in Italian strings.xml</summary>

  > Escape single quotes/apostrophes in `gamelist__search_companies` and `gamelist__sort_disabled_search` to ensure valid XML resource formatting.

  </details>
- [`25c172a`](https://github.com/gamerlogue/app/commit/25c172a815c453a9864c91dd8b695a92d4c8d1b5) **ui:** 🐛 animate bottom sheet dismissal before triggering onDismiss callback
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7f52d6025fd0f3f2103249d4385c445d5bf958dd"><code>7f52d60</code></a> <b>android:</b> 📱 change AppActivity windowSoftInputMode to adjustResize</summary>

  > Update `android:windowSoftInputMode` from `adjustPan` to `adjustResize` in `AndroidManifest.xml` so the activity resizes when the soft keyboard is displayed.

  </details>
- [`1f0341e`](https://github.com/gamerlogue/app/commit/1f0341e4f3e6499cb0a325b2114ece681b2a637f) **ui:** 🐛 add navigationBarsPadding to GameToolbar for proper bottom offset adjustment
- <details><summary><a href="https://github.com/gamerlogue/app/commit/08a3c40268fe69f28a92859590e2eb04c6cdd3ef"><code>08a3c40</code></a> <b>android:</b> 🔒️ harden the app manifest and isolate debug builds</summary>

  > The manifest and its build config had accumulated several issues that
  > only show up outside a debug run:
  >
  > - the cleartext network security config applied to release builds too;
  >   it now lives in the debug source set, so release cannot opt into
  >   cleartext at all
  > - AUTHENTICATE_ACCOUNTS (removed in API 23) and GET_ACCOUNTS (not
  >   required from API 26 for accounts owned by the app) were declared but
  >   unnecessary; reading accounts through getAccountsByType makes the
  >   permission unneeded by construction
  > - READ_PHONE_STATE and the storage permissions were implied by the
  >   spraypaintkt-ktor-integration AAR, whose manifest declares no target
  >   SDK; they are removed until the library is fixed
  > - manifestPlaceholders["appIcon"] was never referenced, so alpha and
  >   beta shipped the production launcher icon
  > - singleInstance pushed activities started by the app into a separate
  >   task; singleTask delivers onNewIntent just the same
  > - the platform theme had no windowBackground, flashing on cold start
  >
  > Debug builds now use a .dev applicationId so they can sit next to a
  > release install, which in turn requires the AccountManager account type
  > to follow the applicationId instead of being hardcoded.
  >
  > Also documents why the shared composeResources directory is registered
  > as an Android res directory: res/xml/authenticator.xml resolves
  > @string/app_name from it.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ebb9f1aac4298c8ea2ac5eab9d8e699ba39d7d7e"><code>ebb9f1a</code></a> <b>web:</b> 🐛 initialize locales once and complete the PWA metadata</summary>

  > webAppInit ran inside the ComposeViewport content lambda, which is
  > composable, so locale initialization was repeated on every recomposition
  > of the root.
  >
  > The web manifest was missing start_url and scope, which suppresses the
  > install prompt; both are relative because the app is published to a
  > GitHub Pages project subpath. Theme colors now follow the color scheme
  > instead of being pinned to white, and the boot placeholder no longer
  > flashes light on a dark background.
  >
  > Drops the jscanvas experimental flag, a leftover from the pre-ComposeViewport
  > Compose/Web canvas: the js target builds and bundles without it.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a9c60ea98947d07d563bb96ea822c1457c8cc830"><code>a9c60ea</code></a> <b>core:</b> 🐛 let cancellation escape safeRequest</summary>

  > kotlin-result's runCatching catches Throwable, so a CancellationException
  > was turned into an Err and swallowed. The caller then carried on past the
  > point where it should have stopped: in GameListViewModel a load cancelled
  > by a filter change still wrote games, loading and endReached back into the
  > state, clobbering the newer load.
  >
  > Cancellation is now rethrown, and the exhaustive-looking `when` no longer
  > has to double as a cancellation guard.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/46242c98b9bfded85cbb4b9dce610fb3cbd94d60"><code>46242c9</code></a> <b>auth:</b> 🔒️ harden token handling and move session state to StateFlow</summary>

  > The bearer token was reaching the logs from two directions: the Ktor
  > Logging plugin was pinned to HEADERS in every build, which prints the
  > Authorization header, and the auth state logger printed the token itself.
  > Logging now follows APP_ENV and only the authenticated flag is logged.
  >
  > Ktor caches the result of loadTokens until an explicit clearToken(), which
  > nobody called: logging out and back in kept sending the previous session's
  > token. The token is an in-memory read, so caching is simply disabled.
  >
  > Callback parsing dropped any query value containing an '=' — base64
  > padding in a token was enough to lose it. Parsing now splits at the first
  > '=' only, and the user id is decoded like the token.
  >
  > AuthTokenProvider and ExceptionReporter held Compose state despite being
  > DI singletons read from the Ktor client and the sync services; both now
  > expose StateFlow, collected at the UI edge. UserStore joins them in Koin
  > instead of building its own Settings instance.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/85739285604920fa3037e51ace1a283977ee2a6f"><code>8573928</code></a> <b>gamelist:</b> 🐛 drop stale company roles from the filter state</summary>

  > Deriving isActive from the default state made one case behave differently
  > from the old hand-written check: roles were written as `id to emptySet()`
  > and never removed, so toggling a role on and off — or deselecting the
  > company entirely — left `companyRoles` non-empty. The filter then read as
  > active (badge lit, Discover section query abandoned) while
  > companiesClause() added nothing to the query.
  >
  > Roles are now dropped when their set empties or their company is
  > deselected, which is what the empty set already meant.
  >
  > Also types the media carousel over a sealed wrapper instead of Any, and
  > replaces a tautological test that asserted MaxReleaseYear > MinReleaseYear
  > with one that checks it against the clock.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d0036d8e5625ac346079eb57605da32d86b5f43e"><code>d0036d8</code></a> <b>core:</b> 🐛 rethrow wrapped cancellation exceptions in SafeRequest</summary>

  > - Traverse the exception cause chain to find and rethrow `CancellationException`, ensuring wrapped cancellation from client libraries escapes properly rather than being reported as an error.
  > - Add unit test verifying wrapped cancellation exceptions in the cause chain are rethrown.

  </details>
- [`2c88fcd`](https://github.com/gamerlogue/app/commit/2c88fcd73c1460db144bb8f829a2221169cc8874) **sync:** 🐛 rethrow wishlist cancellation
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d7364cf76852017e3baa2e946b2ada687f820c57"><code>d7364cf</code></a> <b>http:</b> 🔒 stop caching responses on user-authenticated clients</summary>

  > Ktor keys HttpCache on the URL and ignores the Authorization header, so a
  > response fetched under one bearer token was replayed under the next one after a
  > logout and re-login. HttpCacheAuthTest reproduces it against a MockEngine.
  >
  > HttpCache now lives only on the IGDB client, whose credentials belong to the app
  > rather than to the user, so a URL-keyed cache cannot leak across accounts.
  >
  > Same file, related hardening of the shared client config:
  > - HttpTimeout on every client; a hung connection used to hang indefinitely.
  > - Retry extended to 429 (honouring Retry-After) and to non-cancellation
  >   exceptions, where it previously covered 5xx only.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/79348f8941fb06eb26abc8e4eec96ef481b60b81"><code>79348f8</code></a> <b>data:</b> 🐛 resolve the JSON:API client per access</summary>

  > AppJsonApiConfig is a data object, so the `by lazy` client outlived the Koin
  > instance it came from: after a stop/restart it kept routing requests through the
  > dead context. Resolving on each access costs a lookup plus a KtorHttpClient
  > wrapper whose constructor only stores the reference it is given.
  >
  > Registering the wrapper in HttpModule would be the better shape, but the Koin
  > compiler plugin (1.0.2) silently declines to register a KtorHttpClient provider
  > under every form tried — interface or concrete return type, binds = [...],
  > @Named or Scope parameter — with no diagnostic. Noted in the source; retry on the
  > next plugin release.
  >
  > HttpModuleBindingTest pins the wiring, which nothing checked at compile time: a
  > wrong qualifier would have surfaced only on the first backend call.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/3c22d288d904f4e3f4f9f3e8b8658f559c0dd293"><code>3c22d28</code></a> <b>core:</b> 🐛 report every failed request, not just two exception types</summary>

  > The `when` had no else branch and was used as a statement, so anything that was
  > not an IgdbException or a JsonApiException — timeouts, IO failures,
  > deserialization errors — was swallowed and never reached ExceptionReporter. It
  > only went to printStackTrace, which is invisible in release while Kermit is
  > already a dependency.
  >
  > Cancellation still escapes, including when a client library wrapped it.
  >
  > Note this is user-visible during a linked-services sync: connector failures that
  > used to disappear now each open the global error sheet.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/747b880f10c4f4df0cf79fda07ddb2d6022c31a7"><code>747b880</code></a> <b>auth:</b> 🔒 keep the desktop session token in the OS credential store</summary>

  > java.util.prefs is a plaintext file or registry key readable by any process
  > running as the user. The bearer token now goes to Windows Credential Manager,
  > the macOS Keychain or the Linux Secret Service through java-keyring. The user id
  > stays in Preferences: it is not a secret, and keeping it there avoids a second
  > keyring round-trip on every start.
  >
  > A token written by the previous implementation is migrated into the keyring and
  > the plaintext copy deleted, so upgrading users are neither signed out nor left
  > with the old value in the clear.
  >
  > Where no credential store is usable the token is kept in memory and the failure
  > is logged: the session stops surviving a restart. It is deliberately not written
  > back to Preferences, since silently downgrading to plaintext would defeat the
  > point of the change.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/91f5bdf84029350f94731650139282dea942bab0"><code>91f5bdf</code></a> <b>auth:</b> 🐛 shut down the loopback login server on every path</summary>

  > The callback listener was stopped only from inside the request handler's finally
  > block, so abandoning a login left the server — and its executor's non-daemon
  > thread — alive for the rest of the process, one more per click on "login".
  >
  > The server and its executor are now tracked together and torn down through a
  > single stopFlow() reached from the callback, from every error path, and from a
  > five-minute watchdog for the browser that never comes back. Starting a login
  > also replaces any flow already in flight instead of stacking on it, and both
  > thread pools are daemon so a pending timeout cannot hold the JVM open.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e8f13a279a4b5ac65d0e077427db3ed5414481ce"><code>e8f13a2</code></a> <b>auth:</b> 🔒 open the Android login flow in a Custom Tab</summary>

  > ACTION_VIEW handed the authentication URL to whatever app happened to claim it.
  > A Custom Tab keeps the flow in a browser the user can inspect — URL bar, real
  > certificate state — and shares the browser's cookie jar, falling back to the
  > default browser when no provider supports it.
  >
  > androidx.browser was already on the classpath, so this adds no dependency.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/beb5de70a214761e91555e82b95653f7811abf11"><code>beb5de7</code></a> <b>auth:</b> 🐛 remove the account on logout instead of invalidating the token</summary>

  > invalidateAuthToken only drops a token from the cache — it means "this token is
  > stale", not "forget it" — and it no-ops when peekAuthToken returns null, so a
  > logout could leave the token in place. Removing the account is unambiguous; the
  > next login recreates it through getOrCreateAccount.
  >
  > saveUserId(null) no longer goes through getOrCreateAccount either: logout clears
  > the token first, which removes the account, and recreating an empty one there
  > would leave a stray account behind.

  </details>
- [`e7ac5bc`](https://github.com/gamerlogue/app/commit/e7ac5bce0c4e6a166e55137add4f5e27d283dcb1) **android:** 🐛 handle legacy callback URI scheme in AppActivity
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d844e75efd269063a2234ed7a495f601ed7aba53"><code>d844e75</code></a> <b>theme:</b> 🐛 make the dynamic colors switch do something</summary>

  > USE_DYNAMIC_COLORS was written by the Appearance screen and read back only
  > to populate the switch itself: Theme.kt never consulted it, so the seed was
  > always the brand color and the toggle did nothing.
  >
  > Adds deviceSeedColor(), an expect that returns the system palette seed where
  > one exists. Android reads dynamicLightColorScheme().primary; JVM and web have
  > no system palette and return null, falling back to the brand seed.
  >
  > The switch has always displayed itself as on by default, so Android 12+ users
  > who never touched it will see a wallpaper-derived palette from now on.
  >
  > Known limitation: the Monet role fed back into MaterialKolor's TonalSpot
  > generator tracks the wallpaper but does not reproduce the system palette, and
  > the light scheme is read even in dark. Untested on a device.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a148f410acc66d713d1765371ae0f603bb163ece"><code>a148f41</code></a> <b>navigation:</b> 🐛 stop keying the navigation state on a rebuilt map</summary>

  > remember(selectedRootIndex, backStacks) keyed on a map reallocated on every
  > composition. It is latent today because App() does not recompose, but a new
  > AppNavigationState handed to a staticCompositionLocalOf invalidates the whole
  > tree — precisely what App's KDoc says it avoids. The back stacks are already
  > remembered individually, so the wrapper needs no key at all.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a2eeb076f7f5f5cf4635110154d30aec23948ab3"><code>a2eeb07</code></a> <b>games:</b> 🐛 keep popular games in popularity order</summary>

  > The popularity ids came back ranked, but the games query filtered on them
  > with `id = (...)` and no sort, and IGDB ignores the order of that list: the
  > Popular carousel and its full list showed the right games in no particular
  > order. sortedByIds restores the order of the id source.
  >
  > GameList applies it to every id-driven page, so the time-to-beat filter now
  > follows its upstream ranking (most submitted times first) and events follow
  > the order of the event's games, both previously undefined.

  </details>
- [`89938d7`](https://github.com/gamerlogue/app/commit/89938d74f75adf498b647546e15c957b65e35de1) **i18n:** 🌐 mark deep link configuration and exception placeholder strings as non-translatable
- [`16d4214`](https://github.com/gamerlogue/app/commit/16d421468792c44b4f069f626f06dda2c5664001) 🐛 correct and simplify game detail rendering
- [`14f62fc`](https://github.com/gamerlogue/app/commit/14f62fc55b0a5038e66f468e8582a67eb88c8d59) **resources:** 🐛 remove unnecessary quote escaping in Compose string resources
- <details><summary><a href="https://github.com/gamerlogue/app/commit/604d920886fa93258e1d0b0e68cf341da4c454e8"><code>604d920</code></a> <b>game:</b> 🐛 handle signed-out users and load errors in game detail</summary>

  > - Load the library entry only when signed in, following session changes;
  >   library actions route to the login screen when signed out, and the
  >   library sheet closes on logout.
  > - Keep the known library entry when a reload fails instead of clearing it.
  > - Show a load error with retry, distinct from "game not found".
  > - Center the "not found" message.
  > - Replace LocalGameTopBarOverlayMode with an explicit parameter and
  >   callback; merge the screen branches into a single `when`.
  > - Track the in-flight toggle with a single pendingStatus; simplify the
  >   GameToolbar API.
  > - Inject the ViewModel in the @Branch body, as nav3ksp turns parameters
  >   into nav key properties.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/90b7a2a27ff55aca150e9507f3fc923fb6fe0893"><code>90b7a2a</code></a> <b>ui:</b> 🐛 handle single item shapes in ConnectedButtonGroup</summary>

  > - Fall back to standard fully rounded shapes when `ConnectedButtonGroup` contains only one option

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/05238e35780597ca05d59f81084e1c9cb5591522"><code>05238e3</code></a> <b>build:</b> 🐛 update mdi icon URL to avoid redirect issues</summary>

  > - Changed the URL template for mdi icons to a pinned version to prevent 302 redirects that SymbolCraft does not handle.

  </details>
- [`bce6126`](https://github.com/gamerlogue/app/commit/bce6126f7671a951c70a1eb37b679e6cc9a4785c) **strings:** 🐛 add pluralization for error and day counts in Italian localization
- <details><summary><a href="https://github.com/gamerlogue/app/commit/899ec2a74db76665d5f789e1d9fa496f27580f46"><code>899ec2a</code></a> <b>deps:</b> 🐛 use Material 1.13 compatible Compose Settings fork</summary>

  > Use the temporary GitHub Packages release and authenticate dependency resolution in CI. Align Compose Multiplatform with Material and document the upstream migration path.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f715f3cc4af4d9bd6117fe3bdc763b704eb614be"><code>f715f3c</code></a> <b>library:</b> 🐛 send the page number the backend actually reads</summary>

  > spraypaintkt's page() sends page[number], which the backend silently ignores
  > and answers with the first page again: forEachPage then stopped on the
  > repeated ids, so allPages() returned only the first 30 entries (library sync
  > included). Send the plain page=N parameter instead.
  >
  > meta.totalItems was also never read, since meta holds JsonElements rather
  > than Numbers; it is now exposed as CollectionProxy.totalItems.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c81e9204f08fe6d917b2a483ec29f04a30ba52d3"><code>c81e920</code></a> <b>library:</b> 🐛 handle library dates as calendar days</summary>

  > The backend sends start_date/end_date with a time and offset
  > (2026-01-01T00:00:00+01:00); parsing them as an Instant and showing them in
  > the picker's UTC landed on the day before. They are now read as the
  > LocalDate of their date part and sent back as yyyy-MM-dd.
  >
  > The date field converts through UTC on both ways, as Material date pickers
  > expect, instead of the system zone; the end date field also received
  > seconds where the picker wants milliseconds.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/343ffaf284a1fd8a199c786524afa421add14277"><code>343ffaf</code></a> <b>discover:</b> 🐛 stack two-digit ranks in the most loved carousel</summary>

  > The strip left of each cover fits a single digit, so from 10 on the
  > second digit slid under the cover.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7a187fe9eb01ddce30e4d1dc6815fd4c363793fd"><code>7a187fe</code></a> <b>discover:</b> 🐛 keep &quot;see all&quot; at the end of section headers</summary>

  > The title and the spacer were both weighted, so the spacer only got half
  > of the free space and short titles left the button short of the edge.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/950fba304621b6658415f0d9a39efbac03c3a0ad"><code>950fba3</code></a> <b>user:</b> 🐛 align resource type and migrate cached profiles</summary>

  > Match the backend User resource type and migrate cached lowercase user profiles on restore. Cover API deserialization, cache migration and rejection of unrelated resource types.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/401970e225caaee5f1a8775eac075293f34ca047"><code>401970e</code></a> <b>services:</b> 🐛 fail the store API calls on an HTTP error</summary>

  > The four off-WebView store clients had no response validation, so a 401 or
  > 403 body was handed straight to the JSON parser and read as "no games" —
  > an expired store session looked like an empty library. UbisoftApi worked
  > around it with three runCatching + "unexpected response" logs, while PsnApi
  > and XboxApi threw from requireString: the same failure, handled two ways.
  >
  > The four identical client builders collapse into one storeApiClient() that
  > rejects 4xx/5xx. Not expectSuccess, which also rejects 3xx: the PSN
  > authorize step reads its auth code out of a 302 Location.
  >
  > UbisoftApi drops the workarounds and lets the failures propagate to the one
  > place that already logs them (ServiceWebView's DataSource.Api read). Its
  > session renewal keeps its soft failure on purpose — the captured session is
  > usually still valid — and the GraphQL call keeps its own body check, since
  > GraphQL reports a rejected query with a 200.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/6c63f71648df6304038d221b5628d7c73eebd0bf"><code>6c63f71</code></a> <b>android:</b> 🔇 drop the stale permission overrides from the manifest</summary>

  > spraypaintkt-ktor-integration no longer makes the merger imply READ_PHONE_STATE and
  > the storage permissions, so the remove/replace markers matched nothing and warned.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/6f0f0585f7dbc1b373fc2c423c0da2f8ecb801ce"><code>6f0f058</code></a> <b>game:</b> 🐛 read age ratings from the non-deprecated IGDB fields</summary>

  > category/rating are deprecated in favour of organization and rating_category.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a7c57d401576f3fd70aa911b6ebf3463955a8ffc"><code>a7c57d4</code></a> 🐛 treat empty IGDB strings as missing</summary>

  > Wire models default absent strings to "", never null, so the elvis fallbacks never
  > fired: editions without a version title got a blank label instead of their name.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2b774e4ef2cd6ff926dd1a96ac8b9d951ff02106"><code>2b774e4</code></a> <b>list:</b> 🐛 filter categories by game_type</summary>

  > IGDB no longer fills the deprecated `category` field, so the category
  > filter matched nothing. `game_type` ids equal the old category values.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ecb63d8b7dc47ac46ceefdb900b7d2b5c93c1954"><code>ecb63d8</code></a> <b>game:</b> 🐛 show the badge of age ratings that have no cover</summary>

  > IGDB leaves rating_cover_url empty for most ratings, so every chip fell
  > back to the info icon. Use the badge igdb.com serves for the
  > organization and rating instead.

  </details>
- [`5b86d35`](https://github.com/gamerlogue/app/commit/5b86d3589ccf4951c1d650917fe72b138bf44fde) **ui:** 🐛 let the mouse wheel scroll the page over carousels
- [`d0ec83d`](https://github.com/gamerlogue/app/commit/d0ec83d4b9314fec6ba109cd4dfb1c1bc0233794) **discover:** 🐛 align the upcoming date pill with the other badges
- [`0fb33c2`](https://github.com/gamerlogue/app/commit/0fb33c2a1ca8b2434b2a02d99c87b14d9c6d28a6) **ui:** 🐛 keep a mouse click from scrolling the carousel instead of opening the game
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d17274f3fec8fce66535f9db061b379b81874d3c"><code>d17274f</code></a> 🐛 make gradlew executable</summary>

  > The automatic dependency submission job runs ./gradlew without a chmod
  > step and failed with "Permission denied", falling back to the runner's
  > system Gradle.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c611e1e74a522e2af4e7a90864aa3a021694facd"><code>c611e1e</code></a> 🐛 make local.properties optional in sharedUI build</summary>

  > GitHub's automatic dependency submission doesn't create local.properties,
  > so configuring sharedUI failed. All values read from it already have
  > defaults.

  </details>

### ⚡ Performance Improvements

- <details><summary><a href="https://github.com/gamerlogue/app/commit/0eaaa2c784ddbcaade90f743372d3624a85668fb"><code>0eaaa2c</code></a> <b>sync:</b> ⚡️ read the library once per pull and save imports concurrently</summary>

  > pullWishlist fetched the whole paginated library three times: once inside
  > importWishlist and twice more for the push preview. The pre-import read is
  > now passed down, leaving one read before the import and one after it — the
  > second is still needed, because the preview must also list the entries the
  > import just created.
  >
  > persistEach awaited each save before starting the next one, so importing a
  > few hundred games was a few hundred serial round-trips. Saves now run
  > through a small semaphore instead.
  >
  > Also picks up the StateFlow session accessor from the auth change.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/005f7e9c9b0bf7c18874a6b79c054c119848e558"><code>005f7e9</code></a> <b>ui:</b> ⚡ read the current destination in the bottom bar only</summary>

  > App() read backStack.last(), so every push and pop recomposed it and, with it,
  > KoinApplication. That composable rebuilds its configuration on each recomposition
  > (composeMultiplatformConfiguration is not remembered), which re-provides both
  > Koin composition locals and invalidates every koinInject / koinViewModel in the
  > tree. The current destination is now read where it is needed — AppNavigationBar,
  > the only global chrome that depends on it — so a navigation invalidates the
  > bottom bar rather than the whole shell. AppScaffold no longer takes a nav key,
  > and NavigationBarContrastEnforced moved next to the bar whose visibility it
  > tracks.
  >
  > These changes ship together because they are interlocked: App.kt carries three of
  > them, and the AppScaffold signature ties the scaffold, the bottom bar and the nav
  > layer to the same compilable state.
  >
  > Also in this set:
  > - ExceptionReporter collapses three independent flags into StateFlow<ErrorState?>,
  >   so "sheet open with no error" and "dismissal pending with no sheet" stop being
  >   representable.
  > - The typealias NavBackStack becomes AppNavBackStack: sharing a name with the
  >   class it aliases meant the identifier denoted different things depending on a
  >   file's imports.
  > - NavKeys.Login is deleted. LoginView is rendered inline by the authenticated
  >   destinations, so the key was a destination with no entry that the type system
  >   still allowed onto the back stack; NavEntriesCoverageTest now needs no
  >   exemption list and walks sealed subclasses recursively, since sealedSubclasses
  >   is direct-only and an intermediate layer would have shrunk its coverage
  >   silently.
  > - The login callback is consumed once, so recreating the composition no longer
  >   replays the same URI through AuthHandler.
  > - KoinApp moves to di/, which removes the two same-named KoinApplication imports
  >   from App.kt without aliasing the annotation — the Koin plugin resolves that in
  >   FIR, where a miss shows up as a silently absent definition rather than an error.
  > - Column becomes Box: AppNavDisplay fills the available space, so the alignment
  >   did nothing and the sheet was a sibling with no room left.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/b9bcdd6efd7e2cb565baa97177983bfc67d96654"><code>b9bcdd6</code></a> <b>library:</b> ⚡️ filter sections client-side instead of refetching</summary>

  > loadLibraryEntries already populates every status in one pass, so selecting a
  > section no longer triggers a network round-trip (and no longer replaces the
  > grid with a full-screen spinner). Drops the now-unused section parameter and
  > the conditional clearing branch; mergeGames collapses to a single expression.

  </details>

### 💄 UI & Style

- <details><summary><a href="https://github.com/gamerlogue/app/commit/2c9a390caf267f8458cda2eb49b6acfb77b859a1"><code>2c9a390</code></a> 💄 Increase max line length to 150</summary>

  > Increases the maximum line length for the `formatting` rule in the `detekt.yml` configuration to 150 characters.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/1d08fd30126f6ddc38bb75c0e8c09d00434efb6f"><code>1d08fd3</code></a> 💄 Remove top and bottom padding from section titles</summary>

  > Removes the top and bottom padding from the section title `Text` composable within the `GameDetailScreen`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5f783a3580053724a9e2a820ae21e853a9c9f398"><code>5f783a3</code></a> 💄 Add spacing to GameDetailScreen</summary>

  > Adds a 12.dp spacer at the bottom of the `GameDetailScreen`'s content list to improve layout spacing.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7149da2095e13ea62102eedf92065ea39f3e1a56"><code>7149da2</code></a> 💄 Rename main.kt to Main.kt for desktop</summary>

  > Renames the main entry point file for the desktop target from `main.kt` to `Main.kt`, following Kotlin's naming convention for files containing top-level functions. The import order was also adjusted.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/33fc058cacdeba9b7cc1dfe41c4568b76cb4eb89"><code>33fc058</code></a> 💄 Rename WebAppInit to follow Kotlin conventions</summary>

  > Changes the `WebAppInit` function to `webAppInit` to adhere to the standard Kotlin coding convention for function names (lowerCamelCase). This affects the function declaration in `Platform.web.kt` and its call site in `Main.kt`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f410b1f6a4a32f1e01a10cb34922ec39e9910fdb"><code>f410b1f</code></a> 💄 Reorder imports in build script</summary>

  > Reorders the `JvmTarget` import in the `sharedUI/build.gradle.kts` file to follow idiomatic grouping. This change has no functional impact.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/901a4add4340696e39109eabc36ed96c08a413c1"><code>901a4ad</code></a> 💄 Sort imports in Theme.kt</summary>

  > Sorts the import statements alphabetically in the `Theme.kt` file.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/824c1e930bb80b8bfdbe7a071e68b4e5292d762b"><code>824c1e9</code></a> 🎨 Redesign Discover screen and improve game cards</summary>

  > Enhances the Discover screen layout with a new hero carousel for the featured section and adds contextual metadata badges to game cards.
  >
  > **UI Changes:**
  > - Implements `HeroCarousel` for the primary section, displaying game artwork or screenshots with a bottom scrim for better text legibility.
  > - Updates `GameCard` to display metadata badges (e.g., ratings or release dates) based on the section type.
  > - Refactors the Discover layout into specialized composables: `HeroCarousel`, `GameCarousel`, and `SectionHeader`.
  > - Standardizes card dimensions using `CardWidth`, `CardHeight`, `HeroWidth`, and `HeroHeight` constants.
  > - Adds an `EmptySection` view to handle empty states.
  > - Replaces `ListItem` in headers with a custom `Row` and `TextButton` for a more refined look.
  >
  > **Data Changes:**
  > - Updates `DiscoverViewModel` to include `rating`, `first_release_date`, `artworks.image_id`, and `screenshots.image_id` in IGDB multi-queries.
  >
  > **Resource Updates:**
  > - Adds `home__empty_section` string to English and Italian localization files.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/9b351443b75afaabbcc201e6e94fa5f68f92178b"><code>9b35144</code></a> 🖥️ Update desktop window title</summary>

  > Updates the title of the desktop application window from "Gamerlogue App" to "Gamerlogue".

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/cbd776965890928c3560016608fd9a00c7c99bc9"><code>cbd7769</code></a> 🎨 Refine GlobalExceptionBottomSheet layout and styling</summary>

  > Improves the visual hierarchy, readability, and interaction of the global exception bottom sheet used for error reporting.
  >
  > **Layout & Interaction:**
  > - Configured the bottom sheet to skip the partially expanded state (`skipPartiallyExpanded = true`).
  > - Added a `dismiss` helper using `rememberCoroutineScope` to handle programmatic sheet dismissal.
  > - Added bottom padding to the content container for better spacing on mobile devices.
  >
  > **Visual Enhancements:**
  > - **Header:** Increased error icon size and updated the container shape to `large`.
  > - **Typography:** Updated the main error message to use `bodyLarge` for better readability.
  > - **Styling:** Switched hint and detail backgrounds to modern surface container tokens (`surfaceContainerHigh` and `surfaceContainerHighest`).
  > - **Technical Details:** Repositioned the copy button to the header row of the details section and added `AnimatedVisibility` for a smoother transition when toggling details.
  >
  > **New Actions:**
  > - Added a prominent, full-width "Close" button at the bottom of the sheet using the `error` color scheme to provide a clear exit point for the user.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e3634aee27bcb96a542bafce4002fc12a1927cd2"><code>e3634ae</code></a> ✨ Refine NavigationBar visibility transitions</summary>

  > Updates the `AnimatedVisibility` logic for the bottom navigation bar to provide a smoother transition when switching between screens that show or hide the bar.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/47967764d6a1da51a81444dcefbac8819bdf77e3"><code>4796776</code></a> ⏳ Add loading progress indicator to ServiceWebView</summary>

  > Introduces a visual progress indicator to `ServiceWebView` to improve the user experience during page initialization, preventing the display of a black screen before the first frame is rendered.

  </details>
- [`4059207`](https://github.com/gamerlogue/app/commit/40592078822c6cf1831bc63815e8da07ae710818) Style: 🎨 update colors for toggle buttons and segmented items in AddToLibrarySheet
- <details><summary><a href="https://github.com/gamerlogue/app/commit/fce681106f72bbedeb0d79844f1d7feff78450c9"><code>fce6811</code></a> 💄 add icons to AddToLibrarySheet sections and form fields</summary>

  > - Import new Material icons (`category`, `devices`, `hourglass_top`, `inventory_2`, `rate_review`, `style`, `trophy`) in `build.gradle.kts`
  > - Update `DateField` to place its calendar icon as `leadingIcon` instead of `trailingIcon`
  > - Add section header icons in `AddToLibrarySheet`:
  >   - `Category` icon for Status header
  >   - `Trophy` icon for Completion Status header
  > - Add header and field icons in `AddToLibrarySheetSections`:
  >   - `Inventory2` leading icon for Owned list item
  >   - `Style` icon for Edition header
  >   - `Devices` icon for Platforms header
  >   - `HourglassTop` leading icon for Played Time field
  >   - `Star` icon for Rating header
  >   - `RateReview` leading icon for Review text field

  </details>
- [`f88bb84`](https://github.com/gamerlogue/app/commit/f88bb84dc22b04ad4bbea402cc2cdc8bfdb5d301) **ui:** 💄 unify GameCoverCard usage, carousel layout and library grid cards
- <details><summary><a href="https://github.com/gamerlogue/app/commit/aa20b31d993227fe5921d2acb59183a70507eb56"><code>aa20b31</code></a> 🎨 improve readability in AppTheme</summary>

  > - Add empty lines to separate logic blocks within the `AppTheme` composable function

  </details>
- [`422b6c5`](https://github.com/gamerlogue/app/commit/422b6c5231221281c7a1f43456e5f91a548bdf62) **services:** 🎨 reformat code blocks and Json configuration in ServiceConnector
- [`3bd31da`](https://github.com/gamerlogue/app/commit/3bd31dacbbeba9ae2dbe25a4ba413ce701bf4c25) **game:** 💄 make the IGDB overflow button tonal like back

### 🌐 Translations

- <details><summary><a href="https://github.com/gamerlogue/app/commit/a6cbbf7251548b0f3db150f18c91a457152c8399"><code>a6cbbf7</code></a> 🌐 Move hardcoded UI strings to resources</summary>

  > Extract remaining hardcoded EN/IT strings into strings.xml (placeholders,
  > descriptions, the Profile/Calendar copy and content descriptions) and reuse
  > existing keys where present.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d96b83f238f6f70cc2fec09fd5d9f01408d5c6ed"><code>d96b83f</code></a> 🌐 add strings for sync loading and import grouping</summary>

  > English (values/strings.xml):
  > - settings__import_group_to_import: 'To import (%1\)'
  > - settings__import_group_to_review: 'To review (%1\)'
  > - settings__import_group_already_present: 'Already in your library (%1\)'
  > - settings__service_done: 'All done'
  >
  > Italian (values-it/strings.xml):
  > - settings__import_group_to_import: 'Da importare (%1\)'
  > - settings__import_group_to_review: 'Da rivedere (%1\)'
  > - settings__import_group_already_present: 'Già nella libreria (%1\)'
  > - settings__service_done: 'Fatto'
  >
  > Used by:
  > - ServiceSyncScreen: LoadingContent (done state message)
  > - LibraryImportPreviewScreen: importGroup section headers with item counts

  </details>

### ♻️ Code Refactoring

- <details><summary><a href="https://github.com/gamerlogue/app/commit/ee69b578fbb977badcb69b87e3ee000eaada553d"><code>ee69b57</code></a> 🤫 Ignore cancellation exceptions in error reporting</summary>

  > Prevents `CancellationException` from being reported as a network error within the `safeRequest` utility function. This avoids treating user-initiated cancellations (like navigating away) as actual failures.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4a86af40c216c0b729526b8f1fbfb4f8bb641c23"><code>4a86af4</code></a> ♻️ Remove Koin KSP annotations</summary>

  > Removes the Koin KSP annotation processing library (`koin-annotations`) and refactors the dependency injection setup to use standard Koin DSL modules.
  >
  > This includes:
  > - Deleting the `koin-annotations` and `koin-ksp-compiler` dependencies.
  > - Removing the KSP plugin configuration from `build.gradle.kts`.
  > - Manually defining `appModule` and `httpModule` using the Koin DSL (`module`, `viewModel`, `single`).
  > - Updating the `KoinApplication` in `App.kt` to load the manually defined modules.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e70e8c675b8e02bbdd51236fc45aba3087c50968"><code>e70e8c6</code></a> 🧑‍💻 Use Collection instead of List in ApicalypseQueryBuilder</summary>

  > Changes the parameter type for `where` clause functions (`in`, `notIn`, `inAny`, `notInAny`, `matchesAll`) from `List<String>` to the more generic `Collection<String>`.
  >
  > This refactoring makes the query builder more flexible, allowing it to accept any collection type (like `Set` or `List`) without requiring conversion. It also improves code style by adding newlines for better readability.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/44a0ff27f556cf3d3bdf35162620891026a8fa38"><code>44a0ff2</code></a> ♻️ Use direct Koin modules instead of KoinApp</summary>

  > Replaces the `KoinApp` import with direct imports for `appModule` and `httpModule`. This change simplifies dependency injection setup by using the modules directly.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5456a5739d33606a353b7c73f1a6b1d3ac8f40d9"><code>5456a57</code></a> 📦 Move GameDetailsList to new package</summary>

  > Moves the `GameDetailsList.kt` composable from `ui.components.game` to `ui.views.game.components`. This change organizes the file structure by placing the component within its specific view-related package.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/96cff6e42104e03a31ec89bea7f6fa1fdb22791e"><code>96cff6e</code></a> 📦 Use enum for AppEnvironment and set BuildConfig package</summary>

  > Refactors the way the application environment is handled by introducing an `AppEnvironment` enum. The `build.gradle.kts` file is updated to use this enum for the `APP_ENV` build config field.
  >
  > Additionally, the `packageName` for the generated `BuildConfig` has been explicitly set to `it.maicol07.gamerlogue`, and all imports have been updated accordingly to reflect this change.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/832c005706f8663984b5a4aa2bc39a22c1217f07"><code>832c005</code></a> 🤫 Mark network exception as volatile</summary>

  > Marks the `networkException` variable in `AppUiState` with the `@Volatile` annotation. This ensures that changes to its value are always visible to all threads, preventing potential concurrency issues.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f504854732cacb40c71d11a23face86e96131b6a"><code>f504854</code></a> 🗃️ Persist user ID using AuthTokenProvider</summary>

  > Refactors authentication handling to persist the user ID alongside the authentication token using the `AuthTokenProvider`. This ensures the user ID is saved and restored with the session.
  >
  > This includes:
  > - Adding `getUserId()` and `setUserId()` to the common `AuthTokenProvider` interface.
  > - Implementing user ID storage in `AndroidAuthTokenProvider` using `AccountManager` and `JvmAuthTokenProvider` using Java `Preferences`.
  > - Updating the Android deep link and JVM login server to use the new provider methods for setting the user ID.
  > - Adding a `redirect_uri` to the Android login intent for deep linking.
  > - Ensuring the user ID is loaded into `AuthState` on app startup for both platforms.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/460cabb7e10d67daa3014de15d714997d7d0f4af"><code>460cabb</code></a> 🤫 Report JSON:API exceptions as network errors</summary>

  > Extends the `safeRequest` utility to also handle and report `JsonApiException` as a network error, in addition to `IgdbException`.
  >
  > This ensures that errors originating from the JSON:API client are captured and reported through the UI, consistent with how IGDB errors are handled. The check to ignore `CancellationException` remains in place.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/60b5917dd21f6eb856f4fcddeb2fc3a98e7fcbdb"><code>60b5917</code></a> ♻️ Use Result for library and user handling</summary>

  > Replaces `try-catch` blocks and manual error handling with the `Result` type from `michaelbull/kotlin-result` for more robust and consistent error management across the app.
  >
  > This change affects:
  > - Fetching the current user in `App.kt`.
  > - All library-related operations in `LibraryViewModel`, including loading entries, updating, removing, and fetching an entry for a specific game.
  > - Handling the result of `getLibraryEntryForGame` in `GameDetailViewModel`.
  > - Simplifies the save operation in `AddToLibrarySheetViewModel` by removing a `try-catch` block.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/67d438ef7c587a69dae07d34dbff697596eac574"><code>67d438e</code></a> 🧱 Modularize project structure</summary>

  > Refactors the monolithic `composeApp` module into a more modular structure, separating concerns into `sharedUI`, `androidApp`, `desktopApp`, and `webApp`.
  >
  > This major restructuring includes:
  > - **`sharedUI` module:** A new Kotlin Multiplatform library module that now contains all the common UI code, ViewModels, data layers, and resources previously in `composeApp`. It targets Android, JVM, JS, and iOS.
  > - **`androidApp`, `desktopApp`, `webApp` modules:** New platform-specific application modules that replace the targets within the old `composeApp`. Each module now depends on `sharedUI` and contains only the platform-specific entry points and configurations.
  > - **Gradle Configuration:**
  >     - Updates the root `build.gradle.kts` and `settings.gradle.kts` to reflect the new module structure.
  >     - Moves platform-specific build logic from `composeApp/build.gradle.kts` to the respective new app modules (`androidApp/build.gradle.kts`, `desktopApp/build.gradle.kts`, `webApp/build.gradle.kts`).
  >     - The `composeApp` module has been completely removed.
  > - **File Relocation:** All source code, resources, and platform-specific files have been moved from `composeApp` to their corresponding new modules.
  > - **Build Scripts:** The `README.MD` and `.gitignore` files have been updated to align with the new project layout and build commands.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/251057204fbf6217f49169a154ecedda64b59c1b"><code>2510572</code></a> <b>auth:</b> ♻️ Refactor auth handling with Koin and a base provider</summary>

  > Refactors the authentication and token management logic to use Koin for dependency injection and a more structured, multiplatform approach.
  >
  > This change introduces a `BaseAuthTokenProvider` and platform-specific implementations (Android, JVM, Web) that handle token persistence. The `AuthState` object has been removed, and the `AuthTokenProvider` is now injected via Koin where needed.
  >
  > Key changes include:
  > - A `BaseAuthTokenProvider` to centralize token state management.
  > - `AndroidAuthTokenProvider` and `JvmAuthTokenProvider` moved to `sharedUI` and now inherit from the base provider.
  > - `AuthenticationHandler` is now an abstract class, simplifying platform implementations by providing common callback handling logic.
  > - `CompositionLocalProvider` for auth is replaced with Koin injection (`koinInject<AuthTokenProvider>()`).
  > - `platformModule` expect/actual has been created in `sharedUI` to provide the correct `AuthTokenProvider` for each target.
  > - The HTTP client configuration (`AppJsonApiConfig`) now gets the auth token from the injected `AuthTokenProvider`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8ea0c500d0aad1a16f16dda8083b6caa80ea77bb"><code>8ea0c50</code></a> 🚚 Move web app initialization to sharedUI</summary>

  > Moves the web-specific initialization logic, specifically for `kotlinx-datetime-ext` locales, from the `webApp` module to the `sharedUI` module.
  >
  > This is done by:
  > - Creating a new `WebAppInit()` function in `sharedUI/src/webMain` to handle the locale setup.
  > - Calling this new function from the `main()` entry point in the `webApp`.
  > - Changing the `kotlinx-datetime-ext` dependency in `sharedUI` from `api` to `implementation`, as it's no longer exposed to the `webApp` module.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/1252a24ee84c4dd1ac3cd6627f689bd8ce03b452"><code>1252a24</code></a> 🤫 General code cleanup and modernization</summary>

  > This commit introduces a variety of cleanups, refactorings, and modernizations across the codebase.
  >
  > - **Auth Logic:** The authentication and user session handling logic has been extracted from the main `App` composable into a dedicated `AuthHandler` composable for better separation of concerns.
  > - **Apicalypse DSL:** The `equals` and `notEquals` methods in the `ApicalypseQueryBuilder` have been renamed to `equalTo` and `notEqualTo` to avoid conflicts with Kotlin's built-in `equals` function.
  > - **`AppUiState`:** The state properties `networkException` and `showExceptionBottomSheet` are now delegated properties (`by mutableStateOf`) instead of holding `MutableState` instances, simplifying state access.
  > - **Unused Code Removal:**
  >     - Several unused Material icons (`ic_cyclone.xml`, `ic_dark_mode.xml`, `ic_light_mode.xml`, `ic_rotate_right.xml`) have been deleted.
  >     - Unused imports and composable parameters have been cleaned up across multiple files.
  >     - Redundant functions like `getLibraryEntryForGame(game: Game)` have been removed.
  > - **Error Handling & Logging:**
  >     - Improved error logging in `JvmAuthenticationHandler` and when fetching library games.
  >     - Added robust try-catch blocks in the Android `UriHandler` to prevent crashes if context reflection fails.
  > - **Code Style & Suppressions:**
  >     - Added `@Suppress` annotations for lint warnings (e.g., `TooManyFunctions`, `unused`) to improve code clarity where intended.
  >     - Renamed the internal `GameDetailContent` composable to `gameDetailContent` to follow Kotlin's private function naming conventions.
  >     - Corrected the minimum window height for the desktop application.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/fb60297608807e001bae5eb65ba176c7947f4102"><code>fb60297</code></a> ⬆️ Migrate from KoinApplication to KoinMultiplatformApplication</summary>

  > Updates the Koin dependency injection setup to use `KoinMultiplatformApplication` instead of the deprecated `KoinApplication`. This aligns with the latest Koin for Compose Multiplatform practices.
  >
  > The change also removes the `additionalModules` parameter, as it is no longer used.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/9044bf9af5a9febc0618ef8c9ba632b62054765c"><code>9044bf9</code></a> 🏛️ Rename Home screen to Discover</summary>

  > Renames the "Home" screen and its related components to "Discover" to better reflect its purpose of finding new games.
  >
  > This includes:
  > - Renaming `HomeScreen.kt` to `DiscoverScreen.kt`.
  > - Renaming `HomeViewModel.kt` to `DiscoverViewModel.kt`.
  > - Updating the main `App` navigation graph to use `DiscoverScreen` instead of `Home`.
  > - Renaming internal composables from `homeSection` and `HomeSectionCarouselItem` to `discoverSection` and `DiscoverSectionCarouselItem`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/cd0bdd4e916272673a322e02af2b3965602eae04"><code>cd0bdd4</code></a> 🏛️ Rename expressiveColors to expressiveSegmentedColors</summary>

  > Updates the `ListItemDefaults` extension to align with the `ExperimentalMaterial3ExpressiveApi` and provide more granular control over component states.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/eada62c2f1102089a48d371531b72d5f593636ef"><code>eada62c</code></a> 🎨 Update Material symbol naming in NavigationBar</summary>

  > Updates the icon names used in the `NavigationBar` to match the updated naming convention in the symbol library.
  >
  > - Renames icon symbols with the `fill1` suffix to `Fill`.
  > - Affects `Explore`, `Newsstand`, `CalendarMonth`, and `Person` rounded symbols.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/663b9754f5b70a8af7f9ef5e5d90e3ac5a85a316"><code>663b975</code></a> ♻️ Add ViewModel bases, injectable JSON:API client and LibraryEntry helpers</summary>

  > Introduce core/BaseViewModel and StateViewModel (StateFlow with `update { copy() }`),
  > make the JSON:API Ktor client injectable via Koin so it can be swapped in tests,
  > and centralize LibraryEntry queries/mutations as reusable extensions.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e57e63bd61e3a43d5cb71c46f55935fba5f1c6fa"><code>e57e63b</code></a> ♻️ Migrate ViewModels to StateFlow and move navigation to callbacks</summary>

  > Replace Compose mutable state in the ViewModels with immutable nested UiState
  > exposed as StateFlow, drop the NavBackStack/VM-to-VM injections, and pass
  > navigation into screens as callbacks. Promote the shared section enum to
  > top-level DiscoverSection.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/0c0156e1fa89feb9aa97d197095eab8eeec90e80"><code>0c0156e</code></a> 🎨 Split large composables and drop dead code</summary>

  > Extract GameDetail content and the AddToLibrary sheet sections into focused
  > files (removing the TooManyFunctions suppression), and delete unused code
  > (the duplicate gamelist composable, DatePickerDocked, EditionDropdown).

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/3e398107843321a655dff9cf43d982fea1b45ca8"><code>3e39810</code></a> 🏛️ Decentralize TopBar management and introduce ScreenScaffold</summary>

  > Replaces the global `TopBarState` with a per-screen scaffolding approach. This move simplifies navigation UI management and ensures that each screen (or adaptive pane) correctly manages its own TopBar state, improving consistency and supporting modern Compose navigation patterns.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d2cdbf41f13a88ce5d97a38935f1b597bacbfde6"><code>d2cdbf4</code></a> 🎨 Extract GameCoverCard and improve game ratings UI</summary>

  > Refactors the game card display into a reusable component and enhances the visual presentation of ratings in the game detail view.
  >
  > **UI Components:**
  > - Created `GameCoverCard` to unify the appearance of game covers across the Discover and List screens.
  > - Extracted `bottomScrim` modifier for better text legibility on image backgrounds.
  > - Introduced a `Dimens` object to centralize shared spacing and padding constants.
  >
  > **Game Details:**
  > - Redesigned the ratings section in `GameDetailContent` using a more prominent `RatingTile` layout with icons and score scales.
  > - Added a "Ratings" section header.
  >
  > **Discover & Lists:**
  > - Updated `GameListScreen` to include a scaffold with a title, metadata badges on cards, and a dedicated empty state view.
  > - Moved rating and metadata formatting logic from the UI layer to `DiscoverSection`.
  >
  > **Web Support:**
  > - Added `NodePolyfillPlugin` to the Webpack configuration to support Node.js polyfills in the JS target.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/158968ac81215954212adcac6f6ca0bbe59b26f7"><code>158968a</code></a> 🔐 Improve authentication flow and deep link handling</summary>

  > Refactors the authentication callback logic to move deep link processing from the Android Activity into the Compose layer. This ensures that authentication tokens are handled within the Koin DI scope, allowing for consistent state management across the shared UI module.
  >
  > **Authentication & Deep Links:**
  > - **Activity Refactoring:** Updated `AppActivity` to capture login deep links as state and pass them to the `App` composable instead of processing them locally.
  > - **Shared Auth Handler:** Centralized callback handling in `AuthHandler` using a `LaunchedEffect`.
  > - **Token Decoding:** Implemented a recursive URL decoding mechanism in the shared layer to correctly handle multi-encoded tokens from the backend.
  >
  > **Network & DI:**
  > - **Ktor Auth:** Improved the `Auth` feature configuration to re-evaluate the `AuthTokenProvider` during `refreshTokens`. This allows the client to pick up tokens saved after a login flow during 401 retries.
  > - **Dependencies:** Added `koin-android` to the project and `androidApp` module to support Android-specific Koin features.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5550a98850bec31119bf706bc4ebdabdf4b457d3"><code>5550a98</code></a> 🔗 Migrate to custom openURL extension for URI handling</summary>

  > Replaces standard `uriHandler.openUri` calls with a custom `openURL` extension function to standardize how external links are handled across the UI.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4e5ac1a4837e4f5befe987507763b1e0bad7039b"><code>4e5ac1a</code></a> 📦 Update Koin initialization API</summary>

  > Updates the Koin DI initialization in the main `App` entry point to use the standard `KoinApplication` component.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f3df94d22e7b952133f03f825c01bb51c580a558"><code>f3df94d</code></a> 💉 Migrate to Koin Annotations and refactor navigation state management</summary>

  > Migrates the project's dependency injection from Koin DSL to Koin Annotations and replaces Koin-managed navigation state with a `CompositionLocal` for better UI integration.
  >
  > **Dependency Injection:**
  > - Migrated all modules (`AppModule`, `HttpModule`, `PlatformModule`) from DSL definitions to annotation-based configurations using `@Module`, `@Single`, and `@ComponentScan`.
  > - Integrated the Koin KSP compiler plugin and added `koin-annotations` dependencies.
  > - Annotated all ViewModels with `@KoinViewModel` and replaced manual parameter factory logic with `@InjectedParam`.
  > - Refactored `HttpModule` to use a shared `ktorHttpClientConfig` for consistent logging and retry behavior across IGDB, PSN, and Xbox clients.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2979bc401d796568b54d72f3d1708f797da9a6b5"><code>2979bc4</code></a> 🛠️ Simplify NavKey serialization registration</summary>

  > Introduces a reified helper function to streamline the polymorphic serialization setup for navigation keys, reducing boilerplate and preventing potential class-serializer mismatch errors.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a25d619368e93f535bc089b41f697329aaafd9e2"><code>a25d619</code></a> 🔐 Refactor AuthTokenProvider and implement web persistence</summary>

  > Refactors the authentication token provider hierarchy to centralize state management and introduces persistent storage for the web platform.
  >
  > **Auth State Management:**
  > - Converted `AuthTokenProvider` from an interface to an abstract class to house shared logic.
  > - Merged functionality from the now-deleted `BaseAuthTokenProvider` into the main `AuthTokenProvider` class.
  > - Standardized observable state for `accessToken`, `currentUserId`, and `currentUser` using Compose `mutableStateOf`.
  > - Centralized `updateToken`, `updateUserId`, and `restore` logic to handle both in-memory state and platform-specific persistence.
  >
  > **Web Support:**
  > - Implemented `WebAuthTokenProvider` using `window.localStorage` to persist authentication data across sessions.
  > - Updated `rememberAuthenticationHandler` for web to inject the `AuthTokenProvider` via Koin instead of using a placeholder implementation.
  >
  > **Platform Implementations:**
  > - Updated `AndroidAuthTokenProvider` and `JvmAuthTokenProvider` to inherit directly from the new `AuthTokenProvider` base class.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a1269d7643185126c49d71796b3f99df78b2bed7"><code>a1269d7</code></a> 🔒 Restrict visibility of KoinApp object</summary>

  > Changes the `KoinApp` object to `private` and relocates its definition within `App.kt` for better encapsulation.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d6c965ad26ba61dae4221b0f7bac0f9e3c83bc84"><code>d6c965a</code></a> 🧹 Use expression body syntax in authentication modules</summary>

  > Refactors authentication-related classes to use Kotlin expression body syntax and more idiomatic patterns, improving code conciseness and readability.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/1292380695ce696cfd66a2b7a72aba319b272d41"><code>1292380</code></a> 🛡️ Centralize exception reporting with ExceptionReporter</summary>

  > Replaces the global `AppUiState` and static `safeRequest` utility with a Koin-injected `ExceptionReporter` service. This transition decentralizes error handling logic while providing a cleaner, more testable way for ViewModels and services to report network and API errors.
  >
  > **Core Infrastructure:**
  > - **ExceptionReporter**: Introduced a new `@Single` service to manage the current exception state and the visibility of the error bottom sheet.
  > - **BaseViewModel**: Added a protected `safeRequest` helper that leverages the injected `ExceptionReporter`, making error-aware requests available to all ViewModels.
  > - **SafeRequest**: Relocated and refactored the `safeRequest` utility into the `core` package as an extension of `ExceptionReporter`.
  >
  > **UI & Layout:**
  > - **GlobalExceptionBottomSheet**: Now reacts to state from `ExceptionReporter` via Koin instead of relying on `CompositionLocal`.
  > - **TopBar**: Updated `NetworkErrorAction` to use the centralized reporter for visibility and interaction.
  > - **AppScaffold**: Simplified by removing the `AppUiState` provider and the `DisposableEffect` bridge used for static error reporting.
  > - **App.kt**: Integrated `ExceptionReporter` to manage the lifecycle of the global error sheet.
  >
  > **Data & Services:**
  > - **Service Updates**: Refactored `GameMatcher` and `LibrarySync` to inject `ExceptionReporter` for safe IGDB and API interactions.
  > - **ViewModel Refactoring**: Updated `GameDetailViewModel` and `AddToLibrarySheetViewModel` to use the new `safeRequest` pattern for library persistence.
  > - **Cleanup**: Deleted `AppUiState.kt` and the top-level `utils.kt`. Removed `LibraryEntry.persist()` and `LibraryEntry.remove()` extensions in favor of explicit `safeRequest` blocks.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7ce2d82c9f92f982402b5b0f33857666b57ebe2c"><code>7ce2d82</code></a> 📱 Update window adaptive info API</summary>

  > Updates the adaptive layout implementation in `AppNavDisplay` to use the latest version of the window adaptive info API.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/1f0a0ca7f11a9568c5029ff08cb76097a45ffa8e"><code>1f0a0ca</code></a> ♻️ Consolidate game navigation logic in AppNavDisplay</summary>

  > Extracts the redundant logic for navigating to game details into a reusable lambda to simplify screen definitions and ensure consistent data handoff.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/6c4efbfd597ddea663b4dc02c77301c1350c8a07"><code>6c4efbf</code></a> 🎨 Modernize Linked Services screen with Segmented Lists</summary>

  > Refactors the Linked Services settings screen to utilize the Material 3 Expressive APIs, replacing monolithic cards with a segmented list approach for better visual hierarchy and consistency.
  >
  > **UI Redesign:**
  > - Replaced `ServiceCard` with `ServiceSegmentedGroup` to group service-related actions.
  > - Migrated connection toggles, wishlist synchronization, and library import actions into `ListItem` components.
  > - Applied `expressiveShape` and `expressiveSegmentedColors` to dynamically handle clipping and background tinting for items within a group.
  > - Adjusted vertical spacing between service sections to 16dp for better separation.
  >
  > **New Components:**
  > - Introduced `SegmentedListLayout`, a reusable layout component that applies standard `ListItemDefaults.SegmentedGap` spacing to its children.
  >
  > **Technical Updates:**
  > - Adopted `ExperimentalMaterial3ExpressiveApi` across the linked services UI.
  > - Improved layout logic to conditionally clip items based on their position (first, middle, or last) within the service group.
  > - Cleaned up imports and updated the `uiState` message handling logic for clarity.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/28d4b706dbceb88f2a0f82d8bd845e0267067bd1"><code>28d4b70</code></a> 🛠️ Switch to mutable UI state and in-place updates</summary>

  > Refactors the `StateViewModel` architecture to use mutable state properties instead of immutable data class copies. This simplifies state management by allowing direct field assignments within update blocks.
  >
  > **Core State Management:**
  > - **`StateViewModel`**: Simplified the base class by removing the private backing flow and the `copy`-based `update` helper. It now exposes a `MutableStateFlow` directly.
  > - **`MutableStateFlow.update`**: Added a new extension function that provides an `apply`-style block to mutate the state object in-place.
  >
  > **ViewModel Refactoring:**
  > - Updated `UiState` definitions in `DiscoverViewModel`, `GameDetailViewModel`, `LibraryViewModel`, `GameListViewModel`, `LibraryImportViewModel`, and `LinkedServicesViewModel` to use `var` for state properties.
  > - Migrated all UI state updates to the new in-place mutation pattern (e.g., `uiState.update { isLoading = true }` instead of `update { copy(isLoading = true) }`).
  >
  > **Logic Adjustments:**
  > - **`LibraryImportViewModel`**: Refactored the matching batch processing and search result handling to use mutable list updates.
  > - **`LibraryViewModel`**: Simplified the merging of library entries by directly updating the mutable map of games.
  > - **`LinkedServicesViewModel`**: Updated service state and message handling to reflect the mutable state structure.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/561908999bb8d9dc8d91cd5e72791ec5c0379039"><code>5619089</code></a> 🔑 Improve service login flow and PSN authentication</summary>

  > Enhances the `ServiceConnector` architecture to support JavaScript-driven login triggers and cookie-based session validation. This specifically addresses issues with PSN authentication where direct OAuth navigation in a WebView can fail due to cross-origin restrictions.
  >
  > **Core Architecture:**
  > - **Asynchronous Session Checks:** Updated `isLoggedIn` in `ServiceConnector` to be a `suspend` function, allowing connectors to perform asynchronous operations like cookie lookups.
  > - **Login Triggers:** Introduced `loginTriggerScript` to allow connectors to inject JavaScript into the WebView to initiate login flows (e.g., clicking a specific "Sign In" button on a landing page).
  >
  > **Service Connectors:**
  > - **PSN:** Updated the login flow to land on the PlayStation home page instead of the library to ensure first-party cookies are set. Added a JS trigger to automate clicking the header sign-in button and implemented cookie-based session detection (checking for `isSignedIn`, `session`, or `userinfo` cookies).
  > - **Xbox:** Updated the `isLoggedIn` signature to match the new `suspend` requirement.
  >
  > **WebView Integration:**
  > - **Script Injection:** Modified `ServiceWebView` to execute a connector's `loginTriggerScript` once a landing page has finished loading.
  > - **UI Visibility:** Updated the visibility logic to reveal the WebView when the current URL contains "signin", ensuring consistent behavior across different store login patterns.
  > - **Cookie Management:** Integrated `PlatformCookieManager` to facilitate session detection via cookies.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/51cdde6cd044d279c522cc44ddf68552efa4ebbf"><code>51cdde6</code></a> 🎮 Update Xbox wishlist synchronization logic</summary>

  > Updates the Xbox connector to use the native `xbox.com` wishlist URL and refines the DOM selection logic for more reliable synchronization.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/fec30a828030946cfbac04cac135a82bc036f859"><code>fec30a8</code></a> 🏗️ Introduce DataSource and WishlistWrite in ServiceConnector</summary>

  > - Add sealed DataSource<T> (Web/Api) and WishlistWrite (Batch/PerGame) types
  >   to collapse parallel read/write methods into unified sources
  > - Replace 13 duplicated read/write members (readOwned, apiOwned, ownedViaApi,
  >   readWishlist, apiWishlist, wishlistViaApi, readProfile, apiProfile,
  >   credentialStep, addToWishlist, pushesPerGame, wishlistPushStep) with 4:
  >   ownedGames, wishlist, profile, wishlistWrite
  > - Convert no-arg pure-return methods to properties: loginUrl, storeLoginUrl,
  >   sessionUrls, loginTriggerScript (idiomatic Kotlin)
  > - Add factory helpers webRefs(), webProfile(), apiRefs(), apiProfile()
  > - Delete dead code: wishlistViaApi, apiWishlist, apiAddToWishlist
  >   (no connector implemented them)
  > - Create ApiClientSupport.kt for shared JSON reader and auth helpers

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/39ea72160b723e8923118fee607d1e39b746f2d2"><code>39ea721</code></a> 🧹 Extract shared API client utilities (PSN, Xbox, Epic)</summary>

  > - Move shared Json instance to ApiClientSupport as apiJson
  > - Replace duplicated Base64.encode boilerplate with basicAuth(id, secret)
  > - Add JsonObject.string() and requireString(msg) extensions for cleaner
  >   tree-walking; maintain explicit error messages (CLAUDE.md requirement)
  > - Replace 15+ instances of manual jsonPrimitive?.content chains with extensions
  > - Remove duplicate Json companions and @OptIn(ExperimentalEncodingApi) from each client

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/cb819e893413195bb7f51fe044c2e41728dcca8e"><code>cb819e8</code></a> ♻️ Rewrite connectors to new DataSource&#x2F;WishlistWrite surface</summary>

  > - All 5 connectors (Steam, GOG, PSN, Xbox, Epic) now use the unified read/write model
  > - Eliminate parallel method pairs (readX/apiX) and selector flags (xViaApi)
  > - Adopt factory helpers (webRefs, apiRefs, etc.) for concise read definitions
  > - Private credentialStep fields in API-path connectors (PSN, Xbox, Epic)
  >   shared between owned + profile without wrapper boilerplate
  > - Convert all methods to properties where applicable
  > - Wishlist write now uses sealed WishlistWrite.Batch or .PerGame, improving type safety
  > - Remove dead apiOwned/apiProfile/apiWishlist overrides (never used)

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5866135a949c9bf647a050200b1464fa4181ac89"><code>5866135</code></a> 🔌 Simplify WebSession and ViewModel orchestration</summary>

  > WebSession changes:
  > - Add read<T>(source: DataSource<T>): T resolver; centralizes isBlank guard +
  >   runCatching + log + default fallback (was repeated 6 times in connectors)
  > - Remove runProfile() (merged into generic read)
  > - Update callers to pass properties (not method calls)
  >
  > ViewModel changes:
  > - Delete ownedRefs(), wishlistRefs(), credential() helpers
  > - Delete pushesPerGame() and wishlistViaApi() conditional logic
  > - Simplify fetchProfile to: connector.profile?.let { session.read(it) }
  > - Simplify pushWishlist to use when(WishlistWrite) sealed match
  > - Delete 6 instances of: if (xViaApi) apiX(...) else readX(...)
  > - Update sessionUrls/storeLoginUrl/loginUrl to property access

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/11b2815fa0f5edf68c5c554c0be8260e0b580972"><code>11b2815</code></a> 🎯 Extract igdbCall helper in GameMatcher</summary>

  > - Add private suspend fun <T> igdbCall(warn, request): T? helper
  >   to consolidate safeRequest + getError().let{Logger.w} pattern
  > - Replace 7 identical call-site patterns in:
  >   - matchByStoreId (external_games url lookup)
  >   - matchByUid (external_games uid lookup)
  >   - urlsForGames (external_games url → game mapping)
  >   - urlsForGames (websites fallback lookup)
  >   - gamesByIds (games by-id fetch)
  >   - searchByName (fuzzy search)
  >   - nameMultiquery (multiquery with search/where fallback)
  > - Eliminates 20+ lines of duplicated error handling boilerplate
  > - Maintains explicit error messages per call-site for debugging

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/dafa4b40b08bca787ae8734f12ac96f8730fe630"><code>dafa4b4</code></a> ♻️ Deduplicate sync scripts in GOG and Steam connectors</summary>

  > Refactors the GOG and Steam service connectors to share common JavaScript logic for fetching user data. This reduces code duplication and improves the maintainability of the synchronization scripts.
  >
  > **GOG Connector:**
  > - Extracted paginated product fetching logic into a private `paginated(url)` helper function.
  > - Unified the implementations for `ownedGames` and `wishlist` retrieval using the new helper, as they share the same API response structure.
  >
  > **Steam Connector:**
  > - Extracted the WebAPI token retrieval and SteamID decoding logic into a shared `TOKEN_PREAMBLE` constant.
  > - Cleaned up the `ownedGames` and `wishlist` implementations by injecting the shared preamble, reducing boilerplate in the sync scripts.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/3b872016230c8569b5af7333c78a7fc7039a01f1"><code>3b87201</code></a> ♻️ hoist WebView session from ServiceWebView into reusable host</summary>

  > Extract WebSession interface, ServiceWebViewHost, and rememberServiceWebViewHost
  > composable from the monolithic ServiceWebView. This allows the WebView instance
  > to persist across navigation and be rendered in custom layouts (e.g., bottom
  > sheets) while keeping login state and scripts alive. Removes the old chrome
  > (login overlay, loading spinner, buttons) — now handled by hosting screen.
  >
  > - ServiceWebViewSession replaces WebSessionImpl, exposed publicly
  > - rememberServiceWebViewHost returns (session, webView composable)
  > - Callers provide the chrome, layout, and visibility logic
  > - Enables persistent bottom-sheet WebView during sync operations

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2e54525722d94743ac7fcb0a97e8d1f5b1d991e4"><code>2e54525</code></a> 🔄 simplify LinkedServicesScreen and ViewModel by removing inline WebView</summary>

  > LinkedServicesScreen changes:
  > - Remove inline ServiceWebView and full-screen replacement when action != null
  > - Remove message Card (sync result shown on sync screen now)
  > - Add LifecycleResumeEffect to refresh state when screen returns from sync
  > - Change callbacks to navigateToSync: (service, action) -> backStack.add(...)
  > - Remove busy spinner from rows (action nav means no inline state)
  > - Sync flow: toggle-on calls toggleWishlistSync + navigateToSync (so sync happens on screen)
  >
  > LinkedServicesViewModel changes:
  > - Remove sealed interface Action and action: Action? from UiState
  > - Remove intent methods: connect, refreshProfile, syncWishlistNow, previewWishlist, importLibrary, clearAction
  > - Add refreshAll() to re-read all services from settings (called on screen resume)
  > - Keep all run* suspend fns for sync screen to use

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c5d473891fe63434e527ffbfca03cbf2bda30093"><code>c5d4738</code></a> 🎨 convert OwnedSwitch to SegmentedListItem in AddToLibrarySheetSections</summary>

  > - Replace custom Row and Column layout with Material 3 Expressive `SegmentedListLayout` and `SegmentedListItem`
  > - Apply `expressiveSegmentedColors` and `segmentedShapes` defaults
  > - Pass description as `supportingContent` and move `Switch` to `trailingContent`

  </details>
- [`42e83e7`](https://github.com/gamerlogue/app/commit/42e83e773c142402d935233f4c73016d43e1f156) **nav:** ♻️ migrate game list view to dedicated top-level navigation destination
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8f3de49e9ae10379a504ced6cea018b135e2f8ca"><code>8f3de49</code></a> <b>ui:</b> ♻️ drive navigation bar contrast from shared code</summary>

  > Navigation bar contrast was an Android-only concern leaking into the
  > common App() signature as a callback the launcher had to wire up. Move
  > it behind an expect/actual composable next to SystemBarsVisible, which
  > already models the same kind of platform system-bar tweak.
  >
  > This drops the parameter from App(), the LaunchedEffect that forwarded
  > it and the SDK_INT branch in AppActivity.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5e4363ee4bc8df5ebb1c5dc6e808b5bd4b0d8b49"><code>5e4363e</code></a> <b>gamelist:</b> ♻️ derive the active-filter check and share filter sections</summary>

  > isActive listed 21 clauses that had to be extended by hand for every new
  > filter; it is exactly "differs from the default state", so it is now
  > written that way and cannot fall behind GameListFilterState again. Covered
  > by tests, since the behaviour is no longer obvious from reading it.
  >
  > The release-year bounds were literals repeated across the state defaults,
  > the query and the slider, with the upper one frozen at 2026; they are now
  > shared constants and the upper bound follows the clock.
  >
  > fetchPage carried three detekt suppressions for its size: the filter
  > clauses and the sort clause move into their own functions.
  >
  > The filter sheet repeated the same 15-line block for each of the seven
  > multi-select sections and the four range sliders. Two local composables
  > replace all eleven; the fixed option sets become top-level lists.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d5f309c104cdddf948cfd5c2407b9d642d2e7c33"><code>d5f309c</code></a> <b>ui:</b> ♻️ split the detail sections and the nav graph by concern</summary>

  > GameDetailContent held sixteen composables in one 730-line file; they now
  > sit in three files grouped by what they render (ratings and time to beat,
  > taxonomy chips, media and related games), leaving the entry point as the
  > section list it always was.
  >
  > AppNavDisplay's entryProvider was a 150-line lambda covering every
  > destination in the app. It is now four extension functions, one per area,
  > so a destination is found by its feature rather than by scrolling.
  >
  > Behaviour is unchanged; the repeated "read the session and fall back to
  > the login view" pair is the one thing that became shared.

  </details>
- [`16f14d0`](https://github.com/gamerlogue/app/commit/16f14d06c7757fcddd7a6fceb161ecb85a8b5395) **navigation:** ♻️ share one-shot handoffs
- [`5b0cddc`](https://github.com/gamerlogue/app/commit/5b0cddc1ff8f32a901179fc1a24aab2a5b5a3e73) **igdb:** ♻️ validate multiquery result types
- [`3e08c41`](https://github.com/gamerlogue/app/commit/3e08c4143c26fe476bcfd5e4006024174a2acee9) **game:** ♻️ simplify detail queries and status toggles
- <details><summary><a href="https://github.com/gamerlogue/app/commit/6c863a46d493de78467f2b446a628f83f3227c00"><code>6c863a4</code></a> 🔄 remove NavHandoff and clean up game detail seeding</summary>

  > - Delete `NavHandoff` utility class
  > - Remove `GameHandoff` and initial seed state handling from `GameDetailViewModel`
  > - Remove unused `GameDetailViewModel.inject(Game)` overload
  > - Inline map-based storage directly into `ImportHandoff` in `LibraryImportViewModel`

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ef54ff7440c499efa7f43535cf6f867879cd6ff8"><code>ef54ff7</code></a> <b>desktop:</b> 📦 migrate app icon to Compose Multiplatform resources</summary>

  > - Move `AppIcon.png` to `composeResources/drawable/app_icon.png`
  > - Add `libs.compose.resources` dependency to `desktopApp`
  > - Update `Main.kt` window icon to use generated `Res.drawable.app_icon` with `org.jetbrains.compose.resources.painterResource`
  > - Update Linux packaging configuration to point to the new icon path

  </details>
- [`92a2f5b`](https://github.com/gamerlogue/app/commit/92a2f5b7cfcd3a6046354d88b108666043483ddc) **auth:** ♻️ use `apply` for concise Bundle initialization
- [`fba323f`](https://github.com/gamerlogue/app/commit/fba323f7e7c66a238c1f41e86c50e5cd3b808689) **app:** ♻️ remove experimental API annotation from KoinApp
- <details><summary><a href="https://github.com/gamerlogue/app/commit/6f93144f7081dba47b78a849798ef410da22c326"><code>6f93144</code></a> <b>search:</b> 🔍 simplify GameSearchBar and fix keyboard behavior</summary>

  > - Remove redundant `SearchBar` wrapper and use `SearchBarDefaults.InputField` directly within `SearchBarShell`
  > - Eliminate unnecessary `InterceptPlatformTextInput` side effects from the standard `SearchBar` that interfered with the soft keyboard
  > - Apply `fillMaxWidth()` to the search input field modifier to ensure proper layout occupancy

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/0d29d02b24f741b4e423b145c63ca297813f1ea3"><code>0d29d02</code></a> <b>game:</b> ♻️ preserve search query when resetting filters</summary>

  > - Update `resetFilter` in `GameListViewModel` to retain the current `searchQuery` while clearing all other filter states and searches.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/3496c52622f9ef8424d70d329f1bbca4e8b0eb37"><code>3496c52</code></a> <b>navigation:</b> 🔄 update navigation keys to use sealed interface for better serialization</summary>

  > - Changed navigation keys to implement a sealed interface, improving serialization handling.
  > - Simplified navigation logic by removing the need for a SerializersModule.
  > - Ensured that overrides for title and showBottomBar use custom getters to avoid unnecessary serialization.

  </details>
- [`892a574`](https://github.com/gamerlogue/app/commit/892a5742d1fd6c8a4d370ad699fb660602b034af) **http:** 🔄 streamline imports and remove unused dependencies in HttpModule
- <details><summary><a href="https://github.com/gamerlogue/app/commit/82067fd9a033ace2d254e9df120d61cabf6afab9"><code>82067fd</code></a> <b>ui:</b> 🔄 improve layout readability and streamline modifier usage</summary>

  > - Fix import reordering for consistency.
  > - Enhance code readability by adding block structure in `GlobalExceptionBottomSheet`.
  > - Improve AnimatedVisibility and text formatting for technical details.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/097e02b65bdb315b3ae0f6caa701af2864d3139c"><code>097e02b</code></a> <b>ui:</b> ♻️ restructure AppTheme and improve theme management</summary>

  > - Refactor `AppTheme` to include `isDark` property for better theme mapping.
  > - Move `AppTheme` to `ui.theme` package.
  > - Simplify `isDarkTheme` determination logic in `Theme.kt`.
  > - Replace `derivedStateOf` with `AppTheme.of` for theme selection in `AppearanceScreen`.
  > - Update `SettingsViewModel` to handle dark mode logic using `isDark`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ab840f5ee89c63c41b2342b24010ef48615d297c"><code>ab840f5</code></a> <b>ui:</b> 🔄 remove unused title properties and streamline Scaffold initialization</summary>

  > - Remove redundant `title` parameters from `DiscoverScreen` and `GameListScreen`.
  > - Enhance `Scaffold` with seeded `NavigationSuiteScaffoldState` to prevent animation glitches.
  > - Set default value for the `title` property in `ScreenScaffold`.
  > - Adjust logic to simplify `topBar` initialization for cleaner code.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/1a4a7f41d186dd5f735d04c2ae7c9cdfd9c2e804"><code>1a4a7f4</code></a> <b>ui:</b> ♻️ replace compositionLocalOf with staticCompositionLocalOf and centralize transition logic</summary>

  > - Refactor `LocalSharedTransitionScope` to use `staticCompositionLocalOf` for consistent static composition handling.
  > - Consolidate transition animations with a reusable `fadeTransition` definition.
  > - Optimize `entryProvider` initialization with `remember` for better performance.
  > - Simplify transition specifications by reusing `fadeTransition` across all scene strategies.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/39c21cacf68310ddcb8dec3aa579d3364b2d55c9"><code>39c21ca</code></a> <b>settings:</b> ♻️ centralize preferences behind AppPreferences</summary>

  > Theme.kt built its own settings StateFlow over a rememberCoroutineScope,
  > a composition-scoped flow for state that outlives every composition and a
  > second source over the same key as the ViewModel's. One injected facade now
  > owns the key names and the meaning of an absent value, and hands out typed
  > flows instead of Boolean?.
  >
  > SettingsViewModel is left with logout(); AppearanceScreen reads the facade
  > directly, injected in the body because nav3ksp turns @Branch parameters
  > into nav key properties.

  </details>
- [`58d8fa2`](https://github.com/gamerlogue/app/commit/58d8fa2cac245444370d2fb929d3c80aa36849e1) **ui:** 🔄 convert webViewNestedScrollModifier to Modifier extension function
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d4122522cd548b66859c0e7c7d183af6e14b63ba"><code>d412252</code></a> <b>ui:</b> 🌐 migrate count-based string resources to plurals</summary>

  > - Convert language count, imported items, selected items, and column count strings to plurals resources
  > - Update Italian localization resources with appropriate plural quantity rules
  > - Update GameDetailsList component to use pluralStringResource

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/3516ad6689c2166b1d01ef1ff612122d3c7901aa"><code>3516ad6</code></a> <b>android:</b> 🎨 migrate variant launcher icons to flavor source sets</summary>

  > - Move variant launcher icons (`alpha`, `beta`, `debug`) from `main` to their respective `src/{flavor,buildType}/res` directories
  > - Standardize resource names to `ic_launcher*` across all variants, relying on Gradle source set overlaying
  > - Remove `appIcon` manifest placeholder from `build.gradle.kts` and set `android:icon="@mipmap/ic_launcher"` in `AndroidManifest.xml`
  > - Update `AGENTS.md` with new launcher icon structure and regeneration workflow

  </details>
- [`44049e2`](https://github.com/gamerlogue/app/commit/44049e2885cd484b9f7ec334eb81257abbfd910d) **ui:** 🔄 use explicit 'this' receiver in webViewNestedScrollModifier
- <details><summary><a href="https://github.com/gamerlogue/app/commit/42ecd2fab398315c0a6bdb402182663075dccba5"><code>42ecd2f</code></a> <b>services:</b> ♻️ remove secondary store login flow and clean up suppressions</summary>

  > - Remove `storeLoginUrl` from `ServiceConnector` and manual login handling from `WebSession` and `ServiceWebView`
  > - Update `LinkedServicesViewModel` and `ServiceSyncScreen` to remove store login confirmation and prompts
  > - Clean up redundant `@Suppress("unused")` annotations across dependency injection modules
  > - Add `@Suppress("EmptyMethod")` annotations to no-op JVM platform composable functions

  </details>
- [`7dfcc63`](https://github.com/gamerlogue/app/commit/7dfcc633f5629b642a58ad14a5fd44c6a1b046db) **services:** 🔄 convert isServiceSyncSupported from function to property
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f1f242cfbb0e01121857ef813c192a75d254da4e"><code>f1f242c</code></a> <b>auth:</b> 🔄 add `@Throws` annotations to API and auth helpers</summary>

  > - Add explicit `@Throws` annotation to `basicAuth` in `ApiClientSupport`
  > - Annotate `start`, `handleCallback`, and `writeResponse` in `LoopbackAuthServer` with `@Throws`

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/37fcf789fe58934ebc75e385af4a14f489673865"><code>37fcf78</code></a> 🔄 suppress SameReturnValue warnings across sharedUI</summary>

  > - Add `@Suppress("SameReturnValue")` annotations to constant getters in `Platform` and `ServiceConnector`

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c11f8bf2d1b986a5872bddac6a05c617899db6f1"><code>c11f8bf</code></a> 🔄 remove redundant @Throws annotations and refine Android URL handling</summary>

  > - Remove unnecessary `@Throws` annotations across `ApiClientSupport`, `LoopbackAuthServer`, and `UriHandler`
  > - Refactor `AndroidUriHandler.openURL` context reflection to use early returns and catch `NoSuchFieldException` and `ClassCastException`

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5f84586f308992a69261d43f0012b21d64eb834a"><code>5f84586</code></a> <b>ui:</b> 🔄 simplify service sync completion and clean up image viewer parameters</summary>

  > - Replace `CompletionContent` with dedicated `ErrorContent` in `ServiceSyncScreen`, automatically finishing on sync success
  > - Remove unused `settings__service_done` string resources and `CheckCircleW500Rounded` icon
  > - Remove unused overlay height and scrim parameters from `FullscreenImageViewer` and `TopOverlay`

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/55c8b0b42a63ac17cea82e5b70a1214be1b5b134"><code>55c8b0b</code></a> <b>auth:</b> 🔄 add explicit types and update coroutine delay duration</summary>

  > - Specify explicit return type for GamerlogueAuthenticatorService.onBind
  > - Add explicit ScheduledExecutorService type to watchdog in LoopbackAuthServer
  > - Update AuthenticationHandlerTest to use 50.milliseconds for coroutine delay

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/259bee0b2b79956b1d22d2eff43d6ae8e5ed1501"><code>259bee0</code></a> <b>services:</b> ♻️ use multi-dollar string interpolation in UbisoftApi</summary>

  > - Replace `${'$'}` string escaping with Kotlin multi-dollar raw string literal syntax (`$$"""`) in `GET_OWNED_GAMES_QUERY`

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/9005ed9aebd4b9c0fadba9cfc23c927234698d08"><code>9005ed9</code></a> <b>sharedUI:</b> 🔄 use Kotlin Duration for coroutine delays and timeouts</summary>

  > - Convert raw millisecond constants to `Duration.milliseconds` across `GameMatcher`, `ServiceWebView`, and `GameListViewModel`
  > - Update `delay` and `withTimeoutOrNull` calls to use `kotlin.time.Duration` overloads

  </details>
- [`ed2b5d8`](https://github.com/gamerlogue/app/commit/ed2b5d879445c87b2f2949778730e84003bb4a4f) **auth:** 🔄 suppress SameParameterValue warning in JvmAuthTokenProvider
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c0829218c7ab082f3f08b9673ef0c0f0fb2aaec3"><code>c082921</code></a> 🧹 remove unused OptIn annotations and update inspection profiles</summary>

  > - Remove redundant `@OptIn` annotations for experimental APIs across shared UI screens, components, and view models
  > - Remove unused `@file:OptIn(ExperimentalWasmDsl::class)` annotations in `sharedUI` and `webApp` Gradle build scripts
  > - Disable `unused` and `UnnecessaryModuleDependencyInspection` in default project inspection settings

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/082a1c119c778da680c895b92ca8e5784ef1d2af"><code>082a1c1</code></a> <b>ui:</b> ♻️ move shared widgets to components and fix events lint</summary>

  > - Split DiscoverScreen: events block to DiscoverEvents.kt (detekt TooManyFunctions)
  > - Move FeaturedEvent/BucketLabel, pressMorphShape and SectionIcon to ui/components
  > - Move Game.ratingScore/ratingLabel and Event.startYear to extensions/igdb
  > - EventListScreen: status composables to EventListStatus.kt, read scroll position
  >   via snapshotFlow instead of in composition
  > - EventsViewModel: fix constant naming, ComplexCondition and ReturnCount
  > - AGENTS.md: shared code placement rule, AgentBridge get_problems check
  > - Drop unused OptIn imports and reformat long lines in Discover

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4bacf53e384f922d11ca7e4d119e6ad1424552b9"><code>4bacf53</code></a> <b>ui:</b> 🔄 optimize game list state management, filtering, and pagination</summary>

  > - Inject DiscoverSection and eventId parameters directly into GameListViewModel and execute initial fetch in init
  > - Defer filter range slider updates to onValueChangeFinished to eliminate redundant network requests while dragging
  > - Preserve searchable filter option names across sheet dismissals by tracking knownOptions in UiState
  > - Decouple GameListResults from GameListViewModel by accepting UiState directly and hoisting GameListFilterSheet to GameListScreen
  > - Optimize scroll pagination detection using snapshotFlow on LazyGridState to avoid recompositions
  > - Extract SortSection and update Material3 bottom sheet implementation in GameListFilterSheet

  </details>
- [`49a88ce`](https://github.com/gamerlogue/app/commit/49a88cedd528fd025e0f8d0a13a81ade7eb4e177) ♻️ support modern IGDB metadata localization
- [`c08e2ac`](https://github.com/gamerlogue/app/commit/c08e2ac87a466fef8d7c0d061149519d4bdd5344) **game:** 🔄 suppress SpreadOperator warning in GameDetailViewModel
- <details><summary><a href="https://github.com/gamerlogue/app/commit/18dcfa7fe9050203a256663c51f8cba10d9c26e5"><code>18dcfa7</code></a> <b>ui:</b> ♻️ add connected action button group and standard modifier param</summary>

  > - ConnectedActionButtonGroup: connected look with plain buttons, for
  >   options that act (open a link) rather than select.
  > - Shape choice shared between the toggle and action groups.
  > - rowModifier renamed to modifier and made the first optional parameter,
  >   per Compose conventions; callers updated.
  > - Drop unused imports.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c7095dca50d1803ea6c9ffed469cedde7494eacf"><code>c7095dc</code></a> <b>viewer:</b> ♻️ save images with FileKit</summary>

  > Replace the hand-written MediaStore, AWT FileDialog and JS download code
  > with FileKit: saveImageToGallery on Android, openFileSaver on desktop,
  > download on web. Image bytes are fetched in common code.
  >
  > On Android 8-9 the gallery is shared storage: WRITE_EXTERNAL_STORAGE is
  > now declared with maxSdkVersion 28 (replacing the implied, unbounded one
  > that was removed) and requested at runtime before saving.

  </details>
- [`0db7a06`](https://github.com/gamerlogue/app/commit/0db7a066c1bbfc10f708e6d2c55b10888f4b328c) **settings:** 🎨 make appearance TotalItems private
- <details><summary><a href="https://github.com/gamerlogue/app/commit/76c395426c4170919b7140d7d11d2cd5efe47fce"><code>76c3954</code></a> <b>settings:</b> ♻️ describe settings entries with a typed list</summary>

  > Replace the remembered map of Triples with a list of SettingsEntry, so
  > adding an entry is a single line and segmented shapes follow the list size.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/961439d9cb8e17cbe6ccc74479e05bc8e0227354"><code>961439d</code></a> <b>ui:</b> ♻️ promote game detail status to shared StatusMessage</summary>

  > Container colors and shape become parameters, defaulting to the game
  > detail look, so other screens can reuse the expressive status layout.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/86ed0e931f69abda750369094701ca93afeea5a8"><code>86ed0e9</code></a> <b>ui:</b> 🎨 opt-in to ExperimentalMaterial3Api for TooltipBox, GameToolbar, and AddToLibrarySheet</summary>

  > - Added @OptIn annotation to enable usage of ExperimentalMaterial3Api in multiple UI components.
  > - This change allows for the use of new Material3 features while maintaining compatibility.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2014b3afb515fc009d1465ec42a329fe0f6b00ca"><code>2014b3a</code></a> <b>ui:</b> 🎨 update ToggleButtonDefaults API calls in ConnectedButtonGroup</summary>

  > - Replaced `toggleButtonColors` with `colors` for `ToggleButtonDefaults`.
  > - Updated `shapes()` to `shapesFor(ToggleButtonDefaults.MinHeight)` for single item shapes configuration.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/102c68874901f2a7053594fe516872482c9a6208"><code>102c688</code></a> <b>library:</b> 🎨 derive empty-state text from GameLibraryStatus</summary>

  > Move the per-status empty message into the enum next to displayName/icon,
  > collapsing the duplicated `when` in EmptyLibraryState. Add a stable item key
  > to the grid (Game has structural equality, so the section map already
  > deduplicates), memoize the "all" section flattening and split LibraryGrid /
  > CenteredBox out of LibraryContent.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/bf9d9403559b3de88abc4798af87d80c5e074161"><code>bf9d940</code></a> <b>ui:</b> ♻️ extract the expressive empty state into a shared component</summary>

  > Lift GameListResults' shaped-icon empty state into ui/components/EmptyState
  > so the library can reuse it instead of keeping its own copy.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f985f10b99ae55e7ac3ccb29ada4ae0c50f544ca"><code>f985f10</code></a> <b>discover:</b> ♻️ make GameCarousel reusable outside Discover</summary>

  > Split the press-morphing cover carousel from Discover's section badges:
  > the core takes the card metadata and an overlay badge as slots, and the
  > DiscoverSection overload keeps its date-toggling badge on top of it.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2ad8f4c9f7c1758dd11d6de54010924073ae9d80"><code>2ad8f4c</code></a> <b>schema:</b> ♻️ update ResourceSchema annotations for LibraryEntry and User schemas</summary>

  > - Added resourceType to LibraryEntrySchema for better clarity
  > - Removed unnecessary parameters from UserSchema ResourceSchema

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e50a9d3fc6e06cc013aec46e1e1befc6b48e89ed"><code>e50a9d3</code></a> <b>library:</b> ♻️ load status previews without ignored page params</summary>

  > The backend ignores page[number] and page[size], so the preview request
  > drops them and takes the fixed-size first page as is; the section count
  > comes from the shared totalItems accessor.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/74906c223a28f2e960a6f068ae997c649221dbe7"><code>74906c2</code></a> <b>services:</b> ♻️ share the connectors&#x27; DOM-waiting JavaScript</summary>

  > Every store script hand-rolled its own polling: 9 copies of "wait for this
  > selector to render", 4 of sleep(ms) and 2 poll-and-click login triggers,
  > with retry budgets that drifted between 30 and 40 tries for no reason.
  >
  > SyncScripts now ships __glSleep / __glWaitFor / __glWaitForEl /
  > __glWaitForAll / __glVisible / __glClickUntil in the preamble every wrapped
  > script already goes through, plus a clickWhenPresent() builder for the login
  > triggers, which are injected raw and so carry their own copy of the helpers.
  > __glClickUntil also unifies the click-then-verify-with-retry dance that PSN,
  > Xbox and Epic each wrote separately for React/SSR add-to-wishlist buttons.
  >
  > Behaviour is unchanged except that the few waits that used a 40-try budget
  > keep it explicitly (Ubisoft's store, the login triggers) instead of by
  > accident.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2dfc20de0eb239e591ca03252b35e1a8ceed043c"><code>2dfc20d</code></a> <b>services:</b> ♻️ carry an API credential as a string, not a game list</summary>

  > A DataSource.Api credential (the PSN npsso cookie, the Xbox MSA token, the
  > Epic auth code, the Ubisoft ticket) was smuggled through the games channel:
  > the script built an ExternalGameRef whose uid was the secret, and the reader
  > dug it back out with parseRefsJson(...).firstOrNull()?.uid. The scripts now
  > assign the string itself and parseCredentialJson reads it, telling a blank
  > credential apart from the wrapper's empty-array sentinel.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a2a5aeb80fc990c3b02ea702c29305788dcd67a1"><code>a2a5aeb</code></a> <b>di:</b> ♻️ key the connector registry off the connectors themselves</summary>

  > provideConnectors wrote out ExternalService.X to XConnector() by hand, so
  > the key duplicated the service the connector already declares and the two
  > could disagree — and since LinkedServicesViewModel reads the map with
  > getValue, a new enum entry without a connector compiles and then crashes on
  > the first lookup. The map is now built with associateBy { it.service }, and
  > a test asserts every service resolves.

  </details>
- [`49d3f25`](https://github.com/gamerlogue/app/commit/49d3f259e79984ed61729c47d7f8842d72ccd787) **services:** ♻️ dedupe the push rule and import handoff in the sync screen
- [`b449178`](https://github.com/gamerlogue/app/commit/b44917832710db48fc727795c6a990e1c183d823) **game:** ♻️ drop the redundant time-to-beat conversions
- [`bf7f497`](https://github.com/gamerlogue/app/commit/bf7f497d8b6e8d717bac4c242fc88655a634c0f1) **ui:** ♻️ move off the deprecated Material3 sheet, list item and slider APIs
- [`dae1e1d`](https://github.com/gamerlogue/app/commit/dae1e1d21ce77e0931ad5ca51b1993227a09935e) **services:** 🚨 resolve the detekt and KDoc warnings in the sync stack
- <details><summary><a href="https://github.com/gamerlogue/app/commit/43413fdf92708302d28e53eaaebe41e546088f78"><code>43413fd</code></a> <b>game:</b> 💄 reorder the detail sections around what a visitor asks</summary>

  > The description and media move up next to the ratings; the game's own
  > editions, add-ons and series follow how it plays, then its other
  > releases; reference data (details, age ratings, keywords, websites)
  > sinks lower, and similar games close the page.

  </details>

### 📝 Docs changes

- <details><summary><a href="https://github.com/gamerlogue/app/commit/82d050cd0d0962ad12fce00d786372228851d9c0"><code>82d050c</code></a> 📝 Add IDE best practices for AgentBridge MCP</summary>

  > Updates `CLAUDE.md` with a new section detailing guidelines for working within an IntelliJ IDEA environment via the AgentBridge MCP.
  >
  > **Key Guidelines:**
  > - **Tooling & Workspace:** Emphasizes trusting MCP tool outputs and using `create_scratch_file` for temporary notes and plans to avoid project pollution.
  > - **Editing Workflow:** Provides specific instructions for managing sequential edits, including how to handle `auto_format_and_optimize_imports` to prevent accidental removal of new imports.
  > - **VCS Integration:** Mandates the use of dedicated `git_*` tools instead of shell commands to maintain synchronization with the IDE's version control layer.
  > - **Verification Hierarchy:** Defines a structured approach for verifying code changes, ranging from editor auto-highlights to full incremental builds.
  > - **Formatting:** Standardizes file references using the `FileName.ext:123-456` clickable link format.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/270b75816fe82b315f678bfe99673cac88286f0f"><code>270b758</code></a> 📝 record the http cache rule, token storage and Koin plugin quirks</summary>

  > Written down because each one cost time to rediscover:
  > - Never install HttpCache on a user-authenticated client, and why.
  > - Where the session token lives on each platform.
  > - App.kt must not read the back stack, with the recomposition chain that makes
  >   it matter.
  > - The Koin compiler plugin silently refuses to register a KtorHttpClient
  >   provider, and its checker reports false KOIN-D002 on qualified get calls.

  </details>
- [`4303896`](https://github.com/gamerlogue/app/commit/430389621bd0ae02e770e792a1041784d96981d1) **project:** 📝 refresh development guidance
- [`50c35ed`](https://github.com/gamerlogue/app/commit/50c35ed6d7dab30df70a0a16433f9493f9401f5c) 📝 add AGENTS.md for coding guidance and best practices
- [`8ab0ff9`](https://github.com/gamerlogue/app/commit/8ab0ff953dd034102f874899a194cecec17ad9fb) 📝 update AGENTS.md with auth architecture and local network guidance
- <details><summary><a href="https://github.com/gamerlogue/app/commit/28ffca841d33da641b87e9454377b76f42003c03"><code>28ffca8</code></a> 📝 update AGENTS.md with Kotlin 2.4 and Material 3 Expressive conventions</summary>

  > - Add conventions for Kotlin 2.4 features including explicit backing fields, guard conditions, and single-expression functions
  > - Document Material 3 Expressive UI design guidelines covering emphasized typography, shapes, loading indicators, and motion schemes

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/28ff8946e45ad386239ac64988c02e54bcd66e72"><code>28ff894</code></a> 📝 update AGENTS.md with Koin compiler plugin guidance</summary>

  > - Remove outdated Koin compiler Kotlin version warning
  > - Document Koin compiler 1.2.x test compilation safety workaround

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/06672228d74ec37be1b4ebac8b9506ca06bf1dca"><code>0667222</code></a> 📝 fix typos and grammar in documentation and string resources</summary>

  > - Fix spelling, grammar, and punctuation in KDocs and inline comments across connectors, view models, and modules
  > - Refine wording in `library__played_time_description` string resource for consistency
  > - Rename local variable `kdate` to `date` in `convertMillisToDate` helper function

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ebb8d15f25777c4a11a940f92df5ddc6a4830117"><code>ebb8d15</code></a> 🐛 restore the line break before the Koin quirks bullet</summary>

  > The library_entries filter note added in 46cfdc4 swallowed the newline, so
  > the Koin bullet rendered as part of it.

  </details>
- [`66bcc86`](https://github.com/gamerlogue/app/commit/66bcc868c823dd2e18be5b0309092cc8213ca97a) **services:** 📝 import the KDoc link targets and use American spelling

### ✅ Tests

- <details><summary><a href="https://github.com/gamerlogue/app/commit/0ceae7a97c59b034e415ce0db012423cfc8e14dd"><code>0ceae7a</code></a> ✅ Add Kotest unit tests and Android journey specs</summary>

  > Cover quickDraft and the add-to-library validation guard with Kotest, and add
  > android-cli journey specs for the discover, login-prompt and language flows.

  </details>
- [`1263822`](https://github.com/gamerlogue/app/commit/1263822365ad8f4897d1dfacf4d5186863b8fdd8) ✅ Cover WebView bridge ref JSON parsing
- <details><summary><a href="https://github.com/gamerlogue/app/commit/4f06d4f06ecb6ce396f46c687e64b3d3eb9a1d50"><code>4f06d4f</code></a> ✅ follow ErrorState.error becoming a list</summary>

  > Missed in d603e48: the assertion still read the single-error field, so the
  > test source did not compile against the new state.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/19cbcbe8f0f8729d7eadf1c493a05145143e146e"><code>19cbcbe</code></a> <b>services:</b> ✅ pin the connectors&#x27; store-URL regexes</summary>

  > uidFromUrl is what turns an IGDB external_games/websites URL back into a
  > store id for the wishlist push, so when a store changes its URL shape the
  > push silently stops finding anything — no error, no log. The package had no
  > test at all beyond parseRefsJson.
  >
  > Covers the Steam storeUrl/uidFromUrl round-trip, the GOG, Epic and Nintendo
  > page shapes (with and without a locale segment, plus a non-product URL that
  > must not match), Epic's push-URL normalisation, platformIdsFor for both a PC
  > store and a console family, and parseProfileJson's empty-username rule.

  </details>
- [`e637e3a`](https://github.com/gamerlogue/app/commit/e637e3a501661fcea4532f8620e6fa4d4ed9fb7f) 🔥 drop unused imports and a redundant type argument
- <details><summary><a href="https://github.com/gamerlogue/app/commit/63df146a9299d9265e58b7ab7306e4c741dc1e0f"><code>63df146</code></a> 🔥 suppress &quot;CanBeVal&quot; warnings in test files</summary>

  > - Added @Suppress("CanBeVal") annotations to prevent warnings for mutable variables in test cases.
  > - Ensures cleaner test code without unnecessary warnings.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/cddea76ee22b2bbd7bcd06f455aff4fb66031b2a"><code>cddea76</code></a> <b>library:</b> ✅ use a released game in the missing-status save test</summary>

  > Since unreleased games preselect the backlog, a game with no release date never lacks a
  > status, so the test could not reach the error it asserts.

  </details>

### 👷 Building scripts changes

- <details><summary><a href="https://github.com/gamerlogue/app/commit/6cc827202424f72902ef0ba1b13d91296208a4a7"><code>6cc8272</code></a> 🔧 Add Sonatype snapshots repository</summary>

  > Adds the Sonatype snapshots repository (`https://central.sonatype.com/repository/maven-snapshots/`) to the project's dependency resolution management.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/9a1ed5d7a9678dcbcb36e5002a8a852618d7f764"><code>9a1ed5d</code></a> ⚙️ Centralize Android SDK versions in gradle.properties</summary>

  > Moves Android SDK versions (`compileSdk`, `minSdk`, `targetSdk`) into `gradle.properties` for centralized management. The `build.gradle.kts` files in `sharedUI` and `androidApp` are updated to consume these properties.
  >
  > This also raises the minimum SDK version from 23 to 24 in `sharedUI` to align with the new property.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/95b97b7ccc8f0626ca3bf2d70cc1efa2bd53f5c5"><code>95b97b7</code></a> 🏗️ Increase minSdk to 26</summary>

  > Increases the `androidMinSdk` from 24 to 26 in `gradle.properties`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/1b4af82e900a1d25daa50d38544f42eb81850970"><code>1b4af82</code></a> ⚙️ Add collection literals compiler argument</summary>

  > Adds the `-Xcollection-literals` flag to the Kotlin compiler options in the `sharedUI` module.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/bb7c10b06ad3cba336cc69580fe8abd27a717132"><code>bb7c10b</code></a> 🔧 Set up Kotest tests and tidy build config</summary>

  > Add Kotest (JUnit5 runner + assertions) and coroutines-test to jvmTest,
  > enable the JUnit Platform, and drop the redundant -Xexplicit-backing-fields
  > flag (stable in Kotlin 2.4). Remove test dependencies added but left unused.

  </details>
- [`967c161`](https://github.com/gamerlogue/app/commit/967c161cf65ba0305cbc03684e98261d0832ad9d) **deps:** ⬆️ update project dependencies and gradle settings
- <details><summary><a href="https://github.com/gamerlogue/app/commit/383d1361d0f8b31f30b4163a0757184fe04e78f9"><code>383d136</code></a> ⬆️ update dependencies in libs.versions.toml</summary>

  > Bump versions for:
  > - agp: 9.3.0 -> 9.3.1
  > - compose-hot-reload: 1.2.0-rc01 -> 1.2.0
  > - logbackClassic: 1.5.38 -> 1.6.0

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/54524481c70a21968e253ff1dd9653fff244eac5"><code>5452448</code></a> ⚙️ enable native access JVM argument for desktop app</summary>

  > - Add `--enable-native-access=ALL-UNNAMED` to Compose desktop application `jvmArgs`
  > - Configure all `JavaExec` tasks in `desktopApp/build.gradle.kts` to include the `--enable-native-access=ALL-UNNAMED` JVM flag

  </details>
- [`9328fbc`](https://github.com/gamerlogue/app/commit/9328fbc66caea2f9e4ae63e61c1f8150d47c1090) ⬆️ bump Compose Multiplatform, Ktor, Koin plugin and Navigation 3
- <details><summary><a href="https://github.com/gamerlogue/app/commit/b03e5db376564c97b245cf2b80e24f11493d5e6b"><code>b03e5db</code></a> ➕ generate the filter, sort and company icons</summary>

  > Adds filter_list, sort, tune, arrow_downward, arrow_upward and domain to
  > the SymbolCraft set, used by the game list filter sheet.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8a08566dc46fe32974125da597c37ff5d06f72c3"><code>8a08566</code></a> <b>deps:</b> ➕ add java-keyring and ktor-client-mock</summary>

  > java-keyring (jvmMain) backs the desktop session token with the OS credential
  > store; ktor-client-mock (jvmTest) is needed to exercise the HTTP client plugins
  > without a network.

  </details>
- [`0043e49`](https://github.com/gamerlogue/app/commit/0043e49aa815373a3a7d9541e5a63e8de462e1ac) **android:** 🔧 grant local network access on debug installs for API 36+
- [`9249a45`](https://github.com/gamerlogue/app/commit/9249a4595ce98465bd6030185324eec8989817da) **deps:** ➕ add okio dependency
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7da2c68267cc769d16cb37d4827a61f7349a0488"><code>7da2c68</code></a> <b>android:</b> 🔧 declare app_name via resValue and remove Compose resource directory mapping</summary>

  > - Add `app_name` as an Android `resValue` for use by platform XML resources like `authenticator.xml`
  > - Remove `sourceSets` mapping that linked shared UI Compose resources into Android resources

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c52b3a3b7f94b8da030a266456553755d4fc28d3"><code>c52b3a3</code></a> <b>sharedUI:</b> 🔧 disable Koin compile safety for JVM test compilation</summary>

  > - Disable Koin compiler plugin `compileSafety` for the `compileTestKotlinJvm` task to work around test definition visibility issues
  > - Update comments in `HttpCacheAuthTest` and `PlatformModuleBindingTest` to explain runtime resolution
  > - Add task description to `applyNodePolyfillPlugin` in Gradle configuration

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/fabefa3b0e60a0a73d3bdf78d07df935434299b2"><code>fabefa3</code></a> <b>deps:</b> Bump the github-actions group with 5 updates</summary>

  > Bumps the github-actions group with 5 updates:
  >
  > | Package | From | To |
  > | --- | --- | --- |
  > | [actions/checkout](https://github.com/actions/checkout) | `4` | `7` |
  > | [actions/setup-java](https://github.com/actions/setup-java) | `4` | `6` |
  > | [actions/upload-artifact](https://github.com/actions/upload-artifact) | `4` | `7` |
  > | [actions/download-artifact](https://github.com/actions/download-artifact) | `4` | `8` |
  > | [softprops/action-gh-release](https://github.com/softprops/action-gh-release) | `1` | `3` |
  >
  >
  > Updates `actions/checkout` from 4 to 7
  > - [Release notes](https://github.com/actions/checkout/releases)
  > - [Changelog](https://github.com/actions/checkout/blob/main/CHANGELOG.md)
  > - [Commits](https://github.com/actions/checkout/compare/v4...v7)
  >
  > Updates `actions/setup-java` from 4 to 6
  > - [Release notes](https://github.com/actions/setup-java/releases)
  > - [Commits](https://github.com/actions/setup-java/compare/v4...v6)
  >
  > Updates `actions/upload-artifact` from 4 to 7
  > - [Release notes](https://github.com/actions/upload-artifact/releases)
  > - [Commits](https://github.com/actions/upload-artifact/compare/v4...v7)
  >
  > Updates `actions/download-artifact` from 4 to 8
  > - [Release notes](https://github.com/actions/download-artifact/releases)
  > - [Commits](https://github.com/actions/download-artifact/compare/v4...v8)
  >
  > Updates `softprops/action-gh-release` from 1 to 3
  > - [Release notes](https://github.com/softprops/action-gh-release/releases)
  > - [Changelog](https://github.com/softprops/action-gh-release/blob/master/CHANGELOG.md)
  > - [Commits](https://github.com/softprops/action-gh-release/compare/v1...v3)
  >
  > ---
  > updated-dependencies:
  > - dependency-name: actions/checkout
  >   dependency-version: '7'
  >   dependency-type: direct:production
  >   update-type: version-update:semver-major
  >   dependency-group: github-actions
  > - dependency-name: actions/setup-java
  >   dependency-version: '6'
  >   dependency-type: direct:production
  >   update-type: version-update:semver-major
  >   dependency-group: github-actions
  > - dependency-name: actions/upload-artifact
  >   dependency-version: '7'
  >   dependency-type: direct:production
  >   update-type: version-update:semver-major
  >   dependency-group: github-actions
  > - dependency-name: actions/download-artifact
  >   dependency-version: '8'
  >   dependency-type: direct:production
  >   update-type: version-update:semver-major
  >   dependency-group: github-actions
  > - dependency-name: softprops/action-gh-release
  >   dependency-version: '3'
  >   dependency-type: direct:production
  >   update-type: version-update:semver-major
  >   dependency-group: github-actions
  > ...

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/0e69ac23aab702c1a3ed9d7ef871e064269e2d4f"><code>0e69ac2</code></a> <b>deps:</b> Bump the gradle group with 7 updates</summary>

  > Bumps the gradle group with 7 updates:
  >
  > | Package | From | To |
  > | --- | --- | --- |
  > | org.jetbrains.compose.material3:material3 | `1.12.0-alpha03` | `1.13.0-alpha01` |
  > | org.jetbrains.compose.material3:material3-adaptive-navigation-suite | `1.12.0-alpha03` | `1.13.0-alpha01` |
  > | [io.github.panpf.sketch4:sketch-compose](https://github.com/panpf/sketch) | `4.6.0` | `4.7.0` |
  > | [io.github.panpf.sketch4:sketch-http](https://github.com/panpf/sketch) | `4.6.0` | `4.7.0` |
  > | [uk.uuid.slf4j:slf4j-android](https://github.com/nomis/slf4j-android) | `2.0.19-0` | `2.0.20-0` |
  > | [io.github.panpf.zoomimage:zoomimage-compose](https://github.com/panpf/zoomimage) | `1.6.0` | `1.7.0` |
  > | [com.github.gmazzo.buildconfig](https://github.com/gmazzo/gradle-buildconfig-plugin) | `6.1.1` | `6.1.2` |
  >
  >
  > Updates `org.jetbrains.compose.material3:material3` from 1.12.0-alpha03 to 1.13.0-alpha01
  >
  > Updates `org.jetbrains.compose.material3:material3-adaptive-navigation-suite` from 1.12.0-alpha03 to 1.13.0-alpha01
  >
  > Updates `org.jetbrains.compose.material3:material3-adaptive-navigation-suite` from 1.12.0-alpha03 to 1.13.0-alpha01
  >
  > Updates `io.github.panpf.sketch4:sketch-compose` from 4.6.0 to 4.7.0
  > - [Release notes](https://github.com/panpf/sketch/releases)
  > - [Changelog](https://github.com/panpf/sketch/blob/main/CHANGELOG.md)
  > - [Commits](https://github.com/panpf/sketch/compare/4.6.0...4.7.0)
  >
  > Updates `io.github.panpf.sketch4:sketch-http` from 4.6.0 to 4.7.0
  > - [Release notes](https://github.com/panpf/sketch/releases)
  > - [Changelog](https://github.com/panpf/sketch/blob/main/CHANGELOG.md)
  > - [Commits](https://github.com/panpf/sketch/compare/4.6.0...4.7.0)
  >
  > Updates `io.github.panpf.sketch4:sketch-http` from 4.6.0 to 4.7.0
  > - [Release notes](https://github.com/panpf/sketch/releases)
  > - [Changelog](https://github.com/panpf/sketch/blob/main/CHANGELOG.md)
  > - [Commits](https://github.com/panpf/sketch/compare/4.6.0...4.7.0)
  >
  > Updates `uk.uuid.slf4j:slf4j-android` from 2.0.19-0 to 2.0.20-0
  > - [Commits](https://github.com/nomis/slf4j-android/compare/slf4j-android-2.0.19-0...slf4j-android-2.0.20-0)
  >
  > Updates `io.github.panpf.zoomimage:zoomimage-compose` from 1.6.0 to 1.7.0
  > - [Release notes](https://github.com/panpf/zoomimage/releases)
  > - [Changelog](https://github.com/panpf/zoomimage/blob/main/CHANGELOG.md)
  > - [Commits](https://github.com/panpf/zoomimage/compare/1.6.0...1.7.0)
  >
  > Updates `com.github.gmazzo.buildconfig` from 6.1.1 to 6.1.2
  > - [Release notes](https://github.com/gmazzo/gradle-buildconfig-plugin/releases)
  > - [Commits](https://github.com/gmazzo/gradle-buildconfig-plugin/compare/v6.1.1...v6.1.2)
  >
  > ---
  > updated-dependencies:
  > - dependency-name: org.jetbrains.compose.material3:material3
  >   dependency-version: 1.13.0-alpha01
  >   dependency-type: direct:production
  >   update-type: version-update:semver-minor
  >   dependency-group: gradle
  > - dependency-name: org.jetbrains.compose.material3:material3-adaptive-navigation-suite
  >   dependency-version: 1.13.0-alpha01
  >   dependency-type: direct:production
  >   update-type: version-update:semver-minor
  >   dependency-group: gradle
  > - dependency-name: org.jetbrains.compose.material3:material3-adaptive-navigation-suite
  >   dependency-version: 1.13.0-alpha01
  >   dependency-type: direct:production
  >   update-type: version-update:semver-minor
  >   dependency-group: gradle
  > - dependency-name: io.github.panpf.sketch4:sketch-compose
  >   dependency-version: 4.7.0
  >   dependency-type: direct:production
  >   update-type: version-update:semver-minor
  >   dependency-group: gradle
  > - dependency-name: io.github.panpf.sketch4:sketch-http
  >   dependency-version: 4.7.0
  >   dependency-type: direct:production
  >   update-type: version-update:semver-minor
  >   dependency-group: gradle
  > - dependency-name: io.github.panpf.sketch4:sketch-http
  >   dependency-version: 4.7.0
  >   dependency-type: direct:production
  >   update-type: version-update:semver-minor
  >   dependency-group: gradle
  > - dependency-name: uk.uuid.slf4j:slf4j-android
  >   dependency-version: 2.0.20-0
  >   dependency-type: direct:production
  >   update-type: version-update:semver-patch
  >   dependency-group: gradle
  > - dependency-name: io.github.panpf.zoomimage:zoomimage-compose
  >   dependency-version: 1.7.0
  >   dependency-type: direct:production
  >   update-type: version-update:semver-minor
  >   dependency-group: gradle
  > - dependency-name: com.github.gmazzo.buildconfig
  >   dependency-version: 6.1.2
  >   dependency-type: direct:production
  >   update-type: version-update:semver-patch
  >   dependency-group: gradle
  > ...

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f3edd6126c613c58b8414718aad431ecce72685a"><code>f3edd61</code></a> <b>deps:</b> 🔧 bump logbackClassic to version 1.6.5 and spraypaintkt to version 3.0.0-rc2</summary>

  > - Updated dependencies to ensure compatibility and access to the latest features and fixes.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/bf626a5a051cafc58371eae11d988e9f3eab6364"><code>bf626a5</code></a> <b>deps:</b> 🔧 bump Compose Multiplatform, Material3, Nav3, and spraypaintkt versions</summary>

  > - Updated `compose-multiplatform` to 1.13.0-alpha02 and `compose-hot-reload` to 1.3.0-alpha02
  > - Updated `material3` to 1.13.0-alpha02 and `material3AdaptiveNav3` to 1.3.0
  > - Updated `nav3Ui` to 1.2.0-rc01
  > - Updated `spraypaintkt` to 3.0.0-rc3

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/087814091db96b4b82e4770f9860b7941cff9ec1"><code>0878140</code></a> <b>sharedUI:</b> ➖ drop the deprecated compose.desktop.currentOs</summary>

  > compose.ui already brings the Skiko runtime for every desktop OS (skiko-awt-runtime-all).

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5d0e49fe55c893bb76a2feb4a6a81761198eac38"><code>5d0e49f</code></a> <b>sharedUI:</b> 🔧 declare a JS executable so the JS tests can run</summary>

  > Compose 1.13's checkComposeUiTestConfigurationForJs fails the JS tests when they reach
  > Skiko without an executable binary to bundle its runtime (CMP-4906).

  </details>

### 👷 CI changes

- <details><summary><a href="https://github.com/gamerlogue/app/commit/b3700149e18630d106b0dc3fa81e047f0989b7e5"><code>b370014</code></a> ⚙️ Add GitHub workflow for multiplatform builds and releases</summary>

  > Adds a new GitHub Actions workflow (`build_release.yml`) to automate the building, signing, and releasing of the Android, Desktop, and Web applications.
  >
  > This includes:
  > - **Android**:
  >   - Builds a debug APK on every push.
  >   - Builds, signs, and uploads a release AAB/APK for GitHub releases.
  >   - Automatically publishes the release bundle to the Play Store.
  >   - Adds release signing configuration to `androidApp/build.gradle.kts`.
  >
  > - **Desktop**:
  >   - Builds native packages for Linux (.deb), macOS (.dmg), and Windows (.exe) on each push.
  >   - Replaces the `.msi` target with `.exe` for Windows distributions.
  >   - Uploads the generated desktop artifacts.
  >
  > - **Web**:
  >   - Builds the production web distribution.
  >   - Deploys the web app to GitHub Pages upon a new release.
  >
  > The workflow is triggered by pushes to any branch, new published releases, and can also be run manually.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f415243029a545c434f280d6930426bbc7a9e728"><code>f415243</code></a> ⚙️ Ensure release assets are uploaded even if some builds fail</summary>

  > Updates the `build_release.yml` workflow to make the `release_assets` job run even if one of the platform-specific build jobs (Android, Desktop, Web) fails.
  >
  > The job will now proceed as long as it's a `release` event and at least one of the build jobs has succeeded. This prevents a complete failure of the release process if only a single platform's build is unsuccessful.

  </details>
- [`9376db1`](https://github.com/gamerlogue/app/commit/9376db1a1a289d7e6893a6a3c42e8be2db0adc2a) Set package ecosystem to 'gradle' in dependabot.yml
- [`632eea9`](https://github.com/gamerlogue/app/commit/632eea9970394f1296fa8c9d43818e7da2b2d6ce) Add GitHub Actions to Dependabot configuration
- [`89beff5`](https://github.com/gamerlogue/app/commit/89beff5f145aad0a3272bf10aeb1ecf2ba362b92) ⬆️ update JDK version from 21 to 25 in build_release workflow
- <details><summary><a href="https://github.com/gamerlogue/app/commit/49775e2f208bf143aad89929867c1fb0d7d8d81a"><code>49775e2</code></a> ✨ update permissions for GitHub Actions workflows</summary>

  > - Added read permission for contents in the main workflow
  > - Set write permission for contents in web_build and release_assets jobs
  > - Ensures proper access levels for build and release processes

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/b825ec36a8258f64f5e0abc31b63c6a30d9af3de"><code>b825ec3</code></a> <b>release:</b> 👷 cut releases from CI with the changelog commit tagged</summary>

  > A dispatch with a version commits the regenerated CHANGELOG.md, tags
  > that commit, creates the release with the version's notes and builds
  > from the tag. A release created with GITHUB_TOKEN triggers no
  > workflow, so the builds run in the same run and publishing a release
  > from the GitHub UI no longer does anything.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d858a80362294f12da5b51cad0b971d14d495b6e"><code>d858a80</code></a> ♻️ split the builds into a reusable workflow</summary>

  > build.yml builds every platform and is called by ci.yml on each push
  > and by release.yml on the release tag. It stays read-only, since a
  > called workflow can't get more permissions than its caller, so GitHub
  > Pages and Play Store publishing move to release.yml jobs that use the
  > build artifacts.

  </details>
- [`6f7bff4`](https://github.com/gamerlogue/app/commit/6f7bff42473b882281b51863e67d82cef4fca731) **release:** 🚧 disable Play Store publishing until the app is ready
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8f9fbe1bb871a711ab02434544f4855ce59e6405"><code>8f9fbe1</code></a> <b>release:</b> 👷 compute the version from the commits when none is given</summary>

  > git cliff --bumped-version picks the next version from the conventional
  > commits since the latest tag, which must be a full x.y.z. In 0.x a
  > breaking change bumps the minor, so 1.0.0 stays an explicit choice.
  > git-cliff is now installed and run directly, since the action can't
  > feed a computed version to the next steps.

  </details>

### Other changes

- <details><summary><a href="https://github.com/gamerlogue/app/commit/616c8bf10a0bdee0535b9451521f678555e62991"><code>616c8bf</code></a> <b>deps:</b> ⬆️ Bump dependencies</summary>

  > Updates various dependencies to their latest versions:
  >
  > - AGP: 8.13.1 -> 8.13.2
  > - Activity Compose: 1.12.0 -> 1.12.1
  > - UI Test: 1.9.5 -> 1.10.0
  > - BuildConfig: 6.0.1 -> 6.0.6
  > - Compose: 1.10.0-beta02 -> 1.10.0-rc02
  > - KSP: 2.3.0 -> 2.3.3
  > - Ktor: 3.3.2 -> 3.3.3
  > - Lifecycle ViewModel Nav3: 2.10.0-alpha05 -> 2.10.0-alpha06
  > - MaterialKolor: 5.0.0-alpha02 -> 5.0.0-alpha04
  > - Nav3 UI: 1.0.0-alpha05 -> 1.0.0-alpha06
  >
  > The `animation` library is now aligned with the `compose` version.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/76aef01f5e43b49af3b7211737669d0aef4c0f95"><code>76aef01</code></a> <b>deps:</b> 🪵 Add Logback dependency for desktop</summary>

  > Adds the `ch.qos.logback:logback-classic` dependency, version 1.5.22, specifically for the desktop (`jvm`) target. This is to provide logging capabilities for the desktop application.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2fe672f5a00cc856cd37465ce397ac31b2a4ad9a"><code>2fe672f</code></a> <b>deps:</b> ⬆️ Update dependencies and Gradle version</summary>

  > Updates various dependencies to their latest versions and bumps the Gradle wrapper.
  >
  > **Dependency Updates:**
  > - Kotlin: `2.2.21` -> `2.3.0`
  > - KSP: `2.3.3` -> `2.3.4`
  > - AndroidX Activity Compose: `1.12.1` -> `1.12.2`
  > - Lifecycle ViewModel: `2.10.0-alpha06` -> `2.10.0-alpha07`
  > - Material 3 Adaptive Nav: `1.3.0-alpha02` -> `1.3.0-alpha03`
  > - Other minor version bumps for `buildConfig`, `composeHotReload`, `logbackClassic`, and `symbolCraft`.
  >
  > **Build & Tooling:**
  > - Gradle Wrapper upgraded from `9.0.0` to `9.1.0`.
  > - Adds the `spraypaintkt.processor` KSP dependency to the `commonMain` source set.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/9d38faaee943ee243597ef2a51a72b85758d86ca"><code>9d38faa</code></a> 🎨 Update placeholder URL for platform logos</summary>

  > Changes the placeholder image URL in the `PlatformImages` composable from "https://placehold.net/64x64.png" to "https://placehold.net/default.png".

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8f1e305dd6e3196fe9e4958b252dc8b687f0fd0c"><code>8f1e305</code></a> <b>deps:</b> 🪵 Add SLF4J logging for Android</summary>

  > Adds the SLF4J API (`org.slf4j:slf4j-api`) and its Android implementation (`uk.uuid.slf4j:slf4j-android`) as dependencies for the `androidMain` source set. This provides a logging framework for the Android application.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/190ad6924b241e22fa3530e97ffb6ad6ea3db34a"><code>190ad69</code></a> <b>deps:</b> ⬆️ Update mp-stools and spraypaintkt dependencies</summary>

  > Updates the versions of two dependencies:
  > - `mp_stools` is upgraded from `1.6.2` to `1.6.3`.
  > - `spraypaintkt` is updated from `2.3.0` to a snapshot version (`247280e-SNAPSHOT`).

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/16372d9eb9254aaa49d8da30282fc97d49d6dcac"><code>16372d9</code></a> <b>logging:</b> 🪵 Integrate Kermit for logging</summary>

  > Integrates the Kermit logging library across the application for more structured and configurable logging.
  >
  > This includes:
  > - Setting the minimum log severity to `Verbose` when running in the `LOCAL` environment.
  > - Routing the Ktor `HttpClient`'s logging output through Kermit.
  > - Adding various log statements to track application lifecycle events, such as AuthState changes.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/782329c2ade1e8bde97e61af0c3f196415f4d8a3"><code>782329c</code></a> ⚙️ Add project-specific IDE settings</summary>

  > Adds various IntelliJ IDEA/Android Studio configuration files to the `.idea` directory to ensure consistent project settings for developers.
  >
  > This includes:
  > - A `.gitignore` for the `.idea` directory itself.
  > - Inspection profiles (`Project_Default.xml`) to enforce code style and best practices.
  > - Run configuration and deployment target settings.
  > - Configuration for Detekt (`detekt.xml`).
  > - General project system, Kotlin compiler, and editor settings.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d6942c9185ceb8621d283dedc283c86ec581786e"><code>d6942c9</code></a> <b>deps:</b> ⬆️ Update dependencies and Gradle wrapper</summary>

  > Updates several dependencies and the Gradle wrapper version.
  >
  > - Bumps `ch.qos.logback:logback-classic` from 1.5.23 to 1.5.24.
  > - Bumps `io.github.goooler.shadow.git-semantic-versioning` from 5.0.5 to 5.0.6.
  > - Upgrades Gradle from version 9.1.0 to 9.2.1.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/96a712aff5eee952a9e19ab2b123e2d9ee2b5ae3"><code>96a712a</code></a> <b>deps:</b> ⬆️ Update AGP, Compose, and other dependencies</summary>

  > Updates several key dependencies to their newer versions:
  > - Android Gradle Plugin (AGP) to `9.0.0`
  > - Compose Multiplatform to `1.11.0-alpha01`
  > - Compose Hot Reload to `1.1.0-alpha04`
  > - Other minor version bumps
  >
  > This also includes migrating the build scripts to be compatible with AGP 9.0.0 by removing the explicit `kotlin-android` plugin and updating how `composeResources` and the `android` namespace are configured.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/fd720f21c2a7d68d39dfa69726d5422428e761b6"><code>fd720f2</code></a> <b>deps:</b> 🪵 Bump dependency versions</summary>

  > Updates several dependencies to their latest versions:
  > - `git-semantic-versioning` to `5.0.7`
  > - `kotlinx-serialization` to `1.10.0`
  > - `ktor` to `3.4.0`
  > - `logbackClassic` to `1.5.25`

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c938c3f1f9d02eb55280ffebed5b755ec61ec069"><code>c938c3f</code></a> <b>deps:</b> 🪵 Bump dependency versions and Gradle</summary>

  > Updates several dependencies to their latest versions and upgrades the Gradle wrapper.
  >
  > **Dependency Updates:**
  > - Android Gradle Plugin (AGP) to `9.0.1`
  > - Kotlin to `2.3.10`
  > - Compose Multiplatform to `1.11.0-alpha02`
  > - KSP to `2.3.6`
  > - `spraypaintkt` to `2.4.0`
  > - Various other AndroidX, Compose, and Material Design libraries.
  >
  > **New Dependencies:**
  > - `compose-webview` for web content display.
  > - `compose-settings` for building settings screens.
  > - `multiplatform-settings` coroutines and observable modules.
  >
  > **Other Changes:**
  > - Updates Gradle wrapper to `9.3.1`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/41e8fdac3f01cf2deea5899ddb4ff1a5dd711e62"><code>41e8fda</code></a> <b>deps:</b> 🪵 Bump dependency versions, Gradle, and Android SDK</summary>

  > Updates the project infrastructure, target SDK, and numerous dependencies to their latest versions.
  >
  > **Infrastructure and SDK Updates:**
  > - Gradle to `9.6.0`.
  > - Android `compileSdk` and `targetSdk` to `37`.
  > - Android Gradle Plugin (AGP) to `9.2.1`.
  >
  > **Core Dependency Updates:**
  > - Kotlin to `2.4.0`.
  > - Compose Multiplatform to `1.11.1`.
  > - Kotlinx Coroutines to `1.11.0`, Serialization to `1.11.0`, and Datetime to `0.8.0`.
  > - Koin to `4.2.2`.
  > - Ktor to `3.5.0`.
  > - KSP to `2.3.9`.
  >
  > **Library and UI Updates:**
  > - Navigation 3 (Nav3) components to `1.1.x` stable/beta.
  > - Material3 Adaptive to `1.3.0-beta02`.
  > - Compose Webview to `1.9.0`.
  > - Various updates to `sketch`, `kermit`, `symbolCraft`, and `compose-settings`.
  >
  > **Other Changes:**
  > - Refreshes `gradlew` and `gradlew.bat` scripts.
  > - Updates dependency reference in `sharedUI/build.gradle.kts` to use `libs.multiplatform.settings`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/537cc32e89e8adc5f1e2aa1f9c6b93db84d8f699"><code>537cc32</code></a> ⚙️ Update Kotlin version and IDE configurations</summary>

  > Updates the Kotlin compiler version and various IntelliJ/Android Studio project settings.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/e0601e575339867efe00cc693d449ff9a8e902bf"><code>e0601e5</code></a> ⚙️ Add MCP configuration</summary>

  > Adds a `.mcp.json` file to configure Model Context Protocol (MCP) servers for the project.
  >
  > **Configured Servers:**
  > - `compose-hot-reload`: Executes the `hotMcpServer` Gradle task.
  > - `android-studio`: Connects to a local Server-Sent Events (SSE) endpoint at `http://127.0.0.1:64342/sse`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ec12917642403f38bd58f0f8a7608834649cd4bf"><code>ec12917</code></a> ⚙️ Update deployment target selection</summary>

  > Updates the IDE configuration to persist the selected deployment target for the "main" run configuration.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7e68f9ab19a10467584f1a9c23eb51faf16fbfdc"><code>7e68f9a</code></a> ⚙️ Update Compose Multiplatform and Kotest dependencies</summary>

  > Updates key library versions in `libs.versions.toml` to their latest releases.
  >
  > **Dependency Updates:**
  > - **Compose Multiplatform:** Bumped from `1.11.1` to `1.12.0-alpha02`.
  > - **Kotest:** Bumped from `5.9.1` to `6.2.1`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/5d1040edb56c8297db6e3a1fc9533baf9b97a4d2"><code>5d1040e</code></a> 📖 Add CLAUDE.md project guide</summary>

  > Adds a comprehensive guide for Claude Code to understand the Gamerlogue project structure, technical architecture, and development conventions.
  >
  > **Project documentation includes:**
  > - **Overview:** Defines the Kotlin Multiplatform and Compose Multiplatform setup targeting Android, JVM Desktop, and JS Browser.
  > - **Build & Run:** Documents Gradle commands for platform-specific builds, hot reloading, and JVM-based unit testing.
  > - **Architecture:** Details the use of Koin for DI, `StateViewModel` for state management, and Navigation 3 for adaptive list-detail routing.
  > - **Data Layer:** Explains the JSON:API integration via SprayPaintKT, the IGDB metadata client, and the `safeRequest` network wrapper.
  > - **Linked Services:** Outlines the strategy for external store synchronization (Steam, PSN, Xbox, etc.) using WebView automation and specialized service connectors.
  > - **UI & Conventions:** Specifies coding standards for detekt, Material 3 Expressive components, and the SymbolCraft icon generation workflow.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/6c32ec9c4c8fa118b24944cbc3d46f1f9e828ce9"><code>6c32ec9</code></a> ⚙️ Enable Koin and Android IDE inspections</summary>

  > Updates the project's default inspection profile to include specialized checks for Koin dependency injection and general Android application issues.
  >
  > **Inspections enabled:**
  > - **Koin Validation:** Added `KoinApplicationValidation`, `KoinConfigurationValidation`, `KoinJsr330Validation`, `UndeclaredKoinUsage`, and `UnusedKoinDeclaration` to ensure DI health.
  > - **General:** Added `ApplicationIssue` and `ScopeArchetypeValidation` checks.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/d11c4f482c29bacf151c411bf05fd16bfa72551a"><code>d11c4f4</code></a> ⚙️ Add IDE configuration for Agent Bridge and MCP Server</summary>

  > Adds IntelliJ IDEA configuration files to manage settings for Agent Bridge memory and Model Context Protocol (MCP) server tools.
  >
  > **Agent Bridge:**
  > - Added `.idea/agentbridgeMemory.xml` to enable memory settings and backfill.
  >
  > **MCP Server:**
  > - Added `.idea/mcpServer.xml` with default configurations.
  > - Disabled specific tools: `set_theme`, `get_notifications`, `run_sonarqube_analysis`, `list_themes`, and `get_sonar_rule_description`.
  > - Enabled smooth scrolling for the MCP interface.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/3a57b20e820f5822c944a49f183d379389db2e34"><code>3a57b20</code></a> ⚙️ Enable auto-start for MCP server</summary>

  > Updates the IDE configuration to automatically start the Model Context Protocol (MCP) server.
  >
  > **IDE Configuration:**
  > - Sets `autoStart` to `true` in `.idea/mcpServer.xml` settings.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/cc18c5f79fc59bfbec0f4243d7ed5ff00d2b8b46"><code>cc18c5f</code></a> <b>deps:</b> ⬆️ Update navigation and UI dependencies</summary>

  > Updates several project dependencies to their latest beta and alpha versions in `libs.versions.toml`.
  >
  > **Updated Dependencies:**
  > - **Material 3:** Bumped from `1.9.0` to `1.12.0-alpha02`.
  > - **Nav3 UI:** Bumped from `1.1.1` to `1.2.0-alpha01`.
  > - **Lifecycle ViewModel Nav3:** Bumped from `2.10.0` to `2.11.0-beta02`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/05bae588af8902c4670fd82f978332890034c358"><code>05bae58</code></a> ⚙️ Update .gitignore</summary>

  > Adds `.idea/deploymentTargetSelector.xml` to the ignored files to prevent tracking local Android Studio deployment settings.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/97b9a8d58452fbaaff50ba100c2e9ddc44493801"><code>97b9a8d</code></a> ⚙️ Remove Android Studio MCP configuration</summary>

  > Removes the `android-studio` server entry from the Model Context Protocol (MCP) configuration.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/c16ad834e12ea1df319383ab706dce1988e40ce6"><code>c16ad83</code></a> ⚙️ Update MCP configuration to use Node.js wrapper</summary>

  > Updates the `compose-hot-reload` MCP server configuration to use a Node.js wrapper script. This ensures better process management and filters the standard output to maintain MCP protocol integrity by preventing non-JSON Gradle logs from polluting the communication channel.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/aeb555437a4f3e8efe9c182fcd096562c5c38bc6"><code>aeb5554</code></a> ⬆️ Update dependencies and Gradle wrapper</summary>

  > Updates Gradle to 9.6.1 and bumps various library versions, most notably moving Compose Multiplatform to the `1.12.0-beta01` release.
  >
  > **Dependency updates:**
  > - **Build:** Upgraded Gradle wrapper to `9.6.1`.
  > - **Compose Multiplatform:** Updated `compose-multiplatform` and `compose-hot-reload` to `1.12.0-beta01`.
  > - **Navigation & Lifecycle:** Updated `nav3-core`, `nav3-ui`, and `lifecycle-viewmodel-nav3` (promoted to `rc01`).
  > - **UI & Image Handling:** Bumped `material3` (`alpha03`), `sketch` (`4.5.0`), and `zoomimage-compose` (`1.5.0`).
  > - **Networking & Utilities:** Updated `ktor` to `3.5.1` and `logback-classic` to `1.5.37`.
  > - **Testing:** Updated `androidx-uiTest` to `1.11.4`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/f16b583f96f4383aa1d1b13e8033d5af495d12b0"><code>f16b583</code></a> ⚙️ Configure MCP server port</summary>

  > Updates the IDE configuration to specify a dedicated port for the Model Context Protocol (MCP) server.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2a5840b827e85aaac4af13735206817ca418649d"><code>2a5840b</code></a> ⚙️ Automate adb reverse for local backend access</summary>

  > Adds a custom Gradle task to automatically establish an `adb reverse` tunnel after debug installations, ensuring the Android emulator can reach a local backend on `localhost:80`.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/89cd49bf843191b5954186a001f8970b011fb957"><code>89cd49b</code></a> ⚙️ Add Android run configuration</summary>

  > Adds a shared run configuration for the Android application module in IntelliJ/Android Studio.
  >
  > **Configuration details:**
  > - **Run Configuration:** Created a new `main` Android App configuration for the `Gamerlogue.androidApp` module.
  > - **Pre-run Tasks:** Includes a custom Gradle task `:androidApp:adbReverseLocalhost` to automatically handle ADB port forwarding before deployment.
  > - **Settings:** Configures standard deployment, debugging, and logcat defaults.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7e009d92b06224ee193ce1e78296613cd08d76e1"><code>7e009d9</code></a> ⚙️ Enable host tests in sharedUI</summary>

  > Adds the `withHostTest` configuration to the `sharedUI` build script to support host-side unit testing within the Kotlin Multiplatform configuration.

  </details>
- [`492a4a9`](https://github.com/gamerlogue/app/commit/492a4a9245d7266f1abd65e0ebda4191dc0ddb14) **config:** 🔧 update IDE inspection profile and conventional commit settings
- [`948b745`](https://github.com/gamerlogue/app/commit/948b74585e65229792a70bc3c3cd7b7e188ca018) **deps:** 🔧 bump library versions and Gradle wrapper to 9.7.0
- [`0346940`](https://github.com/gamerlogue/app/commit/0346940283f97cf3d71d93181c10e5c70faae3bc) Update IDE project settings configuration
- [`b37d148`](https://github.com/gamerlogue/app/commit/b37d148a66ac24b60c66bc3064789c65609d80b2) **deps:** ⬆️ bump Gradle wrapper to 9.7.1
- [`b28bfbf`](https://github.com/gamerlogue/app/commit/b28bfbf09ab86e9ecbf62dcc3bd8b5603bfecb69) **deps:** 🔧 bump AGP to 9.3.2 and Compose Multiplatform to 1.12.0
- [`0a23678`](https://github.com/gamerlogue/app/commit/0a2367805f790bc32d8b91a9228135bfe48ede4f) **detekt:** 🔧 allow UPPER_SNAKE_CASE constants and increase destructuring limit
- [`8ec02ab`](https://github.com/gamerlogue/app/commit/8ec02abd7c37d66ffb3cc77e18faf2de8bb74299) **ide:** ⚙️ update MCP server configuration
- [`ffb3be8`](https://github.com/gamerlogue/app/commit/ffb3be8a69a516309044cff4caa32a678089685e) Chore(deps): ⬆️ update Gradle wrapper, Kotlin, and dependencies
- [`5110898`](https://github.com/gamerlogue/app/commit/5110898909f1b63c9f000a83caa170e6f5f64ebe) **mcp:** 🔧 update server port to 8643 and enable static port
- <details><summary><a href="https://github.com/gamerlogue/app/commit/7d0bd296b1a0fcd3a4b333c1650381cccb72fdd0"><code>7d0bd29</code></a> <b>android:</b> 🎨 update monochrome launcher icons and cleanup unused icon assets</summary>

  > - Update `ic_launcher_monochrome.png` across all mipmap density buckets
  > - Remove unused launcher background resources for alpha, beta, and dev variants
  > - Delete obsolete `gen_monochrome_icons.py` script

  </details>
- [`8a50ee6`](https://github.com/gamerlogue/app/commit/8a50ee6fdbd99edf8e130934abf6d1dda93a8db4) **ide:** 🔧 add "Project files owned" custom scope
- <details><summary><a href="https://github.com/gamerlogue/app/commit/3378a8f4c2ef3d81c8bb93a8f3637fc3e7a1eb74"><code>3378a8f</code></a> <b>ide:</b> ⚙️ enable showReviewInEditor in mcpServer config</summary>

  > - Added the `showReviewInEditor` option with value `true` in `.idea/mcpServer.xml`.

  </details>
- [`ab19e64`](https://github.com/gamerlogue/app/commit/ab19e6428924917d1fa46c3ba0987340a72018ae) **gradle:** 🔧 update androidCompileSdkMinor to 2
- <details><summary><a href="https://github.com/gamerlogue/app/commit/a8bb42d4ded8908e8bf238ec5b51a7a9c149a1e9"><code>a8bb42d</code></a> <b>library:</b> 🔥 remove strings left unused by the section redesign</summary>

  > The "All" status filter and the per-status empty messages went away when
  > the library became Discover-style sections.

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/8c2890d93e4dbc891961cef884204ee0d7c06b2d"><code>8c2890d</code></a> 🔧 update Kotlin compiler settings and remove unused markdown configuration</summary>

  > - Added JVM target version 1.8 to Kotlin compiler arguments for compatibility.
  > - Removed obsolete markdown.xml configuration file to clean up the project.

  </details>
- [`7df2573`](https://github.com/gamerlogue/app/commit/7df25734e5a64030d7ec1fd15482ecde78684a3f) 🔧 add code style configuration for project
- <details><summary><a href="https://github.com/gamerlogue/app/commit/393b319f95e412af00392b810e35f2ff34783b3e"><code>393b319</code></a> 🔧 update .gitignore to exclude additional build artifacts</summary>

  > - Added `.agent-work` and `androidApp/*/release/mapping.txt` to ignore list
  > - Prevents unnecessary files from being tracked in the repository

  </details>
- <details><summary><a href="https://github.com/gamerlogue/app/commit/2580c2bd7e4d215bebcea11ec91588bbf0c3ccd9"><code>2580c2b</code></a> 🔧 update Gradle and library versions</summary>

  > - Upgraded Gradle from 9.8.0 to 9.8.1 for improved performance and stability.
  > - Updated compose-hot-reload to version 1.3.0-beta01 for new features and fixes.
  > - Bumped git-semantic-versioning-base to 7.0.25 for compatibility improvements.
  > - Updated Kotlin version from 2.4.20 to 2.4.21 to leverage the latest enhancements.

  </details>
- [`afee9d2`](https://github.com/gamerlogue/app/commit/afee9d251cd27587ff913d9caf0bf2debe42c5b5) **build:** 🔥 drop unused imports from the build scripts
- [`01757b5`](https://github.com/gamerlogue/app/commit/01757b5aff7f7db5e397aea9e570ef4e12b9c381) **i18n:** 🚨 suppress HttpUrlsUsage on the server address hint
- [`2004294`](https://github.com/gamerlogue/app/commit/20042948511f590cd90c596ff0500b1a86c7cab2) Chore(deps): 🔧 update compose-settings dependency to version 3.3.0
- <details><summary><a href="https://github.com/gamerlogue/app/commit/ccf3aa93a63759acb8a5334ac191b4b91ad646c0"><code>ccf3aa9</code></a> <b>changelog:</b> 🔧 configure git-cliff for the changelog</summary>

  > The format follows the former chglog one (anchor, compare link, release
  > date, gitmoji groups, linked short hash), with the commit body collapsed
  > in a blockquote under the entry. Gitmoji placed before the type by older
  > commits are moved after the colon so those commits get grouped too.
  > docs(changelog) commits, which update CHANGELOG.md, are left out.

  </details>
- [`766cabe`](https://github.com/gamerlogue/app/commit/766cabe8e784b381f248b2b9ce96ba0f03929adc) **schema:** 🔧 add JSON schema configuration for git-cliff

### ⏪ Reverts

- [`e94659d`](https://github.com/gamerlogue/app/commit/e94659df6729ab9c4edc0e6b84de462c307044b5) Revert "refactor: 🛠️ Switch to mutable UI state and in-place updates"

### 🔀 Pull Requests

- [`fd38918`](https://github.com/gamerlogue/app/commit/fd3891845285151cc8005a4cc218c31fdb93790e) Merge pull request #13 from gamerlogue/dependabot/github_actions/github-actions-c5f29f1f99
- [`fcd72a5`](https://github.com/gamerlogue/app/commit/fcd72a521d6eda2bc5eef23253e8b2da27be2518) Merge pull request #14 from gamerlogue/dependabot/gradle/gradle-8740273506

