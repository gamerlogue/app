package it.maicol07.gamerlogue.services

import com.russhwolf.settings.ObservableSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

@Serializable
enum class SyncKind { WISHLIST, LIBRARY }

/** What started a sync: the user on the sync screen, a library change, or the periodic pull. */
@Serializable
enum class SyncTrigger { MANUAL, CHANGE, PERIODIC }

@Serializable
enum class SyncResult { OK, PARTIAL, FAILED, LOGIN_REQUIRED }

/**
 * One sync with a service as the history shows it. [added] counts the games added or updated in Gamerlogue and
 * [received] lists them, [pushed] and [sent] the ones written to the service (runs older than the lists only have
 * the counts); [removedOnService] are the games the service no longer has that the user hasn't settled yet. A run
 * is [seen] once the user opened the history after it (a manual one at once).
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
)

/** The last [LIMIT] sync runs, newest first, kept in the settings. */
@Single
class SyncHistory(private val settings: ObservableSettings) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(SyncRun.serializer())

    val runs: StateFlow<List<SyncRun>>
        field = MutableStateFlow(load())

    private fun load(): List<SyncRun> {
        val stored = settings.getStringOrNull(KEY) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, stored) }.getOrDefault(emptyList())
    }

    fun add(run: SyncRun) = save { listOf(run) + it }

    fun markAllSeen() = save { runs -> runs.map { it.copy(seen = true) } }

    /**
     * The user settled [gameId], removed from [service] (deleted it here, or chose to keep it): it leaves every run of
     * that service, since they all asked about the same game.
     */
    fun settleRemoved(service: ExternalService, gameId: Int) = save { runs ->
        runs.map { run ->
            if (run.service == service) {
                run.copy(removedOnService = run.removedOnService.filterNot { it.gameId == gameId })
            } else {
                run
            }
        }
    }

    /** Fill in the cover of every listed game with none yet, from [covers] by IGDB id. */
    fun fillCovers(covers: Map<Int, String>) = save { runs ->
        val fill = { games: List<SyncGame> ->
            games.map { game -> game.coverImageId?.let { game } ?: game.copy(coverImageId = covers[game.gameId]) }
        }
        runs.map { run ->
            run.copy(
                removedOnService = fill(run.removedOnService),
                received = fill(run.received),
                sent = fill(run.sent),
            )
        }
    }

    private fun save(change: (List<SyncRun>) -> List<SyncRun>) {
        runs.update { change(it).take(LIMIT) }
        settings.putString(KEY, json.encodeToString(serializer, runs.value))
    }

    private companion object {
        const val KEY = "sync.history"
        const val LIMIT = 50
    }
}
