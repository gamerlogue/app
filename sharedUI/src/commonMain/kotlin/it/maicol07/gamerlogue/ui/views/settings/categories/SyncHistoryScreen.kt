package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.raedghazal.kotlinx_datetime_ext.LocalDateTimeFormatter
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_cancel
import gamerlogue.sharedui.generated.resources.sync__help
import gamerlogue.sharedui.generated.resources.sync__history
import gamerlogue.sharedui.generated.resources.sync__history_empty
import gamerlogue.sharedui.generated.resources.sync__kind_library
import gamerlogue.sharedui.generated.resources.sync__kind_wishlist
import gamerlogue.sharedui.generated.resources.sync__removed_confirm_text
import gamerlogue.sharedui.generated.resources.sync__removed_confirm_title
import gamerlogue.sharedui.generated.resources.sync__removed_delete
import gamerlogue.sharedui.generated.resources.sync__result_failed
import gamerlogue.sharedui.generated.resources.sync__result_login_required
import gamerlogue.sharedui.generated.resources.sync__result_ok
import gamerlogue.sharedui.generated.resources.sync__result_partial
import gamerlogue.sharedui.generated.resources.sync__status_running
import gamerlogue.sharedui.generated.resources.sync__trigger_change
import gamerlogue.sharedui.generated.resources.sync__trigger_manual
import gamerlogue.sharedui.generated.resources.sync__trigger_periodic
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DeleteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HelpW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HistoryW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.SyncGame
import it.maicol07.gamerlogue.services.SyncKind
import it.maicol07.gamerlogue.services.SyncResult
import it.maicol07.gamerlogue.services.SyncRun
import it.maicol07.gamerlogue.services.SyncTrigger
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.SectionIcon
import it.maicol07.gamerlogue.ui.components.layout.ExpandArrow
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.layout.collapsibleSection
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

/** Every sync with its result, newest first, by day; the games a service removed can be deleted here too. */
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun SyncHistoryView() {
    // Injected in the body: nav3ksp turns @Branch parameters into nav key properties.
    val viewModel = koinViewModel<SyncHistoryViewModel>()
    val navigationState = LocalNavigationState.current
    val runs by viewModel.runs.collectAsStateWithLifecycle()
    val running by viewModel.running.collectAsStateWithLifecycle()
    // Seen while the screen is open, including the runs that end meanwhile.
    LaunchedEffect(runs) { if (runs.any { !it.seen }) viewModel.markAllSeen() }
    var confirmDelete by remember { mutableStateOf<Pair<SyncRun, SyncGame>?>(null) }
    var helpFor by remember { mutableStateOf<SyncRun?>(null) }
    helpFor?.let { RunHelpDialog(it, onDismiss = { helpFor = null }) }
    confirmDelete?.let { (run, game) ->
        DeleteDialog(
            game,
            onConfirm = {
                confirmDelete = null
                viewModel.delete(run, game)
            },
            onDismiss = { confirmDelete = null },
        )
    }

    ScreenScaffold(title = Res.string.sync__history) {
        if (runs.isEmpty() && running.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    Icons.HistoryW500Rounded,
                    stringResource(Res.string.sync__history_empty),
                    hint = null,
                    modifier = Modifier.padding(Dimens.ScreenPadding),
                )
            }
            return@ScreenScaffold
        }
        HistoryList(
            running,
            runs,
            onOpen = { navigationState.backStack.add(it) },
            onHelp = { helpFor = it },
            onDelete = { run, game -> confirmDelete = run to game },
            onKeep = viewModel::keep,
        )
    }
}

/** Something to look at, open by default: a run that didn't end well, or the [latest] one with games to settle. */
private fun SyncRun.needsLook(latest: Boolean): Boolean =
    result != SyncResult.OK || (latest && removedOnService.isNotEmpty())

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HistoryList(
    running: Set<ExternalService>,
    runs: List<SyncRun>,
    onOpen: (RootNavTree.GameDetail) -> Unit,
    onHelp: (SyncRun) -> Unit,
    onDelete: (SyncRun, SyncGame) -> Unit,
    onKeep: (SyncRun, SyncGame) -> Unit,
) {
    // The runs the user opened or closed; the others follow needsLook.
    val toggled = remember { mutableStateMapOf<String, Boolean>() }
    // The game groups (received, sent, skipped) the user opened.
    val openGroups = remember { mutableStateMapOf<String, Boolean>() }
    val runningList = running.toList()
    // The newest run of each service and kind: the only one whose removed games are still a choice to make.
    val latestOf = runs.groupBy { it.service to it.kind }.mapValues { (_, serviceRuns) -> serviceRuns.first() }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            bottom = Dimens.ScreenPadding
        ),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(runningList, key = { _, service -> "running-$service" }) { i, service ->
            RunningItem(service, ListItemDefaults.segmentedShapes(i, runningList.size))
        }
        runs.groupBy { it.startedAt.format(DAY) }.forEach { (day, dayRuns) ->
            item(key = "day-$day") {
                Text(
                    day,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Dimens.SectionGap, start = 4.dp),
                )
            }
            dayRuns.forEach { run ->
                val latestRun = latestOf.getValue(run.service to run.kind)
                val isLatest = run.id == latestRun.id
                val open = toggled[run.id] ?: run.needsLook(isLatest)
                val pending = latestRun.removedOnService.mapTo(mutableSetOf()) { it.gameId }
                collapsibleSection(
                    key = run.id,
                    rows = run.details(isLatest, pending, openGroups.keys),
                    rowKey = { it.key },
                    expanded = open,
                    header = { shapes, modifier ->
                        RunHeader(run, open, shapes, modifier, onHelp = { onHelp(run) }) { toggled[run.id] = !open }
                    },
                ) { detail, position, count ->
                    DetailItem(
                        detail,
                        stringResource(run.service.labelRes()),
                        ListItemDefaults.segmentedShapes(position, count),
                        onToggle = { key -> if (openGroups.remove(key) == null) openGroups[key] = true },
                        onOpen = onOpen,
                        onDelete = { onDelete(run, it) },
                        onKeep = { onKeep(run, it) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RunningItem(service: ExternalService, shapes: ListItemShapes) = SegmentedListItem(
    shapes = shapes,
    colors = ListItemDefaults.expressiveSegmentedColors(),
    leadingContent = { LoadingIndicator(Modifier.size(SectionIconSize)) },
    supportingContent = { Text(stringResource(Res.string.sync__status_running)) },
) { Text(stringResource(service.labelRes()), style = MaterialTheme.typography.titleMedium) }

/** A run's header: the service in a shape coloured by the result, what started the run and how it ended. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RunHeader(
    run: SyncRun,
    open: Boolean,
    shapes: ListItemShapes,
    modifier: Modifier,
    onHelp: () -> Unit,
    onToggle: () -> Unit,
) {
    val (container, content) = run.result.colors()
    SegmentedListItem(
        onClick = onToggle,
        shapes = shapes,
        colors = ListItemDefaults.expressiveSegmentedColors(),
        modifier = modifier,
        leadingContent = {
            SectionIcon(run.service.icon(), MaterialShapes.Cookie4Sided, container, content, run.service.logoUrl())
        },
        supportingContent = {
            val time = run.startedAt.format(TIME)
            Text("$time · ${stringResource(run.trigger.label)} · ${stringResource(run.result.label)}")
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onHelp, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.HelpW500Rounded, contentDescription = stringResource(Res.string.sync__help))
                }
                ExpandArrow(open, Modifier)
            }
        },
    ) {
        Text(
            "${stringResource(run.service.labelRes())} · ${stringResource(run.kind.label)}",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeleteDialog(game: SyncGame, onConfirm: () -> Unit, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.DeleteW500Rounded, contentDescription = null) },
    title = { Text(stringResource(Res.string.sync__removed_confirm_title, game.name)) },
    text = { Text(stringResource(Res.string.sync__removed_confirm_text)) },
    confirmButton = {
        TextButton(onClick = onConfirm, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(Res.string.sync__removed_delete), color = MaterialTheme.colorScheme.error)
        }
    },
    dismissButton = {
        TextButton(
            onClick = onDismiss,
            shapes = ButtonDefaults.shapes()
        ) { Text(stringResource(Res.string.common_cancel)) }
    },
)

/** The result as a container and content colour pair for the run's [SectionIcon]. */
@Composable
private fun SyncResult.colors(): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (this@colors) {
        SyncResult.OK -> primaryContainer to onPrimaryContainer
        SyncResult.PARTIAL -> tertiaryContainer to onTertiaryContainer
        SyncResult.FAILED, SyncResult.LOGIN_REQUIRED -> errorContainer to onErrorContainer
    }
}

private val SyncResult.label
    get() = when (this) {
        SyncResult.OK -> Res.string.sync__result_ok
        SyncResult.PARTIAL -> Res.string.sync__result_partial
        SyncResult.FAILED -> Res.string.sync__result_failed
        SyncResult.LOGIN_REQUIRED -> Res.string.sync__result_login_required
    }

internal val SyncKind.label
    get() = when (this) {
        SyncKind.WISHLIST -> Res.string.sync__kind_wishlist
        SyncKind.LIBRARY -> Res.string.sync__kind_library
    }

private val SyncTrigger.label
    get() = when (this) {
        SyncTrigger.MANUAL -> Res.string.sync__trigger_manual
        SyncTrigger.CHANGE -> Res.string.sync__trigger_change
        SyncTrigger.PERIODIC -> Res.string.sync__trigger_periodic
    }

/** Matches [SectionIcon]'s size, so a running row lines up with the run headers. */
private val SectionIconSize = 36.dp
private const val DAY = "dd/MM/yyyy"
private const val TIME = "HH:mm"

/** This epoch-milliseconds instant in the device's time zone, as [pattern]. */
private fun Long.format(pattern: String): String = LocalDateTimeFormatter.ofPattern(
    pattern,
    com.raedghazal.kotlinx_datetime_ext.Locale.forLanguageTag(Locale.current.toLanguageTag()),
).format(Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()))
