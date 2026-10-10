package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_close
import gamerlogue.sharedui.generated.resources.settings__import_deselect_all
import gamerlogue.sharedui.generated.resources.settings__import_no_match
import gamerlogue.sharedui.generated.resources.settings__import_select_all
import gamerlogue.sharedui.generated.resources.settings__import_selected
import gamerlogue.sharedui.generated.resources.settings__open_store
import gamerlogue.sharedui.generated.resources.settings__service_sync_added
import gamerlogue.sharedui.generated.resources.settings__service_sync_error
import gamerlogue.sharedui.generated.resources.settings__service_sync_pushed
import gamerlogue.sharedui.generated.resources.settings__service_sync_summary
import gamerlogue.sharedui.generated.resources.settings__service_webview_busy
import gamerlogue.sharedui.generated.resources.settings__service_working
import gamerlogue.sharedui.generated.resources.settings__sync_title_connect
import gamerlogue.sharedui.generated.resources.settings__sync_title_import_library
import gamerlogue.sharedui.generated.resources.settings__sync_title_refresh_profile
import gamerlogue.sharedui.generated.resources.settings__sync_title_sync_wishlist
import gamerlogue.sharedui.generated.resources.settings__wishlist_already_present
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_confirm
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_off_platform
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_skip
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_title
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DownloadW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HourglassW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PublishW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.WishlistWrite
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.StatusMessage
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.label
import it.maicol07.gamerlogue.ui.components.rememberServiceWebViewHost
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.settings.components.SettingsGroupHeader
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Hosts one store automation flow ([ServiceSyncAction]) as a dedicated screen: the single WebView
 * lives in a persistent bottom sheet — expanded for interactive login, collapsed to a non-interactive
 * peek while working (so the user can see what's happening). The body shows the loading log, the
 * outgoing push checklist, or a completion state. Import/preview flows hand off to the preview screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun ServiceSyncView(
    service: ExternalService,
    action: ServiceSyncAction,
    viewModel: LinkedServicesViewModel = koinViewModel(),
) {
    val navigationState = LocalNavigationState.current
    val onFinish: () -> Unit = { navigationState.backStack.removeLastOrNull() }
    val navigateToImportPreview = { importService: ExternalService, mode: ImportMode ->
        navigationState.backStack.removeLastOrNull()
        navigationState.backStack.add(RootNavTree.LibraryImportPreview(importService, mode))
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val connector = remember(service) { viewModel.connector(service) }

    // Guards the flow's onClose: import/preview navigate away (don't show completion), the rest finish here.
    var navigatedAway by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val host = rememberServiceWebViewHost(
        initialUrl = connector.loginUrl,
        onClose = { if (!navigatedAway) finished = true },
    ) { session ->
        when (action) {
            ServiceSyncAction.CONNECT -> viewModel.runConnect(service, session)
            ServiceSyncAction.REFRESH_PROFILE -> viewModel.runRefreshProfile(service, session)
            ServiceSyncAction.SYNC_WISHLIST -> viewModel.runWishlistSync(service, session)
            ServiceSyncAction.IMPORT_LIBRARY, ServiceSyncAction.PREVIEW_WISHLIST -> {
                val wishlist = action == ServiceSyncAction.PREVIEW_WISHLIST
                val refs =
                    if (wishlist) viewModel.runWishlistPreview(service, session) else viewModel.runReadOwned(service, session)
                ImportHandoff.put(service, refs)
                navigatedAway = true
                navigateToImportPreview(service, if (wishlist) ImportMode.WISHLIST else ImportMode.OWNED)
            }
        }
    }
    val session = host.session
    val loginRequired = session.loginRequired

    val title = when (action) {
        ServiceSyncAction.CONNECT -> Res.string.settings__sync_title_connect
        ServiceSyncAction.REFRESH_PROFILE -> Res.string.settings__sync_title_refresh_profile
        ServiceSyncAction.SYNC_WISHLIST,
        ServiceSyncAction.PREVIEW_WISHLIST -> Res.string.settings__sync_title_sync_wishlist

        ServiceSyncAction.IMPORT_LIBRARY -> Res.string.settings__sync_title_import_library
    }

    val sheetState = rememberBottomSheetState(
        SheetValue.PartiallyExpanded,
        enabledValues = setOf(SheetValue.PartiallyExpanded, SheetValue.Expanded),
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    // Expand the sheet to a full, interactive WebView while login is needed; peek otherwise.
    LaunchedEffect(loginRequired) {
        if (loginRequired) sheetState.expand() else sheetState.partialExpand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 220.dp,
        topBar = {
            MediumFlexibleTopAppBar(
                title = { Text(stringResource(title)) },
                subtitle = { Text(stringResource(service.labelRes())) },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.CloseW500Rounded, contentDescription = stringResource(Res.string.common_close))
                    }
                },
            )
        },
        sheetContent = {
            Box(Modifier.fillMaxSize()) {
                host.webView(Modifier.fillMaxSize())
                // Non-interactive while working: a scrim hides the page and tells the user it can't be used
                // right now. It does NOT consume pointer events, so vertical drags still reach the WebView's
                // nested-scroll bridge and keep the bottom sheet draggable.
                if (!loginRequired) {
                    // Top-aligned so the notice stays inside the collapsed peek.
                    Box(
                        Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Row(
                            Modifier
                                .padding(24.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.extraLarge)
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.HourglassW500Rounded, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(Res.string.settings__service_webview_busy),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val pending = session.pendingConfirm
            val outcome = uiState.outcome
            when {
                pending != null -> PushChecklist(
                    games = pending,
                    matchesByName = connector.wishlistWrite is WishlistWrite.SearchByName,
                    onConfirm = session::resolveConfirm,
                    onSkip = { session.resolveConfirm(emptyList()) },
                )
                // Pop straight back to Linked Services unless there is something worth reporting.
                finished && outcome == LinkedServicesViewModel.SyncOutcome.Failed -> ErrorContent(onFinish)
                finished && outcome is LinkedServicesViewModel.SyncOutcome.WishlistSynced &&
                    outcome.changedAnything -> WishlistSummary(outcome, onFinish)

                finished -> LaunchedEffect(Unit) { onFinish() }
                else -> LoadingContent(session.log)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingContent(log: List<SyncPhase>) {
    // Each WebView step logs READING again: show every phase once.
    val steps = log.distinct()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContainedLoadingIndicator(Modifier.size(96.dp))
        Text(stringResource(Res.string.settings__service_working), style = MaterialTheme.typography.headlineSmallEmphasized)
        if (steps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, phase ->
                    // LOGGED_IN is a milestone; READING is ongoing while it's the latest phase.
                    PhaseStep(phase, inProgress = phase == SyncPhase.READING && index == steps.lastIndex)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PhaseStep(phase: SyncPhase, inProgress: Boolean) = Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    if (inProgress) {
        LoadingIndicator(Modifier.size(32.dp))
    } else {
        Box(
            Modifier.size(32.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.CheckW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    Text(
        phase.label(),
        style = if (inProgress) MaterialTheme.typography.titleMediumEmphasized else MaterialTheme.typography.bodyLarge,
        color = if (inProgress) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ErrorContent(onFinish: () -> Unit) = StatusMessage(
    Icons.ErrorW500Rounded,
    stringResource(Res.string.settings__service_sync_error),
    containerColor = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
) {
    Button(onClick = onFinish, shapes = ButtonDefaults.shapes()) { Text(stringResource(Res.string.common_close)) }
}

/** What a wishlist sync actually did, shown only when it changed something on either side. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WishlistSummary(
    outcome: LinkedServicesViewModel.SyncOutcome.WishlistSynced,
    onFinish: () -> Unit,
) = StatusMessage(
    Icons.CheckCircleW500Rounded,
    stringResource(Res.string.settings__service_sync_summary),
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = MaterialShapes.Sunny,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryTile(
            outcome.added,
            stringResource(Res.string.settings__service_sync_added),
            Icons.DownloadW500Rounded,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Modifier.weight(1f),
        )
        SummaryTile(
            outcome.pushed,
            stringResource(Res.string.settings__service_sync_pushed),
            Icons.PublishW500Rounded,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Modifier.weight(1f),
        )
    }
    Button(onClick = onFinish, shapes = ButtonDefaults.shapes()) { Text(stringResource(Res.string.common_close)) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SummaryTile(
    count: Int,
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier,
) = Column(
    modifier
        .background(containerColor, MaterialTheme.shapes.extraLarge)
        .padding(16.dp)
        .semantics(mergeDescendants = true) {},
    verticalArrangement = Arrangement.spacedBy(4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Icon(icon, contentDescription = null, tint = contentColor)
    Text(count.toString(), style = MaterialTheme.typography.displaySmallEmphasized, color = contentColor)
    Text(label, style = MaterialTheme.typography.labelLarge, color = contentColor, textAlign = TextAlign.Center)
}

/** Outgoing-direction preview: pick which backlog games to add to the store wishlist. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PushChecklist(
    games: List<LibrarySync.OutgoingGame>,
    matchesByName: Boolean,
    onConfirm: (List<LibrarySync.OutgoingGame>) -> Unit,
    onSkip: () -> Unit,
) {
    // Pushable rows need to release on this platform and either a store page or, for connectors that
    // push by searching the store's title (no store URL from IGDB), a confirmed publisher match — so an
    // unrelated backlog game from another publisher isn't searched for and pushed onto the wrong store.
    val selected = remember(games) {
        mutableStateMapOf<String, Boolean>().apply { games.forEach { put(it.uid, it.isPushable(matchesByName)) } }
    }
    val onPlatform = games.filter { it.onPlatform }
    val offPlatform = games.filter { !it.onPlatform }
    val pushable = games.filter { it.isPushable(matchesByName) }
    val selectedCount = games.count { selected[it.uid] == true }
    val allSelected = pushable.isNotEmpty() && selectedCount == pushable.size
    val uriHandler = LocalUriHandler.current
    val rows: LazyListScope.(List<LibrarySync.OutgoingGame>) -> Unit = { group ->
        itemsIndexed(group) { index, game ->
            PushRow(game, index, group.size, selected[game.uid] == true, matchesByName, uriHandler::openURL) {
                selected[game.uid] = it
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.settings__wishlist_push_title),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
                Text(
                    pluralStringResource(Res.plurals.settings__import_selected, selectedCount, selectedCount, pushable.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = { pushable.forEach { selected[it.uid] = !allSelected } },
                enabled = pushable.isNotEmpty(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    stringResource(
                        if (allSelected) Res.string.settings__import_deselect_all
                        else Res.string.settings__import_select_all
                    )
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            rows(onPlatform)
            if (offPlatform.isNotEmpty()) {
                item { SettingsGroupHeader(stringResource(Res.string.settings__wishlist_push_off_platform)) }
                rows(offPlatform)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val height = ButtonDefaults.MediumContainerHeight
            val leading = ButtonGroupDefaults.connectedLeadingButtonShapes()
            val trailing = ButtonGroupDefaults.connectedTrailingButtonShapes()
            FilledTonalButton(
                onClick = onSkip,
                shapes = ButtonDefaults.shapes(leading.shape, leading.pressedShape),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier.weight(1f).heightIn(min = height),
            ) {
                Text(stringResource(Res.string.settings__wishlist_push_skip), style = ButtonDefaults.textStyleFor(height))
            }
            Button(
                onClick = { onConfirm(games.filter { selected[it.uid] == true }) },
                enabled = selectedCount > 0,
                shapes = ButtonDefaults.shapes(trailing.shape, trailing.pressedShape),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier.weight(1f).heightIn(min = height),
            ) {
                Text(stringResource(Res.string.settings__wishlist_push_confirm), style = ButtonDefaults.textStyleFor(height))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PushRow(
    game: LibrarySync.OutgoingGame,
    index: Int,
    count: Int,
    checked: Boolean,
    matchesByName: Boolean,
    onOpenStore: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    SegmentedListItem(
        selected = checked,
        enabled = game.isPushable(matchesByName),
        onClick = { onCheckedChange(!checked) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.expressiveSegmentedColors(),
        leadingContent = {
            val cover = game.coverImageId
            if (cover != null) {
                val coverModifier = Modifier.size(width = 40.dp, height = 53.dp).clip(MaterialTheme.shapes.medium)
                RemoteImage(
                    url = igdbImageUrl(cover, IgdbImageSize.COVER_SMALL),
                    contentDescription = game.name,
                    modifier = coverModifier,
                    loadingModifier = coverModifier,
                )
            }
        },
        trailingContent = game.storeUrl?.let { url ->
            {
                IconButton(onClick = { onOpenStore(url) }) {
                    Icon(Icons.OpenInNewW500Rounded, contentDescription = stringResource(Res.string.settings__open_store))
                }
            }
        },
        supportingContent = {
            val subtitle = when {
                game.alreadyOnWishlist -> stringResource(Res.string.settings__wishlist_already_present)
                game.onPlatform && !game.hasPushTarget(matchesByName) -> stringResource(Res.string.settings__import_no_match)

                else -> null
            }
            subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
    ) { Text(game.name) }
}

/** A store page, or — for connectors that push by searching the store's title — a confirmed publisher match. */
private fun LibrarySync.OutgoingGame.hasPushTarget(matchesByName: Boolean) =
    storeUrl != null || (matchesByName && matchesPublisher)

private fun LibrarySync.OutgoingGame.isPushable(matchesByName: Boolean) =
    hasPushTarget(matchesByName) && onPlatform && !alreadyOnWishlist
