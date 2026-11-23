package it.maicol07.gamerlogue.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import org.koin.core.annotation.Single

@Serializable
enum class SyncKind { WISHLIST, LIBRARY }

/** What started a sync: the user on the sync screen, a library change, or the periodic pull. */
@Serializable
enum class SyncTrigger { MANUAL, CHANGE, PERIODIC }

@Serializable
enum class SyncResult { OK, PARTIAL, FAILED, LOGIN_REQUIRED, CANCELLED, INTERRUPTED }

/**
 * One sync with a service as the history shows it. [added] counts the games added or updated in Gamerlogue and
 * [received] lists them, [pushed] and [sent] the ones written to the service (runs older than the lists only have
 * the counts). Still to settle by the user: [removedOnService], the games the service no longer has;
 * [removedHere], the ones removed from Gamerlogue still on the service; [toConfirm], the service games matched only
 * by a guess. A run is [seen] once the user opened the history after it (a manual one at once).
 */
@Serializable
data class SyncRun(
    val id: String,
    val service: ExternalService,
    val kind: SyncKind,
    val trigger: SyncTrigger,
    val startedAt: Long,
    val endedAt: Long,
    val result: SyncResult,
    val added: Int = 0,
    val pushed: Int = 0,
    val skipped: List<SkippedWrite> = emptyList(),
    val conflicts: Int = 0,
    val removedOnService: List<SyncGame> = emptyList(),
    val received: List<SyncGame> = emptyList(),
    val sent: List<SyncGame> = emptyList(),
    val error: String? = null,
    val seen: Boolean = false,
    val removedHere: List<SyncGame> = emptyList(),
    val toConfirm: List<PendingMatch> = emptyList(),
)

/**
 * What the user settled, so later runs don't ask again: the removed games they [kept] (by service and kind, since a
 * library and a wishlist remove different games) and the service games they [ignored].
 */
@Serializable
private data class Decisions(
    val kept: Map<String, Set<Int>> = emptyMap(),
    val ignored: Map<ExternalService, Set<String>> = emptyMap(),
)

/** The [runs] and the [decisions] about them, one document so a change to both is one write. */
@Serializable
private data class History(val runs: List<SyncRun> = emptyList(), val decisions: Decisions = Decisions())

/** The last [LIMIT] sync runs, newest first, and the answers the user gave about them. */
@Single
class SyncHistory(storage: SyncStorage) {
    private val store = storage.dataStore("sync-history", History.serializer(), History())

    val runs: Flow<List<SyncRun>> = store.data.map { it.runs }

    /**
     * Records [run] without what the user already settled. A kept game the run no longer reports as removed is
     * forgotten, so removing it again later asks again; a failed run reports nothing, so it forgets nothing.
     * Returns the run as recorded.
     */
    suspend fun add(run: SyncRun): SyncRun = store.updateData { (runs, decisions) ->
        val key = keptKey(run.service, run.kind)
        val kept = decisions.kept[key].orEmpty()
        val ignored = decisions.ignored[run.service].orEmpty()
        val settled = run.copy(
            removedOnService = run.removedOnService.filterNot { it.gameId in kept },
            removedHere = run.removedHere.filterNot { it.gameId in kept },
            toConfirm = run.toConfirm.filterNot { it.uid in ignored },
        )
        History(
            runs = (listOf(settled) + runs).take(LIMIT),
            decisions = if (run.result == SyncResult.OK || run.result == SyncResult.PARTIAL) {
                val removed = (run.removedOnService + run.removedHere).mapTo(mutableSetOf()) { it.gameId }
                decisions.copy(kept = decisions.kept + (key to (kept intersect removed)))
            } else {
                decisions
            },
        )
    }.runs.first()

    /** The run of [run]'s service and kind before it, if the history still has one. */
    suspend fun previous(run: SyncRun): SyncRun? =
        runs.first().firstOrNull { it.service == run.service && it.kind == run.kind && it.startedAt < run.startedAt }

    suspend fun markAllSeen() = updateRuns { runs -> runs.map { it.copy(seen = true) } }

    /**
     * The user settled [gameId], removed on one side of [service]'s [kind] sync: it leaves every such run, since they
     * all asked about the same game. Kept on both sides ([keep]), later runs don't ask about it again either.
     */
    suspend fun settleRemoved(service: ExternalService, kind: SyncKind, gameId: Int, keep: Boolean) {
        store.updateData { (runs, decisions) ->
            val key = keptKey(service, kind)
            History(
                runs = runs.map { run ->
                    if (run.service == service && run.kind == kind) {
                        run.copy(
                            removedOnService = run.removedOnService.filterNot { it.gameId == gameId },
                            removedHere = run.removedHere.filterNot { it.gameId == gameId },
                        )
                    } else {
                        run
                    }
                },
                decisions = if (keep) {
                    decisions.copy(kept = decisions.kept + (key to decisions.kept[key].orEmpty() + gameId))
                } else {
                    decisions
                },
            )
        }
    }

    /** The user answered about [service]'s game [uid]: it leaves every run; [ignore]d, no later run asks again. */
    suspend fun settleMatch(service: ExternalService, uid: String, ignore: Boolean) {
        store.updateData { (runs, decisions) ->
            History(
                runs = runs.map { run ->
                    if (run.service == service) run.copy(toConfirm = run.toConfirm.filterNot { it.uid == uid }) else run
                },
                decisions = if (ignore) {
                    val ignored = decisions.ignored[service].orEmpty() + uid
                    decisions.copy(ignored = decisions.ignored + (service to ignored))
                } else {
                    decisions
                },
            )
        }
    }

    /** Fill in the cover of every listed game with none yet, from [covers] by IGDB id. */
    suspend fun fillCovers(covers: Map<Int, String>) = updateRuns { runs ->
        val fill = { games: List<SyncGame> ->
            games.map { game -> game.coverImageId?.let { game } ?: game.copy(coverImageId = covers[game.gameId]) }
        }
        runs.map { run ->
            run.copy(
                removedOnService = fill(run.removedOnService),
                removedHere = fill(run.removedHere),
                received = fill(run.received),
                sent = fill(run.sent),
            )
        }
    }

    private suspend fun updateRuns(change: (List<SyncRun>) -> List<SyncRun>) {
        store.updateData { it.copy(runs = change(it.runs)) }
    }

    private fun keptKey(service: ExternalService, kind: SyncKind) = "$service:$kind"

    private companion object {
        const val LIMIT = 50
    }
}
