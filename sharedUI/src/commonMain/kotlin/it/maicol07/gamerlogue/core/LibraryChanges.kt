package it.maicol07.gamerlogue.core

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.koin.core.annotation.Single

/**
 * Tells screens that keep library data alive (e.g. the calendar's backlog filter) that the user's
 * library entries changed, since they are not refetched when the screen shows again.
 */
@Single
class LibraryChanges {
    val changes: SharedFlow<Unit>
        field = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Call after an entry is saved or deleted; a burst of changes collapses into one signal per collector. */
    fun notifyChanged() {
        changes.tryEmit(Unit)
    }
}
