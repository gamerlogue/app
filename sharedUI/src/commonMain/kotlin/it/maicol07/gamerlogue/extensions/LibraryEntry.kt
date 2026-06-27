package it.maicol07.gamerlogue.extensions

import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.User
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

// Page-based pagination: backend returns one page per request; pages start at 1.
private const val MaxPages = 500

/**
 * Walk every page of this scope, invoking [onPage] with each page's *new* entries as it arrives.
 * [Scope.all] returns only one page. Stops when a page brings no new ids (covers both the end and a
 * backend that ignores the page number and re-serves the same page) or once `meta.totalItems` is
 * reached; [MaxPages] is a final guard.
 */
suspend fun Scope<LibraryEntry>.forEachPage(onPage: suspend (List<LibraryEntry>) -> Unit) {
    val seen = mutableSetOf<String?>()
    var total: Int? = null
    var page = 1
    repeat(MaxPages) {
        val result = page(page).all()
        total = total ?: (result.meta["totalItems"] as? Number)?.toInt()
        val fresh = result.data.filter { seen.add(it.id) }
        if (fresh.isEmpty()) return
        onPage(fresh)
        if (total != null && seen.size >= total) return
        page++
    }
}

/** Collect every page of this scope into one list (see [forEachPage]). */
suspend fun Scope<LibraryEntry>.allPages(): List<LibraryEntry> {
    val out = mutableListOf<LibraryEntry>()
    forEachPage { out += it }
    return out
}

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
