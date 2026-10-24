package it.maicol07.gamerlogue.extensions

import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.spraypaintkt.Scope
import it.maicol07.spraypaintkt.SortDirection

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
 * Scope returning [userId]'s library entries, optionally filtered by [status]. The backend answers
 * 403 (`profile_private`/`user_blocked`) when they are not visible to the current user.
 */
fun LibraryEntry.Companion.userEntries(userId: String, status: GameLibraryStatus? = null): Scope<LibraryEntry> =
    scope {
        if (status != null) {
            where("status", status.name)
        }
        where("user", userId)
    }

/** Scope returning [userId]'s reviewed entries, the most recently reviewed first. */
fun LibraryEntry.Companion.userReviews(userId: String): Scope<LibraryEntry> =
    scope {
        where("user", userId)
        where("has_review", "true")
        order("review_updated_at", SortDirection.DESC)
    }

/**
 * Scope returning every user's reviews of [gameId] (the base game), the most liked first, with their
 * authors; private profiles and blocked users are left out by the server.
 */
fun LibraryEntry.Companion.gameReviews(gameId: Int): Scope<LibraryEntry> =
    scope {
        where("game_id", gameId)
        where("has_review", "true")
        order("likes_count", SortDirection.DESC)
        order("review_updated_at", SortDirection.DESC)
    }.includes("user")

/**
 * Scope returning the authenticated user's library entry for [gameId], if any.
 */
fun LibraryEntry.Companion.currentUserEntryForGame(gameId: Number): Scope<LibraryEntry> =
    scope {
        where("game_id", gameId)
        extraParam(CurrentUserParam, "true")
    }

/**
 * Build (without persisting) a quick library entry for [game] with [status]. The owner is never set:
 * the server assigns it from the authenticated token and rejects a client-sent `user` relationship.
 * Reuses [existing] when present so an update keeps the same id. [platformsIds] (derived from the
 * store connector) is applied only when the entry has none yet, so a re-import never clobbers a
 * platform selection the user made by hand in the add/edit sheet.
 */
fun LibraryEntry.Companion.quickDraft(
    game: Game,
    status: GameLibraryStatus,
    existing: LibraryEntry? = null,
    platformsIds: List<Int> = emptyList(),
): LibraryEntry = (existing ?: LibraryEntry()).apply {
    gameId = game.id.toInt()
    owned = false
    this.status = status
    // platformsIds throws NoSuchElementException on a never-set attribute rather than defaulting
    // to an empty list, so treat "not found" the same as "empty" here.
    val hasPlatforms = runCatching { this.platformsIds.isNotEmpty() }.getOrDefault(false)
    if (platformsIds.isNotEmpty() && !hasPlatforms) {
        this.platformsIds = platformsIds
    }
}
