package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
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
import gamerlogue.sharedui.generated.resources.sync__removed_here_count
import gamerlogue.sharedui.generated.resources.sync__removed_see_latest
import gamerlogue.sharedui.generated.resources.sync__sent_game
import gamerlogue.sharedui.generated.resources.sync__sign_in_again
import gamerlogue.sharedui.generated.resources.sync__skipped_count
import gamerlogue.sharedui.generated.resources.sync__to_confirm_count
import gamerlogue.sharedui.generated.resources.sync__to_confirm_see_latest
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DeleteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DownloadW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LinkOffW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LoginW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PublishW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.QuizW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WarningW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.PendingMatch
import it.maicol07.gamerlogue.services.SkippedWrite
import it.maicol07.gamerlogue.services.SyncGame
import it.maicol07.gamerlogue.services.SyncResult
import it.maicol07.gamerlogue.services.SyncRun
import it.maicol07.gamerlogue.ui.components.layout.ExpandArrow
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
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

    class RemovedHere(override val key: String, val game: SyncGame) : RunDetail

    class ToConfirm(override val key: String, val match: PendingMatch) : RunDetail

    /** Opens the sign-in of [service], for a run that found it signed out. */
    class SignIn(override val key: String, val service: ExternalService) : RunDetail
}

/**
 * The segments under [run]. The received, sent and skipped games are groups, listed while their key is in [open]
 * (runs from before the lists only have the counts). The games left to the user (removed on one side, to confirm)
 * are listed only when the run is the [latest] of its service and kind, since they are a choice to make on the
 * current state; an older run points there for the ones still pending in it, and says nothing of the ones settled or
 * found since.
 */
internal fun SyncRun.details(latest: SyncRun, open: Set<String>): List<RunDetail> = buildList {
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
        SyncResult.LOGIN_REQUIRED -> {
            add(
                RunDetail.Line(
                    "$id-login",
                    Icons.LoginW500Rounded
                ) { stringResource(Res.string.sync__login_required_hint) }
            )
            add(RunDetail.SignIn("$id-sign-in", service))
        }
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
    if (latest.id == id) {
        decisions("$id-removed", Icons.LinkOffW500Rounded, removedOnService, Res.plurals.sync__removed_count) {
            RunDetail.Removed("$id-removed-${it.gameId}", it)
        }
        decisions("$id-removed-here", Icons.DeleteW500Rounded, removedHere, Res.plurals.sync__removed_here_count) {
            RunDetail.RemovedHere("$id-removed-here-${it.gameId}", it)
        }
        decisions("$id-confirm", Icons.QuizW500Rounded, toConfirm, Res.plurals.sync__to_confirm_count) {
            RunDetail.ToConfirm("$id-confirm-${it.uid}", it)
        }
    } else {
        addAll(stillPending(latest))
    }
    if (isEmpty()) {
        add(RunDetail.Line("$id-none", Icons.CheckCircleW500Rounded) { stringResource(Res.string.sync__no_changes) })
    }
}

/** A line counting the [games] left to the user, followed by them; nothing for none. */
private fun <T> MutableList<RunDetail>.decisions(
    key: String,
    icon: ImageVector,
    games: List<T>,
    label: PluralStringResource,
    row: (T) -> RunDetail,
) {
    if (games.isEmpty()) return
    add(RunDetail.Line(key, icon) { plural(label, games.size) })
    games.forEach { add(row(it)) }
}

/** The lines of an older run pointing to the [latest] one, for its games left to the user still pending there. */
private fun SyncRun.stillPending(latest: SyncRun): List<RunDetail> {
    val removedIds = (latest.removedOnService + latest.removedHere).mapTo(mutableSetOf()) { it.gameId }
    val removed = (removedOnService + removedHere).count { it.gameId in removedIds }
    val uids = latest.toConfirm.mapTo(mutableSetOf()) { it.uid }
    val toConfirm = toConfirm.count { it.uid in uids }
    return listOfNotNull(
        RunDetail.Line("$id-removed", Icons.LinkOffW500Rounded) {
            plural(Res.plurals.sync__removed_see_latest, removed)
        }.takeIf { removed > 0 },
        RunDetail.Line("$id-confirm", Icons.QuizW500Rounded) {
            plural(Res.plurals.sync__to_confirm_see_latest, toConfirm)
        }.takeIf { toConfirm > 0 },
    )
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
 * One segment: [onToggle] opens or closes a group line, [onOpen] opens a game, [answers] settle the games left to the
 * user. [service] names the service in a game's supporting line and buttons.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DetailItem(
    detail: RunDetail,
    service: String,
    shapes: ListItemShapes,
    onToggle: (String) -> Unit,
    onOpen: (RootNavTree.GameDetail) -> Unit,
    answers: GameAnswers,
) {
    val colors = ListItemDefaults.expressiveSegmentedColors()
    val navigationState = LocalNavigationState.current
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

        is RunDetail.Removed -> RemovedOnServiceItem(detail.game, shapes, open, answers)
        is RunDetail.RemovedHere -> RemovedHereItem(detail.game, service, shapes, open, answers)

        is RunDetail.ToConfirm -> ToConfirmItem(detail.match, service, shapes, open, answers)

        is RunDetail.SignIn -> SegmentedListItem(
            onClick = {
                navigationState.backStack.add(RootNavTree.ServiceSync(detail.service, ServiceSyncAction.CONNECT))
            },
            shapes = shapes,
            colors = colors,
            leadingContent = { Icon(Icons.LoginW500Rounded, contentDescription = null) },
        ) { Text(stringResource(Res.string.sync__sign_in_again, service), color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
internal fun SyncGame.statusText(): String = status?.let { stringResource(it.displayName) }.orEmpty()

/**
 * A game row: its cover, name and [supporting] line, with any [actions] under it; it opens the game when [onOpen]
 * is set (the sync knew which game it was).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GameItem(
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
