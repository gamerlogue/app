package it.maicol07.gamerlogue.extensions

import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.safeRequest
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.spraypaintkt.Scope

/**
 * Reusable JSON:API query builders for [LibraryEntry].
 *
 * Centralizes the server-side parameter conventions (current-user scoping, status/game
 * filters) so call sites no longer repeat raw scope-building boilerplate or magic strings.
 */

// Server-side flag that scopes library entries to the authenticated user.
private const val CurrentUserParam = "current_user"

/**
 * Scope returning the authenticated user's library entries, optionally filtered by [status].
 */
fun LibraryEntry.Companion.currentUserEntries(status: GameLibraryStatus? = null): Scope<LibraryEntry> =
    scope {
        if (status != null) {
            where("status", status.name)
        }
        extraParam(CurrentUserParam, "true")
    }

/**
 * Scope returning the authenticated user's library entry for [gameId], if any.
 */
fun LibraryEntry.Companion.currentUserEntryForGame(gameId: Number): Scope<LibraryEntry> =
    scope {
        where("game_id", gameId)
        extraParam(CurrentUserParam, "true")
    }

/** Persist this entry (create or update) through the JSON:API backend. */
suspend fun LibraryEntry.persist() = safeRequest { save() }

/** Delete this entry from the JSON:API backend. */
suspend fun LibraryEntry.remove() = safeRequest { destroy() }

/**
 * Build (without persisting) a quick library entry for [game] with [status], owned by [user].
 * Reuses [existing] when present so an update keeps the same id.
 */
fun LibraryEntry.Companion.quickDraft(
    game: Game,
    status: GameLibraryStatus,
    user: User?,
    existing: LibraryEntry? = null,
): LibraryEntry = (existing ?: LibraryEntry()).apply {
    gameId = game.id.toInt()
    owned = false
    this.user = user
    this.status = status
}
