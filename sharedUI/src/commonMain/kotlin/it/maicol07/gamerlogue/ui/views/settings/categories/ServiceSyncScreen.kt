package it.maicol07.gamerlogue.ui.views.settings.categories

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_close
import gamerlogue.sharedui.generated.resources.settings__import_no_match
import gamerlogue.sharedui.generated.resources.settings__open_store
import gamerlogue.sharedui.generated.resources.settings__service_done
import gamerlogue.sharedui.generated.resources.settings__service_sync_error
import gamerlogue.sharedui.generated.resources.settings__service_working
import gamerlogue.sharedui.generated.resources.settings__wishlist_already_present
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_confirm
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_off_platform
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_skip
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.label
import it.maicol07.gamerlogue.ui.components.rememberServiceWebViewHost
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Hosts one store automation flow ([ServiceSyncAction]) as a dedicated screen: the single WebView
 * lives in a persistent bottom sheet — expanded for interactive login, collapsed to a non-interactive
 * peek while working (so the user can see what's happening). The body shows the loading log, the
 * outgoing push checklist, or a completion state. Import/preview flows hand off to the preview screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServiceSyncScreen(
    service: ExternalService,
    action: ServiceSyncAction,
    onFinish: () -> Unit,
    navigateToImportPreview: (ExternalService, ImportMode) -> Unit,
    viewModel: LinkedServicesViewModel = koinViewModel(),
) {
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
            ServiceSyncAction.IMPORT_LIBRARY -> {
                val refs = viewModel.runReadOwned(service, session)
                ImportHandoff.put(service, refs)
                navigatedAway = true
                navigateToImportPreview(service, ImportMode.OWNED)
            }
            ServiceSyncAction.PREVIEW_WISHLIST -> {
                val refs = viewModel.runWishlistPreview(service, session)
                ImportHandoff.put(service, refs)
                navigatedAway = true
                navigateToImportPreview(service, ImportMode.WISHLIST)
            }
        }
    }
    val session = host.session
    val loginRequired = session.loginRequired

    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    // Expand the sheet to a full, interactive WebView while login is needed; peek otherwise.
    LaunchedEffect(loginRequired) {
        if (loginRequired) sheetState.expand() else sheetState.partialExpand()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 220.dp,
        topBar = {
            TopAppBar(
                title = { Text(service.name) },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.CloseW500Rounded, contentDescription = stringResource(Res.string.common_close))
                    }
                },
            )
        },
        sheetContent = {
            Box(Modifier.fillMaxWidth().fillMaxSize()) {
                host.webView(Modifier.fillMaxSize())
                // Non-interactive while working: consume all pointer events so the automation isn't
                // disturbed. ponytail: best-effort — on desktop CEF the native surface draws above
                // Compose, so the overlay can't fully block it (same limit as the cookie/CEF notes).
                if (!loginRequired) {
                    Box(
                        Modifier.matchParentSize().pointerInput(Unit) {
                            awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
                        },
                    )
                }
                if (session.awaitingManualLogin) {
                    Button(
                        onClick = session::resolveManualLogin,
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                    ) {
                        Text(stringResource(Res.string.settings__wishlist_push_confirm))
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val pending = session.pendingConfirm
            when {
                pending != null -> PushChecklist(
                    games = pending,
                    onConfirm = session::resolveConfirm,
                    onSkip = { session.resolveConfirm(emptyList()) },
                )
                finished -> CompletionContent(error = uiState.message == "error", onFinish = onFinish)
                else -> LoadingContent(session.log)
            }
        }
    }
}

@Composable
private fun LoadingContent(log: List<SyncPhase>) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(stringResource(Res.string.settings__service_working), style = MaterialTheme.typography.titleMedium)
        log.forEach { phase ->
            Text(phase.label(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompletionContent(error: Boolean, onFinish: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            if (error) Icons.ErrorW500Rounded else Icons.CheckCircleW500Rounded,
            contentDescription = null,
            tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            stringResource(if (error) Res.string.settings__service_sync_error else Res.string.settings__service_done),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(onClick = onFinish) { Text(stringResource(Res.string.common_close)) }
    }
}

/** Outgoing-direction preview: pick which backlog games to add to the store wishlist. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PushChecklist(
    games: List<LibrarySync.OutgoingGame>,
    onConfirm: (List<LibrarySync.OutgoingGame>) -> Unit,
    onSkip: () -> Unit,
) {
    // Pushable rows need both a store page and to release on this platform; the rest are shown disabled.
    val selected = remember(games) {
        mutableStateMapOf<String, Boolean>().apply {
            games.forEach { put(it.uid, it.storeUrl != null && it.onPlatform && !it.alreadyOnWishlist) }
        }
    }
    val onPlatform = games.filter { it.onPlatform }
    val offPlatform = games.filter { !it.onPlatform }
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(Res.string.settings__wishlist_push_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(onPlatform) { index, game ->
                PushRow(game, index, onPlatform.size, selected[game.uid] == true, uriHandler::openURL) {
                    selected[game.uid] = it
                }
            }
            if (offPlatform.isNotEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.settings__wishlist_push_off_platform),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 4.dp),
                    )
                }
                itemsIndexed(offPlatform) { index, game ->
                    PushRow(game, index, offPlatform.size, selected[game.uid] == true, uriHandler::openURL) {
                        selected[game.uid] = it
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.settings__wishlist_push_skip))
            }
            Button(
                onClick = { onConfirm(games.filter { selected[it.uid] == true }) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.settings__wishlist_push_confirm))
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
    onOpenStore: (String) -> Unit,
    onCheckedChange: (Boolean) -> Unit,
) {
    val pushable = game.storeUrl != null && game.onPlatform && !game.alreadyOnWishlist
    SegmentedListItem(
        selected = checked,
        enabled = pushable,
        onClick = { onCheckedChange(!checked) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.expressiveSegmentedColors(),
        leadingContent = {
            val cover = game.coverImageId
            if (cover != null) {
                RemoteImage(
                    url = igdbImageUrl(cover, IgdbImageSize.COVER_SMALL),
                    contentDescription = game.name,
                    modifier = Modifier.size(width = 40.dp, height = 53.dp).clip(RoundedCornerShape(6.dp)),
                    loadingModifier = Modifier.size(width = 40.dp, height = 53.dp).clip(RoundedCornerShape(6.dp)),
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
                game.onPlatform && game.storeUrl == null -> stringResource(Res.string.settings__import_no_match)
                else -> null
            }
            subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
    ) { Text(game.name) }
}
