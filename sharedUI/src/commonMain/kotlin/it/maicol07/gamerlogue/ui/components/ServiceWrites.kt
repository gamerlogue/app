package it.maicol07.gamerlogue.ui.components

import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ScriptWriteException
import it.maicol07.gamerlogue.services.StatusChange
import it.maicol07.gamerlogue.services.WebStep

/** Checkpoint confirmed writes before starting another bounded WebView script. */
internal suspend fun <T> WebSession.writeBatches(
    games: List<T>,
    step: (List<T>) -> WebStep,
    record: (List<T>, List<ExternalGameRef>) -> Unit,
): Int {
    var written = 0
    var done = 0
    val batches = games.chunked(WRITE_BATCH_SIZE)
    for ((index, batch) in batches.withIndex()) {
        beginBatch(index + 1, batches.size, done, games.size, batch.size)
        val refs = try {
            run(step(batch))
        } catch (error: ScriptWriteException) {
            record(batch, error.written)
            finishBatch(done + error.written.size, games.size)
            throw error
        }
        record(batch, refs)
        written += refs.size
        done += batch.size
        finishBatch(done, games.size)
    }
    return written
}

private const val WRITE_BATCH_SIZE = 10

/** New tracker entries need an explicit platform; existing entries keep their remote platform. */
internal suspend fun WebSession.confirmLibraryAdditions(changes: List<StatusChange>): List<StatusChange> {
    val additions = changes.filter { it.uid == null }
    if (additions.isEmpty()) return changes
    val chosen = confirmPush(
        additions.map {
            LibrarySync.OutgoingGame(
                it.gameId,
                it.gameId.toString(),
                it.name,
                null,
                null,
                platformIds = it.platformIds,
                platformNames = it.platformNames,
            )
        },
    ).associateBy { it.gameId }
    return changes.mapNotNull { change ->
        if (change.uid != null) change else chosen[change.gameId]?.let { change.copy(platformIds = it.platformIds) }
    }
}
