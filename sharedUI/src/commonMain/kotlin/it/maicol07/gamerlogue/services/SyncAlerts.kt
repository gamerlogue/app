package it.maicol07.gamerlogue.services

import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.sync__notification_attention
import gamerlogue.sharedui.generated.resources.sync__notification_conflicts
import gamerlogue.sharedui.generated.resources.sync__notification_failed
import gamerlogue.sharedui.generated.resources.sync__notification_login
import gamerlogue.sharedui.generated.resources.sync__notification_login_text
import gamerlogue.sharedui.generated.resources.sync__notification_removed
import gamerlogue.sharedui.generated.resources.sync__notification_to_confirm
import it.maicol07.gamerlogue.core.LocalNotifier
import it.maicol07.gamerlogue.core.NotificationChannel
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.settings.categories.labelRes
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Single

/**
 * What the background syncs tell the user once they end, only when something needs a look: a service that signed
 * out gets a notification of its own, opening the linked services to sign in again; the rest share one, opening the
 * history. Things the previous run of the same sync already listed aren't told again: they'd come back every run.
 */
@Single
class SyncAlerts(private val notifier: LocalNotifier, private val history: SyncHistory) {
    suspend fun finished(runs: List<SyncRun>) {
        val lines = mutableListOf<String>()
        for (run in runs) {
            val before = history.previous(run)
            if (run.result != SyncResult.LOGIN_REQUIRED) {
                lines += run.attention(before)
            } else if (before?.result != SyncResult.LOGIN_REQUIRED) {
                val name = getString(run.service.labelRes())
                notifier.notify(
                    NotificationChannel.SYNC_LOGIN,
                    tag = "sync.login.${run.service.name}",
                    title = getString(Res.string.sync__notification_login, name),
                    text = getString(Res.string.sync__notification_login_text, name),
                    target = RootNavTree.LinkedServices,
                )
            }
        }
        if (lines.isEmpty()) return
        notifier.notify(
            NotificationChannel.SYNC,
            tag = "sync.attention",
            title = getString(Res.string.sync__notification_attention),
            text = lines.joinToString("\n"),
            target = RootNavTree.SyncHistory,
        )
    }

    /**
     * What of [this] run needs a look, after the [before] one, as notification lines: a failed run has only its failure;
     * else one line for each kind of thing to look at, none when nothing is.
     */
    private suspend fun SyncRun.attention(before: SyncRun?): List<String> {
        val name = getString(service.labelRes())
        val removed = (removedOnService + removedHere).map { it.gameId } -
            before?.let { (it.removedOnService + it.removedHere).map { g -> g.gameId }.toSet() }.orEmpty()
        val toConfirm = toConfirm.map { it.uid } - before?.toConfirm?.map { it.uid }.orEmpty().toSet()
        suspend fun count(resource: PluralStringResource, n: Int) =
            if (n > 0) getPluralString(resource, n, name, n) else null
        return when (result) {
            SyncResult.FAILED -> listOf(getString(Res.string.sync__notification_failed, name))
            else -> listOfNotNull(
                count(Res.plurals.sync__notification_removed, removed.size),
                count(Res.plurals.sync__notification_to_confirm, toConfirm.size),
                count(Res.plurals.sync__notification_conflicts, conflicts),
            )
        }
    }
}
