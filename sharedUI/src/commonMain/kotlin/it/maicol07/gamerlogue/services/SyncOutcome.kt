package it.maicol07.gamerlogue.services

import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.serialization.Serializable

/**
 * A game a sync touched, as the history shows it: its IGDB [gameId], [name] and cover, and the [status] it has in
 * the library (received from the service), was sent with, or, for one the service removed, still has here.
 */
@Serializable
data class SyncGame(
    val gameId: Int,
    val name: String,
    val coverImageId: String? = null,
    val status: GameLibraryStatus? = null,
)

/** How a store flow ended, for the sync screen's closing message and the sync history. */
sealed interface SyncOutcome {
    /**
     * [added] entries created in Gamerlogue, [pushed] backlog games sent to the store wishlist, [skipped]
     * selected games the store didn't take, [removedOnOneSide] games removed on one side and left on the other
     * (reported only alongside a change: they'd stop every later sync otherwise), [removedOnService] the ones of
     * those removed from the store wishlist; [received] and [sent] the games behind [added] and [pushed].
     */
    data class WishlistSynced(
        val added: Int,
        val pushed: Int,
        val skipped: List<SkippedWrite>,
        val removedOnOneSide: Int,
        val removedOnService: List<SyncGame> = emptyList(),
        val received: List<SyncGame> = emptyList(),
        val sent: List<SyncGame> = emptyList(),
    ) : SyncOutcome {
        /** False when the two libraries already agreed — nothing worth stopping to report. */
        val changedAnything: Boolean get() = added > 0 || pushed > 0 || skipped.isNotEmpty()
    }

    /**
     * [pulled] games updated from the tracker, [pushed] written to it, [skipped] selected writes the tracker
     * didn't take, [conflicts] left for a manual sync, [removedOnOneSide], [removedOnService], [received] and [sent]
     * as in [WishlistSynced].
     */
    data class LibrarySynced(
        val pulled: Int,
        val pushed: Int,
        val skipped: List<SkippedWrite>,
        val conflicts: Int,
        val removedOnOneSide: Int,
        val removedOnService: List<SyncGame> = emptyList(),
        val received: List<SyncGame> = emptyList(),
        val sent: List<SyncGame> = emptyList(),
    ) : SyncOutcome {
        val changedAnything: Boolean get() = pulled > 0 || pushed > 0 || skipped.isNotEmpty() || conflicts > 0
    }

    data object Failed : SyncOutcome
    data class IdsResynced(val updated: Int, val unmatched: Int) : SyncOutcome
}
