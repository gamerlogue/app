package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_ok
import gamerlogue.sharedui.generated.resources.sync__help_conflicts
import gamerlogue.sharedui.generated.resources.sync__help_failed
import gamerlogue.sharedui.generated.resources.sync__help_library
import gamerlogue.sharedui.generated.resources.sync__help_none
import gamerlogue.sharedui.generated.resources.sync__help_removed
import gamerlogue.sharedui.generated.resources.sync__help_removed_here
import gamerlogue.sharedui.generated.resources.sync__help_skipped
import gamerlogue.sharedui.generated.resources.sync__help_to_confirm
import gamerlogue.sharedui.generated.resources.sync__help_trigger_change
import gamerlogue.sharedui.generated.resources.sync__help_trigger_manual
import gamerlogue.sharedui.generated.resources.sync__help_trigger_periodic
import gamerlogue.sharedui.generated.resources.sync__help_wishlist
import gamerlogue.sharedui.generated.resources.sync__login_required_hint
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HelpW500Rounded
import it.maicol07.gamerlogue.services.SyncKind
import it.maicol07.gamerlogue.services.SyncResult
import it.maicol07.gamerlogue.services.SyncRun
import it.maicol07.gamerlogue.services.SyncTrigger
import org.jetbrains.compose.resources.stringResource

/** What happened in [run], in plain words: what started it, what that kind of sync does, and what each part means. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RunHelpDialog(run: SyncRun, onDismiss: () -> Unit) {
    val service = stringResource(run.service.labelRes())
    val paragraphs = listOf(run.startText(service), run.kindText(service)) + run.outcomeTexts(service) + run.decisionTexts(service)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.HelpW500Rounded, contentDescription = null) },
        title = { Text("$service · ${stringResource(run.kind.label)}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                paragraphs.forEach { Text(it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) { Text(stringResource(Res.string.common_ok)) }
        },
    )
}

/** What started the run. */
@Composable
private fun SyncRun.startText(service: String): String = when (trigger) {
    SyncTrigger.MANUAL -> stringResource(Res.string.sync__help_trigger_manual)
    SyncTrigger.CHANGE -> stringResource(Res.string.sync__help_trigger_change, service)
    SyncTrigger.PERIODIC -> stringResource(Res.string.sync__help_trigger_periodic, service)
}

/** What this kind of sync does. */
@Composable
private fun SyncRun.kindText(service: String): String = when (kind) {
    SyncKind.LIBRARY -> stringResource(Res.string.sync__help_library, service)
    SyncKind.WISHLIST -> stringResource(Res.string.sync__help_wishlist, service)
}

/** What each part of the outcome means, for the parts this run has. */
@Composable
private fun SyncRun.outcomeTexts(service: String): List<String> = buildList {
    when (result) {
        SyncResult.LOGIN_REQUIRED -> add(stringResource(Res.string.sync__login_required_hint))
        SyncResult.FAILED -> add(stringResource(Res.string.sync__help_failed))
        else -> Unit
    }
    val quiet = added == 0 && pushed == 0 && conflicts == 0 && skipped.isEmpty() && !hasDecisions
    if (result == SyncResult.OK && quiet) add(stringResource(Res.string.sync__help_none, service))
    if (conflicts > 0) add(stringResource(Res.string.sync__help_conflicts))
    if (skipped.isNotEmpty()) add(stringResource(Res.string.sync__help_skipped, service))
}

private val SyncRun.hasDecisions get() = removedOnService.isNotEmpty() || removedHere.isNotEmpty() || toConfirm.isNotEmpty()

/** What each kind of game left to the user means, for the kinds this run has. */
@Composable
private fun SyncRun.decisionTexts(service: String): List<String> = buildList {
    if (removedOnService.isNotEmpty()) add(stringResource(Res.string.sync__help_removed, service))
    if (removedHere.isNotEmpty()) add(stringResource(Res.string.sync__help_removed_here, service))
    if (toConfirm.isNotEmpty()) add(stringResource(Res.string.sync__help_to_confirm, service))
}
