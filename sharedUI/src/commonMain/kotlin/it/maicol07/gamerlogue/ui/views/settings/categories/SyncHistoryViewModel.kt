package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.core.BaseViewModel
import it.maicol07.gamerlogue.core.LibraryChange
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.services.GameMatcher
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.PendingMatch
import it.maicol07.gamerlogue.services.SyncCoordinator
import it.maicol07.gamerlogue.services.SyncGame
import it.maicol07.gamerlogue.services.SyncHistory
import it.maicol07.gamerlogue.services.SyncKind
import it.maicol07.gamerlogue.services.SyncRun
import it.maicol07.gamerlogue.services.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class SyncHistoryViewModel(
    private val history: SyncHistory,
    private val librarySync: LibrarySync,
    private val libraryChanges: LibraryChanges,
    private val matcher: GameMatcher,
    private val coordinator: SyncCoordinator,
    private val scheduler: SyncScheduler,
) : BaseViewModel() {
    val runs = history.runs
    val running = coordinator.running
    val queue = coordinator.queue

    /** A game the tracker didn't remove when asked, to tell the user; null once they read it. */
    val removalFailed: StateFlow<Pair<SyncRun, SyncGame>?>
        field = MutableStateFlow(null)

    init {
        viewModelScope.launch { fillMissingCovers() }
    }

    /** Opening the history shows every result: none is new any more. */
    fun markAllSeen() = history.markAllSeen()

    /** Keeps [game] where it still is: it's no longer asked about, in this run, any other or later ones. */
    fun keep(run: SyncRun, game: SyncGame) = history.settleRemoved(run.service, run.kind, game.gameId, keep = true)

    /** Removes [game] from the service too, as the user did here. */
    fun removeThere(run: SyncRun, game: SyncGame) = viewModelScope.launch {
        if (!coordinator.removeFromService(run.service, run.kind, game)) removalFailed.value = run to game
    }

    fun dismissRemovalFailed() {
        removalFailed.value = null
    }

    /**
     * [match] is its guess: remembered, so later syncs take it as certain, and synced soon, as a library change is.
     */
    fun confirm(run: SyncRun, match: PendingMatch) {
        val guess = match.guess ?: return
        matcher.memory.remember(run.service, mapOf(match.uid to guess.gameId))
        history.settleMatch(run.service, match.uid, ignore = false)
        scheduler.changed(wishlist = run.kind == SyncKind.WISHLIST)
    }

    /** Leaves [match] out of the syncs for good: no later run asks about it. */
    fun ignore(run: SyncRun, match: PendingMatch) = history.settleMatch(run.service, match.uid, ignore = true)

    /** Deletes [game] from the library, as the service did; settled once the entry is gone. */
    fun delete(run: SyncRun, game: SyncGame) = viewModelScope.launch {
        // ponytail: loads the whole library to find one entry; filter by game on the server if this gets slow.
        val entries = safeRequest { librarySync.allEntries() }.get() ?: return@launch
        val entry = entries.firstOrNull { it.gameId == game.gameId }
        if (entry == null) {
            // Already gone (deleted meanwhile, here or elsewhere): nothing left to settle.
            history.settleRemoved(run.service, run.kind, game.gameId, keep = false)
            return@launch
        }
        if (safeRequest { entry.destroy() }.isOk) {
            history.settleRemoved(run.service, run.kind, game.gameId, keep = false)
            libraryChanges.notifyChanged(LibraryChange.Other)
        }
    }

    /**
     * Runs saved before the history kept covers list their games without: look them up once and store them.
     *
     * ponytail: a game IGDB has no cover for is looked up again on every visit; remember the misses if that adds up.
     */
    private suspend fun fillMissingCovers() {
        val ids = history.runs.value.flatMap { it.removedOnService + it.removedHere + it.received + it.sent }
            .filter { it.coverImageId == null }
            .map { it.gameId }
            .distinct()
        if (ids.isEmpty()) return
        val games = safeRequest { matcher.gamesByIds(ids) }.get().orEmpty()
        val covers = games.mapNotNull { game -> game.cover?.image_id?.let { game.id.toInt() to it } }.toMap()
        if (covers.isNotEmpty()) history.fillCovers(covers)
    }
}
