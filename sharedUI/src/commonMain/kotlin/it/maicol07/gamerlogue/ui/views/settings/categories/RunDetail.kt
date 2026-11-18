package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.sync__added
import gamerlogue.sharedui.generated.resources.sync__conflicts
import gamerlogue.sharedui.generated.resources.sync__error
import gamerlogue.sharedui.generated.resources.sync__login_required_hint
import gamerlogue.sharedui.generated.resources.sync__no_changes
import gamerlogue.sharedui.generated.resources.sync__pushed
import gamerlogue.sharedui.generated.resources.sync__received_game
import gamerlogue.sharedui.generated.resources.sync__removed_count
import gamerlogue.sharedui.generated.resources.sync__removed_delete
import gamerlogue.sharedui.generated.resources.sync__removed_keep
import gamerlogue.sharedui.generated.resources.sync__removed_see_latest
import gamerlogue.sharedui.generated.resources.sync__removed_title
import gamerlogue.sharedui.generated.resources.sync__sent_game
import gamerlogue.sharedui.generated.resources.sync__skipped_count
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DeleteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DownloadW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LinkOffW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LoginW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PublishW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WarningW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.services.SkippedWrite
import it.maicol07.gamerlogue.services.SyncGame
import it.maicol07.gamerlogue.services.SyncResult
import it.maicol07.gamerlogue.services.SyncRun
import it.maicol07.gamerlogue.ui.components.layout.ExpandArrow
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** A segment under a run's header: a line on what the run did, or a game it touched. */
internal sealed interface RunDetail {
    val key: String

    /** A line; one with [open] set heads a group of games and opens or closes it. */
    class Line(
        override val key: String,
        val icon: ImageVector,
        val open: Boolean? = null,
        val text: @Composable () -> String,
    ) : RunDetail

    class Received(override val key: String, val game: SyncGame) : RunDetail

    class Sent(override val key: String, val game: SyncGame) : RunDetail

    class Skipped(override val key: String, val write: SkippedWrite) : RunDetail

    class Removed(override val key: String, val game: SyncGame) : RunDetail
}

/**
 * The segments under [run]. The received, sent and skipped games are groups, listed while their key is in [open]
 * (runs from before the lists only have the counts). The removed games are listed only when the run is the
 * [latest] of its service, since they are a choice to make on the service's current state; an older run points
 * there for the ones of its games still [pending] in it, and says nothing of the ones settled or found since.
 */
internal fun SyncRun.details(latest: Boolean, pending: Set<Int>, open: Set<String>): List<RunDetail> = buildList {
    group("$id-received", Icons.DownloadW500Rounded, added, received, open, Res.plurals.sync__added) { i, game ->
        RunDetail.Received("$id-received-$i", game)
    }
    group("$id-sent", Icons.PublishW500Rounded, pushed, sent, open, Res.plurals.sync__pushed) { i, game ->
        RunDetail.Sent("$id-sent-$i", game)
    }
    if (conflicts > 0) {
        add(
            RunDetail.Line("$id-conflicts", Icons.WarningW500Rounded) { plural(Res.plurals.sync__conflicts, conflicts) }
        )
    }
    when (result) {
        SyncResult.LOGIN_REQUIRED ->
            add(
                RunDetail.Line(
                    "$id-login",
                    Icons.LoginW500Rounded
                ) { stringResource(Res.string.sync__login_required_hint) }
            )
        SyncResult.FAILED ->
            add(
                RunDetail.Line(
                    "$id-error",
                    Icons.ErrorW500Rounded
                ) { stringResource(Res.string.sync__error, error.orEmpty()) }
            )
        else -> Unit
    }
    group(
        "$id-skipped",
        Icons.CloseW500Rounded,
        skipped.size,
        skipped,
        open,
        Res.plurals.sync__skipped_count
    ) { i, write ->
        RunDetail.Skipped("$id-skipped-$i", write)
    }
    val removed = if (latest) removedOnService.size else removedOnService.count { it.gameId in pending }
    if (removed > 0 && latest) {
        add(
            RunDetail.Line("$id-removed", Icons.LinkOffW500Rounded) { plural(Res.plurals.sync__removed_count, removed) }
        )
        removedOnService.forEach { add(RunDetail.Removed("$id-removed-${it.gameId}", it)) }
    } else if (removed > 0) {
        add(
            RunDetail.Line(
                "$id-removed",
                Icons.LinkOffW500Rounded
            ) { plural(Res.plurals.sync__removed_see_latest, removed) }
        )
    }
    if (isEmpty()) {
        add(RunDetail.Line("$id-none", Icons.CheckCircleW500Rounded) { stringResource(Res.string.sync__no_changes) })
    }
}

/** A [count] line that opens onto [games] when it has them and its [key] is [open]; nothing for none. */
private fun <T> MutableList<RunDetail>.group(
    key: String,
    icon: ImageVector,
    count: Int,
    games: List<T>,
    open: Set<String>,
    label: PluralStringResource,
    row: (index: Int, item: T) -> RunDetail,
) {
    if (count == 0) return
    val expanded = key in open
    add(RunDetail.Line(key, icon, open = expanded.takeIf { games.isNotEmpty() }) { plural(label, count) })
    if (expanded) games.forEachIndexed { i, game -> add(row(i, game)) }
}

@Composable
private fun plural(resource: PluralStringResource, count: Int) = pluralStringResource(resource, count, count)

/**
 * One segment: [onToggle] opens or closes a group line, [onOpen] opens a game, [onKeep] and [onDelete] settle a game
 * the service removed. [service] names the service in a game's supporting line.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailItem(
    detail: RunDetail,
    service: String,
    shapes: ListItemShapes,
    onToggle: (String) -> Unit,
    onOpen: (RootNavTree.GameDetail) -> Unit,
    onDelete: (SyncGame) -> Unit,
    onKeep: (SyncGame) -> Unit,
) {
    val colors = ListItemDefaults.expressiveSegmentedColors()
    val open = { game: SyncGame -> onOpen(RootNavTree.GameDetail(game.gameId, game.coverImageId, game.name)) }
    when (detail) {
        is RunDetail.Line -> if (detail.open == null) {
            SegmentedListItem(
                shapes = shapes,
                colors = colors,
                leadingContent = { Icon(detail.icon, contentDescription = null) },
            ) { Text(detail.text()) }
        } else {
            SegmentedListItem(
                onClick = { onToggle(detail.key) },
                shapes = shapes,
                colors = colors,
                leadingContent = { Icon(detail.icon, contentDescription = null) },
                trailingContent = { ExpandArrow(detail.open, Modifier) },
            ) { Text(detail.text()) }
        }

        is RunDetail.Received -> GameItem(
            detail.game,
            stringResource(Res.string.sync__received_game, service, detail.game.statusText()),
            shapes,
            onOpen = { open(detail.game) },
        )

        is RunDetail.Sent -> GameItem(
            detail.game,
            stringResource(Res.string.sync__sent_game, service, detail.game.statusText()),
            shapes,
            onOpen = { open(detail.game) },
        )

        is RunDetail.Skipped -> {
            val write = detail.write
            val game = SyncGame(write.gameId ?: 0, write.name, write.coverImageId)
            GameItem(
                game,
                stringResource(write.reason.label),
                shapes,
                onOpen = write.gameId?.let { { open(game) } },
            )
        }

        is RunDetail.Removed -> {
            val game = detail.game
            val removed = stringResource(Res.string.sync__removed_title)
            // Its section in the Library, so the user knows what deleting it here would drop.
            val supporting = game.status?.let { "$removed · ${game.statusText()}" } ?: removed
            GameItem(game, supporting, shapes, onOpen = { open(game) }) {
                RemovedActions(onKeep = { onKeep(game) }, onDelete = { onDelete(game) })
            }
        }
    }
}

@Composable
private fun SyncGame.statusText(): String = status?.let { stringResource(it.displayName) }.orEmpty()

/**
 * A game row: its cover, name and [supporting] line, with any [actions] under it; it opens the game when [onOpen]
 * is set (the sync knew which game it was).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameItem(
    game: SyncGame,
    supporting: String,
    shapes: ListItemShapes,
    onOpen: (() -> Unit)?,
    actions: (@Composable () -> Unit)? = null,
) {
    val colors = ListItemDefaults.expressiveSegmentedColors()
    val leading: @Composable () -> Unit = { GameCover(game.coverImageId, game.name) }
    val supportingContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(supporting)
            actions?.invoke()
        }
    }
    if (onOpen == null) {
        SegmentedListItem(
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            supportingContent = supportingContent,
        ) { Text(game.name) }
    } else {
        SegmentedListItem(
            onClick = onOpen,
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            supportingContent = supportingContent,
        ) { Text(game.name) }
    }
}

/** The two answers for a game the service removed, spelled out: kept here, or deleted here too. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RemovedActions(onKeep: () -> Unit, onDelete: () -> Unit) {
    val height = ButtonDefaults.ExtraSmallContainerHeight
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onKeep,
            shapes = ButtonDefaults.shapesFor(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            modifier = Modifier.heightIn(min = height),
        ) {
            Icon(
                Icons.CheckW500Rounded,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
            )
            Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
            Text(stringResource(Res.string.sync__removed_keep), style = ButtonDefaults.textStyleFor(height))
        }
        FilledTonalButton(
            onClick = onDelete,
            shapes = ButtonDefaults.shapesFor(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
            modifier = Modifier.heightIn(min = height),
        ) {
            Icon(
                Icons.DeleteW500Rounded,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
            )
            Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
            Text(stringResource(Res.string.sync__removed_delete), style = ButtonDefaults.textStyleFor(height))
        }
    }
}
