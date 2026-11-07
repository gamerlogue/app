package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_close
import gamerlogue.sharedui.generated.resources.exception__details_copy
import gamerlogue.sharedui.generated.resources.settings__library_push_title
import gamerlogue.sharedui.generated.resources.settings__library_sync_conflicts_left
import gamerlogue.sharedui.generated.resources.settings__library_sync_pulled
import gamerlogue.sharedui.generated.resources.settings__library_sync_pushed
import gamerlogue.sharedui.generated.resources.settings__library_sync_summary
import gamerlogue.sharedui.generated.resources.settings__service_progress_batch
import gamerlogue.sharedui.generated.resources.settings__service_progress_count
import gamerlogue.sharedui.generated.resources.settings__service_progress_of
import gamerlogue.sharedui.generated.resources.settings__service_resync_ids
import gamerlogue.sharedui.generated.resources.settings__service_resync_ids_result
import gamerlogue.sharedui.generated.resources.settings__service_skip_already_present
import gamerlogue.sharedui.generated.resources.settings__service_skip_no_platform
import gamerlogue.sharedui.generated.resources.settings__service_skip_not_found
import gamerlogue.sharedui.generated.resources.settings__service_skip_notes_too_long
import gamerlogue.sharedui.generated.resources.settings__service_skip_platform_rejected
import gamerlogue.sharedui.generated.resources.settings__service_skip_rejected
import gamerlogue.sharedui.generated.resources.settings__service_skip_unconfirmed
import gamerlogue.sharedui.generated.resources.settings__service_skip_unreadable
import gamerlogue.sharedui.generated.resources.settings__service_sync_added
import gamerlogue.sharedui.generated.resources.settings__service_sync_error
import gamerlogue.sharedui.generated.resources.settings__service_sync_pushed
import gamerlogue.sharedui.generated.resources.settings__service_sync_skipped
import gamerlogue.sharedui.generated.resources.settings__service_sync_skipped_list
import gamerlogue.sharedui.generated.resources.settings__service_sync_summary
import gamerlogue.sharedui.generated.resources.settings__service_technical_details
import gamerlogue.sharedui.generated.resources.settings__service_webview_busy
import gamerlogue.sharedui.generated.resources.settings__service_working
import gamerlogue.sharedui.generated.resources.settings__sync_title_connect
import gamerlogue.sharedui.generated.resources.settings__sync_title_import_library
import gamerlogue.sharedui.generated.resources.settings__sync_title_refresh_profile
import gamerlogue.sharedui.generated.resources.settings__sync_title_sync_library
import gamerlogue.sharedui.generated.resources.settings__sync_title_sync_wishlist
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_title
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CodeW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ContentCopyW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DownloadW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HourglassW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PublishW500Rounded
import it.maicol07.gamerlogue.clipEntryFor
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.ServiceCategory
import it.maicol07.gamerlogue.services.SkipReason
import it.maicol07.gamerlogue.services.SkippedWrite
import it.maicol07.gamerlogue.ui.components.BatchProgress
import it.maicol07.gamerlogue.ui.components.StatusMessage
import it.maicol07.gamerlogue.ui.components.StepProgress
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.label
import it.maicol07.gamerlogue.ui.components.rememberServiceWebViewHost
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Hosts one store automation flow ([ServiceSyncAction]) as a dedicated screen: the single WebView
 * lives in a persistent bottom sheet — expanded for interactive login, collapsed to a non-interactive
 * peek while working (so the user can see what's happening). The body shows the loading log, the
 * outgoing push checklist, or a completion state. Import/preview flows hand off to the preview screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
// One host owns the WebView lifecycle through login, selection and writes.
@Suppress("LongMethod", "CyclomaticComplexMethod")
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
            ServiceSyncAction.PUSH_WISHLIST -> viewModel.runWishlistPush(service, session)
            ServiceSyncAction.SYNC_LIBRARY -> viewModel.runLibrarySync(service, session)
            ServiceSyncAction.APPLY_LIBRARY_SYNC -> viewModel.runApplyLibrarySync(service, session)
            ServiceSyncAction.APPLY_RESYNC_IDS -> viewModel.runApplyResyncIds(service, session)
            ServiceSyncAction.RESYNC_IDS -> {
                viewModel.runReadForIdSync(service, session)?.let { refs ->
                    ImportHandoff.put(service, refs)
                    navigatedAway = true
                    navigateToImportPreview(service, ImportMode.ID_SYNC)
                }
            }

            ServiceSyncAction.PREVIEW_LIBRARY_SYNC -> viewModel.runReadOwned(service, session)?.let { refs ->
                ImportHandoff.put(service, refs)
                navigatedAway = true
                navigateToImportPreview(service, ImportMode.SYNC)
            }

            ServiceSyncAction.IMPORT_LIBRARY, ServiceSyncAction.PREVIEW_WISHLIST -> {
                val wishlist = action == ServiceSyncAction.PREVIEW_WISHLIST
                val refs = if (wishlist) {
                    viewModel.runWishlistPreview(service, session)
                } else {
                    viewModel.runReadOwned(service, session)
                }
                refs?.let {
                    ImportHandoff.put(service, it)
                    navigatedAway = true
                    navigateToImportPreview(
                        service,
                        when {
                            wishlist -> ImportMode.WISHLIST
                            service.category == ServiceCategory.TRACKER -> ImportMode.TRACKED
                            else -> ImportMode.OWNED
                        },
                    )
                }
            }
        }
    }
    val session = host.session
    val loginRequired = session.loginRequired
    val pending = session.pendingConfirm
    val pushTitle = when (action) {
        ServiceSyncAction.SYNC_LIBRARY, ServiceSyncAction.APPLY_LIBRARY_SYNC -> Res.string.settings__library_push_title
        else -> Res.string.settings__wishlist_push_title
    }

    val title = when (action) {
        ServiceSyncAction.CONNECT -> Res.string.settings__sync_title_connect
        ServiceSyncAction.REFRESH_PROFILE -> Res.string.settings__sync_title_refresh_profile
        ServiceSyncAction.RESYNC_IDS, ServiceSyncAction.APPLY_RESYNC_IDS -> Res.string.settings__service_resync_ids
        ServiceSyncAction.SYNC_WISHLIST,
        ServiceSyncAction.PUSH_WISHLIST,
        ServiceSyncAction.PREVIEW_WISHLIST -> Res.string.settings__sync_title_sync_wishlist

        ServiceSyncAction.IMPORT_LIBRARY -> Res.string.settings__sync_title_import_library
        ServiceSyncAction.SYNC_LIBRARY,
        ServiceSyncAction.PREVIEW_LIBRARY_SYNC,
        ServiceSyncAction.APPLY_LIBRARY_SYNC -> Res.string.settings__sync_title_sync_library
    }

    // Closing a running flow interrupts it (writes already sent stay sent): ask first. Signing in or
    // refreshing the profile has nothing to lose, so those close straight away.
    val running = !finished && !navigatedAway &&
        action != ServiceSyncAction.CONNECT && action != ServiceSyncAction.REFRESH_PROFILE
    // A long read/write stops when the device locks itself: the sheet keeps the screen on until the flow is done.
    var askInterrupt by remember { mutableStateOf(false) }
    SyncInterruptGuard(
        running = running,
        asking = askInterrupt,
        onAsk = { askInterrupt = true },
        onDismiss = { askInterrupt = false },
        onConfirm = onFinish,
    )

    // The push review hides the sheet. Dropping the peek height to 0 instead crashed the scaffold: the partial
    // anchor vanished while the sheet was still animating to it.
    val reviewing by rememberUpdatedState(pending != null)
    val sheetState = rememberBottomSheetState(
        SheetValue.PartiallyExpanded,
        confirmValueChange = { it != SheetValue.Hidden || reviewing },
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    // Expand the sheet to a full, interactive WebView while login is needed; peek otherwise.
    LaunchedEffect(loginRequired, pending != null) {
        when {
            loginRequired -> sheetState.expand()
            pending != null -> sheetState.hide()
            else -> sheetState.partialExpand()
        }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 220.dp,
        // While the WebView is interactive the sheet is expanded and stays put: the Android WebView doesn't
        // report reliably how much of a scroll it consumed (pages scrolling an inner container), so the
        // nested-scroll interop alone let the sheet steal drags. It collapses by itself once login is done.
        sheetSwipeEnabled = !loginRequired && pending == null,
        sheetDragHandle = { if (pending == null) SheetToggleHandle(sheetState) },
        topBar = {
            MediumFlexibleTopAppBar(
                title = {
                    Text(
                        if (pending == null) {
                            stringResource(title)
                        } else {
                            stringResource(pushTitle, stringResource(service.labelRes()))
                        }
                    )
                },
                subtitle = { Text(stringResource(service.labelRes())) },
                navigationIcon = {
                    IconButton(
                        onClick = { if (running) askInterrupt = true else onFinish() },
                        shapes = IconButtonDefaults.shapes(),

                    ) {
                        Icon(Icons.CloseW500Rounded, contentDescription = stringResource(Res.string.common_close))
                    }
                },
            )
        },
        sheetContent = {
            Box(Modifier.fillMaxSize().then(if (running) Modifier.keepScreenOn() else Modifier)) {
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
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    MaterialTheme.shapes.extraLarge,
                                )
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.HourglassW500Rounded,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
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
        // The body is always padded by the peek height, even while the review hides the sheet: drop it then.
        Box(Modifier.padding(if (pending == null) padding else PaddingValues()).fillMaxSize()) {
            val outcome = uiState.outcome
            when {
                pending != null -> WishlistPushScreen(
                    games = pending,
                    requiresPlatform = service == ExternalService.BACKLOGGERY,
                    // Name, IGDB-id and full-game writes do not need a store page.
                    matchesByName = connector.wishlistWrite?.needsStorePage == false,
                    onConfirm = session::resolveConfirm,
                    onSkip = { session.resolveConfirm(emptyList()) },
                )
                // Pop straight back to Linked Services unless there is something worth reporting.
                finished && outcome == LinkedServicesViewModel.SyncOutcome.Failed ->
                    SyncReport(leftOut = emptyList(), session.console) { ErrorContent(onFinish) }

                finished && outcome is LinkedServicesViewModel.SyncOutcome.WishlistSynced &&
                    outcome.changedAnything -> SyncReport(outcome.skipped, session.console) {
                    SyncSummary(
                        Res.string.settings__service_sync_summary,
                        outcome.added,
                        Res.string.settings__service_sync_added,
                        outcome.pushed,
                        Res.string.settings__service_sync_pushed,
                        outcome.skipped.size,
                        conflicts = 0,
                        onFinish,
                    )
                }

                finished && outcome is LinkedServicesViewModel.SyncOutcome.LibrarySynced &&
                    outcome.changedAnything -> SyncReport(outcome.skipped, session.console) {
                    SyncSummary(
                        Res.string.settings__library_sync_summary,
                        outcome.pulled,
                        Res.string.settings__library_sync_pulled,
                        outcome.pushed,
                        Res.string.settings__library_sync_pushed,
                        outcome.skipped.size,
                        outcome.conflicts,
                        onFinish,
                    )
                }

                finished && outcome is LinkedServicesViewModel.SyncOutcome.IdsResynced -> StatusMessage(
                    Icons.CheckCircleW500Rounded,
                    stringResource(Res.string.settings__service_resync_ids_result, outcome.updated, outcome.unmatched),
                ) {
                    Button(onClick = onFinish, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(Res.string.common_close))
                    }
                }

                finished -> LaunchedEffect(Unit) { onFinish() }
                else -> LoadingContent(session.log, session.progress, session.batchProgress)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingContent(log: List<SyncPhase>, progress: StepProgress?, batch: BatchProgress?) {
    // Each WebView step logs its phase again: show every phase once.
    val steps = log.distinct()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContainedLoadingIndicator(Modifier.size(96.dp))
        Text(
            stringResource(Res.string.settings__service_working),
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        if (steps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, phase ->
                    // LOGGED_IN is a milestone; the work phases are ongoing while they're the latest one.
                    PhaseStep(phase, inProgress = phase != SyncPhase.LOGGED_IN && index == steps.lastIndex)
                }
            }
        }
        // Games read/written so far: X/Y with a determinate bar once the total is known, just X before.
        progress?.let { (done, total) ->
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (total > 0) {
                    AnimatedSyncProgress(done, total)
                    Text(pluralStringResource(Res.plurals.settings__service_progress_of, total, done, total))
                } else {
                    LinearWavyProgressIndicator(Modifier.fillMaxWidth())
                    Text(pluralStringResource(Res.plurals.settings__service_progress_count, done, done))
                }
            }
        }
        batch?.let {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.settings__service_progress_batch, it.index, it.count))
                key(it.index) { AnimatedSyncProgress(it.step.done, it.step.total) }
                Text(
                    pluralStringResource(
                        Res.plurals.settings__service_progress_of,
                        it.step.total,
                        it.step.done,
                        it.step.total,
                    ),
                )
            }
        }
    }
}

@Composable
private fun AnimatedSyncProgress(done: Int, total: Int) {
    val animated by animateFloatAsState(
        if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f,
        MaterialTheme.motionScheme.defaultEffectsSpec(),
    )
    LinearWavyProgressIndicator(progress = { animated }, Modifier.fillMaxWidth())
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

/**
 * [summary] above two collapsed sections: the selected games a write [leftOut], each with its reason, and the
 * flow's script [console] for a bug report. With neither, [summary] fills the screen as before.
 */
@Composable
private fun SyncReport(
    leftOut: List<SkippedWrite>,
    console: List<String>,
    summary: @Composable () -> Unit,
) {
    val skipped = leftOut.withIndex().toList()
    var showSkipped by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "summary") {
            val bare = skipped.isEmpty() && console.isEmpty()
            Box(if (bare) Modifier.fillParentMaxSize() else Modifier.padding(vertical = 24.dp)) { summary() }
        }
        collapsibleSection(
            key = "skipped",
            rows = skipped,
            rowKey = { "skip-${it.index}" },
            expanded = showSkipped,
            header = { shapes, modifier ->
                SectionHeader(
                    stringResource(Res.string.settings__service_sync_skipped_list),
                    Icons.ErrorW500Rounded,
                    supporting = null,
                    expanded = showSkipped,
                    shapes = shapes,
                    onToggle = { showSkipped = !showSkipped },
                    modifier = modifier,
                )
            },
        ) { (_, game), position, count ->
            SegmentedListItem(
                shapes = ListItemDefaults.segmentedShapes(index = position, count = count),
                colors = ListItemDefaults.expressiveSegmentedColors(),
                supportingContent = { Text(stringResource(game.reason.label)) },
            ) { Text(game.name) }
        }
        technicalDetails(console, showLog) { showLog = !showLog }
    }
}

/** The flow's script logs as one selectable monospace block, so they can be copied into a bug report. */
private fun LazyListScope.technicalDetails(console: List<String>, expanded: Boolean, onToggle: () -> Unit) =
    collapsibleSection(
        key = "log",
        rows = listOfNotNull(console.joinToString("\n").ifEmpty { null }),
        rowKey = { "log-text" },
        expanded = expanded,
        header = { shapes, modifier ->
            SectionHeader(
                stringResource(Res.string.settings__service_technical_details),
                Icons.CodeW500Rounded,
                supporting = null,
                expanded = expanded,
                shapes = shapes,
                onToggle = onToggle,
                modifier = modifier,
            )
        },
    ) { text, position, count ->
        Surface(
            shape = ListItemDefaults.segmentedShapes(index = position, count = count).shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Box {
                SelectionContainer {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        // Room on the right for the copy button.
                        modifier = Modifier.fillMaxWidth().padding(16.dp).padding(end = 48.dp),
                    )
                }
                CopyButton(text, Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }
    }

/** Copies [text] to the clipboard, like the exception sheet's details. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyButton(text: String, modifier: Modifier) = TooltipBox(
    TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    { PlainTooltip { Text(stringResource(Res.string.exception__details_copy)) } },
    rememberTooltipState(),
    modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    FilledTonalIconButton(
        onClick = { scope.launch { clipboard.setClipEntry(clipEntryFor(text)) } },
        shapes = IconButtonDefaults.shapes(),
    ) {
        Icon(Icons.ContentCopyW500Rounded, stringResource(Res.string.exception__details_copy))
    }
}

private val SkipReason.label: StringResource
    get() = when (this) {
        SkipReason.ALREADY_PRESENT -> Res.string.settings__service_skip_already_present
        SkipReason.NO_PLATFORM -> Res.string.settings__service_skip_no_platform
        SkipReason.PLATFORM_REJECTED -> Res.string.settings__service_skip_platform_rejected
        SkipReason.REJECTED -> Res.string.settings__service_skip_rejected
        SkipReason.NOTES_TOO_LONG -> Res.string.settings__service_skip_notes_too_long
        SkipReason.UNREADABLE -> Res.string.settings__service_skip_unreadable
        SkipReason.NOT_FOUND -> Res.string.settings__service_skip_not_found
        SkipReason.UNCONFIRMED -> Res.string.settings__service_skip_unconfirmed
    }

@Composable
private fun ErrorContent(onFinish: () -> Unit) = StatusMessage(
    Icons.ErrorW500Rounded,
    stringResource(Res.string.settings__service_sync_error),
    containerColor = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
) {
    Button(onClick = onFinish, shapes = ButtonDefaults.shapes()) {
        Text(stringResource(Res.string.common_close))
    }
}

/** What a wishlist or library sync actually did, shown only when it changed something on either side or
 *  [skipped] selected writes; [conflicts] (library sync only) were left for a reviewed sync. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SyncSummary(
    title: StringResource,
    added: Int,
    addedLabel: StringResource,
    pushed: Int,
    pushedLabel: StringResource,
    skipped: Int,
    conflicts: Int,
    onFinish: () -> Unit,
) = StatusMessage(
    Icons.CheckCircleW500Rounded,
    stringResource(title),
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = MaterialShapes.Sunny,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryTile(
            added,
            stringResource(addedLabel),
            Icons.DownloadW500Rounded,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Modifier.weight(1f),
        )
        SummaryTile(
            pushed,
            stringResource(pushedLabel),
            Icons.PublishW500Rounded,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Modifier.weight(1f),
        )
    }
    if (skipped > 0) {
        Text(
            pluralStringResource(Res.plurals.settings__service_sync_skipped, skipped, skipped),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (conflicts > 0) {
        Text(
            pluralStringResource(Res.plurals.settings__library_sync_conflicts_left, conflicts, conflicts),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    Button(onClick = onFinish, shapes = ButtonDefaults.shapes()) {
        Text(stringResource(Res.string.common_close))
    }
}

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
