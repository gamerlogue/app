package it.maicol07.gamerlogue.core

import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.koin.core.annotation.Single

/** What changed in the library, for the background sync to pick what to push. */
sealed interface LibraryChange {
    /** The user saved an entry, now with [status]. */
    data class Saved(val status: GameLibraryStatus) : LibraryChange

    /** A change the background sync pushes nothing for: a removal (never propagated), a review, a sync's own writes. */
    data object Other : LibraryChange
}

/**
 * Tells screens that keep library data alive (e.g. the calendar's backlog filter) that the user's
 * library entries changed, since they are not refetched when the screen shows again.
 */
@Single
class LibraryChanges {
    val changes: SharedFlow<LibraryChange>
        field = MutableSharedFlow<LibraryChange>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Call after an entry is saved or deleted; a burst of changes collapses into one signal per collector. */
    fun notifyChanged(change: LibraryChange) {
        changes.tryEmit(change)
    }
}
