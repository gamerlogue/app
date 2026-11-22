package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_cancel
import gamerlogue.sharedui.generated.resources.common_ok
import gamerlogue.sharedui.generated.resources.sync__removed_here_confirm_text
import gamerlogue.sharedui.generated.resources.sync__removed_here_confirm_title
import gamerlogue.sharedui.generated.resources.sync__removed_here_failed_text
import gamerlogue.sharedui.generated.resources.sync__removed_here_failed_title
import gamerlogue.sharedui.generated.resources.sync__removed_here_remove
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DeleteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import it.maicol07.gamerlogue.services.SyncGame
import it.maicol07.gamerlogue.services.SyncRun
import org.jetbrains.compose.resources.stringResource

/**
 * The dialogs of removing a game from the service, as the user did from Gamerlogue: confirming the one [asking], then
 * telling when the service didn't remove it. [onAnswered] closes the first.
 */
@Composable
internal fun RemoveThereDialogs(
    viewModel: SyncHistoryViewModel,
    asking: Pair<SyncRun, SyncGame>?,
    onAnswered: () -> Unit,
) {
    asking?.let { (run, game) ->
        RemoveThereDialog(
            game,
            stringResource(run.service.labelRes()),
            onConfirm = {
                onAnswered()
                viewModel.removeThere(run, game)
            },
            onDismiss = onAnswered,
        )
    }
    val failed by viewModel.removalFailed.collectAsStateWithLifecycle()
    failed?.let { (run, game) ->
        RemovalFailedDialog(game, stringResource(run.service.labelRes()), onDismiss = viewModel::dismissRemovalFailed)
    }
}

/** Asks before removing [game], removed from Gamerlogue, from the [service] too. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RemoveThereDialog(game: SyncGame, service: String, onConfirm: () -> Unit, onDismiss: () -> Unit) =
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.DeleteW500Rounded, contentDescription = null) },
        title = { Text(stringResource(Res.string.sync__removed_here_confirm_title, game.name, service)) },
        text = { Text(stringResource(Res.string.sync__removed_here_confirm_text, service)) },
        confirmButton = {
            TextButton(onClick = onConfirm, shapes = ButtonDefaults.shapes()) {
                Text(
                    stringResource(Res.string.sync__removed_here_remove, service),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )

/** Tells that the [service] didn't remove [game]: it stays listed, to try again. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RemovalFailedDialog(game: SyncGame, service: String, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.ErrorW500Rounded, contentDescription = null) },
    title = { Text(stringResource(Res.string.sync__removed_here_failed_title)) },
    text = { Text(stringResource(Res.string.sync__removed_here_failed_text, service, game.name)) },
    confirmButton = {
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(Res.string.common_ok)) }
    },
)
