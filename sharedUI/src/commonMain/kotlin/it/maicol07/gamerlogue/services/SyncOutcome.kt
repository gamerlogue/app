package it.maicol07.gamerlogue.services

import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.serialization.Serializable

/**
 * A game a sync touched, as the history shows it: its IGDB [gameId], [name] and cover, and the [status] it has in
 * the library (received from the service), was sent with, or, for one the service removed, still has here; for one
 * removed here, the status and [uid] it still has on the service.
 */
@Serializable
data class SyncGame(
    val gameId: Int,
    val name: String,
    val coverImageId: String? = null,
    val status: GameLibraryStatus? = null,
    val uid: String? = null,
)

/**
 * A game of the service an unattended sync couldn't tell for sure: its service [uid] and [name], and the IGDB game
 * it most likely is ([guess], null when the search found none). Left out of the sync until the user confirms it.
 */
@Serializable
data class PendingMatch(val uid: String, val name: String, val guess: SyncGame? = null)

/** How a store flow ended, for the sync screen's closing message and the sync history. */
sealed interface SyncOutcome {
    /**
     * [added] entries created in Gamerlogue, [pushed] backlog games sent to the store wishlist, [skipped]
     * selected games the store didn't take, [removedOnOneSide] games removed on one side and left on the other
     * (reported only alongside a change: they'd stop every later sync otherwise), [removedOnService] the ones of
     * those removed from the store wishlist; [received] and [sent] the games behind [added] and [pushed];
     * [toConfirm] the store games matched only by a guess, left out; [removedHere] the games removed from Gamerlogue
     * still on a store wishlist that can remove them too.
     */
    data class WishlistSynced(
        val added: Int,
        val pushed: Int,
        val skipped: List<SkippedWrite>,
        val removedOnOneSide: Int,
        val removedOnService: List<SyncGame> = emptyList(),
        val received: List<SyncGame> = emptyList(),
        val sent: List<SyncGame> = emptyList(),
        val toConfirm: List<PendingMatch> = emptyList(),
        val removedHere: List<SyncGame> = emptyList(),
    ) : SyncOutcome {
        /** False when the two libraries already agreed — nothing worth stopping to report. */
        val changedAnything: Boolean get() = added > 0 || pushed > 0 || skipped.isNotEmpty()
    }

    /**
     * [pulled] games updated from the tracker, [pushed] written to it, [skipped] selected writes the tracker
     * didn't take, [conflicts] left for a manual sync, [removedOnOneSide], [removedOnService], [received] and [sent]
     * and [toConfirm] as in [WishlistSynced]; [removedHere] the games removed from Gamerlogue still on a tracker that
     * can remove them too.
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
        val toConfirm: List<PendingMatch> = emptyList(),
        val removedHere: List<SyncGame> = emptyList(),
    ) : SyncOutcome {
        val changedAnything: Boolean get() = pulled > 0 || pushed > 0 || skipped.isNotEmpty() || conflicts > 0
    }

    data object Failed : SyncOutcome
    data class IdsResynced(val updated: Int, val unmatched: Int) : SyncOutcome
}
