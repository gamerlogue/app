package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.sync__removed_delete
import gamerlogue.sharedui.generated.resources.sync__removed_here_keep
import gamerlogue.sharedui.generated.resources.sync__removed_here_remove
import gamerlogue.sharedui.generated.resources.sync__removed_here_title
import gamerlogue.sharedui.generated.resources.sync__removed_keep
import gamerlogue.sharedui.generated.resources.sync__removed_title
import gamerlogue.sharedui.generated.resources.sync__to_confirm_confirm
import gamerlogue.sharedui.generated.resources.sync__to_confirm_guess
import gamerlogue.sharedui.generated.resources.sync__to_confirm_ignore
import gamerlogue.sharedui.generated.resources.sync__to_confirm_none
import gamerlogue.sharedui.generated.resources.sync__to_confirm_pick
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DeleteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SearchW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.VisibilityOffW500Rounded
import it.maicol07.gamerlogue.services.PendingMatch
import it.maicol07.gamerlogue.services.SyncGame
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What the user can answer about a game the history lists, each settling it in its own way. */
internal class GameAnswers(
    val delete: (SyncGame) -> Unit,
    val keep: (SyncGame) -> Unit,
    val removeThere: (SyncGame) -> Unit,
    val confirm: (PendingMatch) -> Unit,
    val ignore: (PendingMatch) -> Unit,
    val pick: () -> Unit,
)

/**
 * A game removed on one side ([title]), with its status in the Library, so the user knows what removing it would
 * drop: [keep] it where it still is, or [remove] it there too.
 */
@Composable
private fun RemovedItem(
    game: SyncGame,
    title: StringResource,
    shapes: ListItemShapes,
    open: (SyncGame) -> Unit,
    keep: Answer,
    remove: Answer,
) {
    val removed = stringResource(title)
    val supporting = game.status?.let { "$removed · ${game.statusText()}" } ?: removed
    GameItem(game, supporting, shapes, onOpen = { open(game) }) { Answers(listOf(keep, remove)) }
}

/** A game the service removed: keep it here, or delete it here too. */
@Composable
internal fun RemovedOnServiceItem(
    game: SyncGame,
    shapes: ListItemShapes,
    open: (SyncGame) -> Unit,
    answers: GameAnswers,
) = RemovedItem(
    game,
    Res.string.sync__removed_title,
    shapes,
    open,
    Answer(Res.string.sync__removed_keep, Icons.CheckW500Rounded) { answers.keep(game) },
    Answer(Res.string.sync__removed_delete, Icons.DeleteW500Rounded, destructive = true) { answers.delete(game) },
)

/** A game removed from Gamerlogue still on the [service]: keep it there, or remove it there too. */
@Composable
internal fun RemovedHereItem(
    game: SyncGame,
    service: String,
    shapes: ListItemShapes,
    open: (SyncGame) -> Unit,
    answers: GameAnswers,
) = RemovedItem(
    game,
    Res.string.sync__removed_here_title,
    shapes,
    open,
    Answer(Res.string.sync__removed_here_keep, Icons.CheckW500Rounded, service) { answers.keep(game) },
    Answer(Res.string.sync__removed_here_remove, Icons.DeleteW500Rounded, service, destructive = true) {
        answers.removeThere(game)
    },
)

/** A service game matched only by a guess: confirm the guess, pick another in a reviewed sync, or ignore it. */
@Composable
internal fun ToConfirmItem(
    match: PendingMatch,
    service: String,
    shapes: ListItemShapes,
    open: (SyncGame) -> Unit,
    answers: GameAnswers,
) {
    val guess = match.guess
    val supporting = guess?.let { stringResource(Res.string.sync__to_confirm_guess, service, match.name, it.name) }
        ?: stringResource(Res.string.sync__to_confirm_none, service, match.name)
    GameItem(guess ?: SyncGame(0, match.name), supporting, shapes, onOpen = guess?.let { { open(it) } }) {
        val confirm = guess?.let {
            Answer(Res.string.sync__to_confirm_confirm, Icons.CheckW500Rounded) { answers.confirm(match) }
        }
        val pick = Answer(Res.string.sync__to_confirm_pick, Icons.SearchW500Rounded, onClick = answers.pick)
        val ignore = Answer(Res.string.sync__to_confirm_ignore, Icons.VisibilityOffW500Rounded) {
            answers.ignore(match)
        }
        Answers(listOfNotNull(confirm, pick, ignore))
    }
}

/** One button under a game: its [label] (formatted with [arg] when given), [icon], and whether it drops something. */
private class Answer(
    val label: StringResource,
    val icon: ImageVector,
    val arg: String? = null,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/** The [answers] for a game, spelled out as small buttons: a destructive one stands out in the error colours. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Answers(answers: List<Answer>) {
    val height = ButtonDefaults.ExtraSmallContainerHeight
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        answers.forEach { answer ->
            val content: @Composable RowScope.() -> Unit = {
                Icon(
                    answer.icon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
                )
                Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                val label = answer.arg?.let { stringResource(answer.label, it) } ?: stringResource(answer.label)
                Text(label, style = ButtonDefaults.textStyleFor(height))
            }
            if (answer.destructive) {
                FilledTonalButton(
                    onClick = answer.onClick,
                    shapes = ButtonDefaults.shapesFor(height),
                    contentPadding = ButtonDefaults.contentPaddingFor(height),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.heightIn(min = height),
                    content = content,
                )
            } else {
                OutlinedButton(
                    onClick = answer.onClick,
                    shapes = ButtonDefaults.shapesFor(height),
                    contentPadding = ButtonDefaults.contentPaddingFor(height),
                    modifier = Modifier.heightIn(min = height),
                    content = content,
                )
            }
        }
    }
}
