package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Game
import com.russhwolf.settings.ObservableSettings
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.igdb.baseGameId
import it.maicol07.gamerlogue.extensions.igdb.platformNames
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single

/** A game's state as of the last library sync with a tracker: its tracker [uid] and the agreed [status]. */
@Serializable
data class SyncedGame(val uid: String, val status: GameLibraryStatus)

/** Which side's status a [SyncChange] keeps. */
enum class SyncSide { LOCAL, REMOTE }

/**
 * One game whose status differs between Gamerlogue ([local]) and the tracker ([remote]); a null side is a
 * game missing there. [winner] is the side that changed since the last sync, or null for a conflict (both
 * changed, or no previous sync to tell) that the user has to settle.
 */
data class SyncChange(
    val gameId: Int,
    val uid: String?,
    val local: GameLibraryStatus?,
    val remote: GameLibraryStatus?,
    val winner: SyncSide?,
)

/** The [changes] to make and the games already [agreed] on both sides (recorded as the new baseline). */
data class SyncPlan(val changes: List<SyncChange>, val agreed: Map<Int, SyncedGame>)

/**
 * Three-way plan of a library sync. [local] and [remote] are the current statuses by IGDB id (remote as
 * the tracker refs, carrying uid + status); [base] is the state of the last sync.
 *
 * - A game is synced only while its Gamerlogue status is one of [supported]: otherwise the tracker can't
 *   represent it and the two sides would never agree.
 * - A game missing on one side is added there — unless [base] knows it, meaning it was removed on that
 *   side since: removals aren't propagated, so it is left alone.
 * - When both sides differ, the side still equal to [base] lost; if neither (or there is no base), it's a
 *   conflict.
 */
fun planSync(
    local: Map<Int, GameLibraryStatus>,
    remote: Map<Int, ExternalGameRef>,
    base: Map<Int, SyncedGame>,
    supported: Set<GameLibraryStatus>,
): SyncPlan {
    val changes = mutableListOf<SyncChange>()
    val agreed = mutableMapOf<Int, SyncedGame>()
    for (id in local.keys + remote.keys) {
        val l = local[id]
        val ref = remote[id]
        // A tracker game with no status of ours (e.g. on a custom list) counts as backlog.
        val r = ref?.let { it.status ?: GameLibraryStatus.BACKLOG }
        when {
            l != null && l !in supported -> Unit
            ref != null && r != null && r == l -> agreed[id] = SyncedGame(ref.uid, r)
            else -> decide(l, r, base[id])?.let { changes += SyncChange(id, ref?.uid, l, r, it.winner) }
        }
    }
    return SyncPlan(changes, agreed)
}

private enum class Decision(val winner: SyncSide?) { PULL(SyncSide.REMOTE), PUSH(SyncSide.LOCAL), CONFLICT(null) }

/** How [planSync] settles a game whose two sides differ; null = leave it alone (removed on one side since [synced]). */
private fun decide(local: GameLibraryStatus?, remote: GameLibraryStatus?, synced: SyncedGame?): Decision? = when {
    local == null -> Decision.PULL.takeIf { synced == null }
    remote == null -> Decision.PUSH.takeIf { synced == null }
    local == synced?.status -> Decision.PULL
    remote == synced?.status -> Decision.PUSH
    else -> Decision.CONFLICT
}

/** JSON array literal of the changes, for embedding in a [ServiceConnector.statusWrite] script. */
internal fun List<StatusChange>.jsonArray(): String =
    scriptJson.encodeToString(ListSerializer(StatusChange.serializer()), this)

// Defaults are encoded too: the scripts read every field, and an omitted default (e.g. an empty
// `platformIds`) would reach them as `undefined` instead of an empty list.
private val scriptJson = Json { encodeDefaults = true }

/**
 * Tracker library sync: the plan/apply steps shared by the automatic run and the preview, plus the
 * persisted state per service — the last-synced status of each game, which doubles as the confirmed tracker
 * uid → IGDB id mapping (so later syncs don't re-ask for name-matched games) — and the outgoing changes
 * handed from the preview screen to the WebView that writes them.
 */
@Single
class TrackerSync(
    private val settings: ObservableSettings,
    private val matcher: GameMatcher,
    private val librarySync: LibrarySync,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(Int.serializer(), SyncedGame.serializer())

    /** The state of the last sync with [service], by IGDB id; empty before the first one. */
    fun base(service: ExternalService): Map<Int, SyncedGame> =
        settings.getStringOrNull(service.settingsKey(KEY))
            ?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() }
            .orEmpty()

    /** Merge [games] into the stored state of [service]. */
    fun record(service: ExternalService, games: Map<Int, SyncedGame>) {
        if (games.isEmpty()) return
        settings.putString(service.settingsKey(KEY), json.encodeToString(serializer, base(service) + games))
    }

    fun forget(service: ExternalService) = settings.remove(service.settingsKey(KEY))

    /** IGDB games identified by tracker metadata or the last sync, by tracker uid; metadata takes precedence. */
    suspend fun knownGames(service: ExternalService, refs: List<ExternalGameRef>): Map<String, Game> {
        val stored = base(service).entries.associate { (id, g) -> g.uid to id }
        val idByUid = refs.mapNotNull { ref ->
            (ref.gameId?.takeIf { it > 0 } ?: stored[ref.uid])?.let { ref.uid to it }
        }.toMap()
        val ids = refs.mapNotNull { idByUid[it.uid] }
        val games = matcher.gamesByIds(ids).associateBy { it.id.toInt() }
        return refs.mapNotNull { ref -> idByUid[ref.uid]?.let(games::get)?.let { ref.uid to it } }.toMap()
    }

    /** Tracker refs by IGDB id, for an unattended run: synced mappings plus confident matches only — a
     *  name guess is never acted on without the user confirming it in the preview. */
    suspend fun resolve(connector: ServiceConnector, refs: List<ExternalGameRef>): Map<Int, ExternalGameRef> {
        val known = knownGames(connector.service, refs)
        val matched = matcher.match(connector, refs.filter { it.uid !in known })
            .filter { it.confident }
            .mapNotNull { m -> m.game?.let { it.baseGameId to m.ref.copy(gameId = it.id.toInt()) } }
        return refs.mapNotNull { ref -> known[ref.uid]?.let { it.baseGameId to ref.copy(gameId = it.id.toInt()) } }
            .toMap() + matched
    }

    /**
     * Plan a library sync with the tracker. The Gamerlogue wishlist (backlog, not owned) belongs to the
     * wishlist sync, so it isn't pushed here — except games already synced with this tracker, which came from
     * its library and must keep following it.
     */
    suspend fun plan(
        connector: ServiceConnector,
        remote: Map<Int, ExternalGameRef>,
        snapshot: List<LibraryEntry>? = null,
    ): SyncPlan {
        val base = base(connector.service)
        val local = (snapshot ?: librarySync.allEntries())
            .filterNot { it.status == GameLibraryStatus.BACKLOG && !it.owned && it.gameId !in base }
            .associate { it.gameId to it.status }
        val plan = planSync(local, remote, base, connector.syncStatuses)
        // A tracker that can't add games only gets status updates for the ones it already has.
        return if (connector.canAddGames) plan else plan.copy(changes = plan.changes.filter { it.remote != null })
    }

    /** Apply the tracker side of [changes] to Gamerlogue and record them. Recorded only when every save
     *  succeeded: an unrecorded change is simply planned again next time. Returns how many were saved. */
    suspend fun pull(
        service: ExternalService,
        changes: List<SyncChange>,
        snapshot: MutableList<LibraryEntry>? = null,
        remote: Map<Int, ExternalGameRef> = emptyMap(),
    ): Int {
        val pulled = changes.mapNotNull { c -> c.remote?.let { r -> c.uid?.let { c.gameId to SyncedGame(it, r) } } }.toMap()
        val games = matcher.gamesByIds(pulled.keys.map { remote[it]?.gameId ?: it })
        val saved = librarySync.applyStatuses(games.map { it to pulled.getValue(it.baseGameId).status }, snapshot)
        if (saved == pulled.size) record(service, pulled)
        return saved
    }

    /**
     * The Gamerlogue side of [changes] as tracker writes, named from IGDB (trackers search new games by name).
     * A game's platforms are the user's own, or else the game's only platform — what the trackers' own add
     * forms default to; a multi-platform game the user set none for gets none, and isn't added there.
     */
    suspend fun outgoing(changes: List<SyncChange>, snapshot: List<LibraryEntry>? = null): List<StatusChange> {
        val pushed = changes.mapNotNull { c -> c.local?.let { c to it } }
        val games = matcher.gamesByIds(pushed.map { it.first.gameId }).associateBy { it.id.toInt() }
        // platformsIds throws on an entry that never had it set (see quickDraft): treat that as empty.
        val entries = (snapshot ?: librarySync.allEntries()).associateBy { it.gameId }
        return pushed.map { (c, to) ->
            val game = games[c.gameId]
            val entry = entries[c.gameId]
            val platforms = runCatching { entry?.platformsIds }.getOrNull().orEmpty()
                .ifEmpty { listOfNotNull(game?.platforms?.singleOrNull()?.id?.toInt()) }
            StatusChange(
                c.gameId, game?.name.orEmpty(), c.uid, c.remote, to, platforms,
                platformNames = game?.platformNames.orEmpty(), owned = entry?.owned == true,
            )
        }
    }

    /** Record what a [ServiceConnector.statusWrite] reported as written: each ref's uid + the status sent. */
    fun recordWritten(service: ExternalService, sent: List<StatusChange>, written: List<ExternalGameRef>) {
        val statusOf = sent.associate { it.gameId to it.to }
        record(
            service,
            written.mapNotNull { ref -> ref.gameId?.let { id -> statusOf[id]?.let { id to SyncedGame(ref.uid, it) } } }.toMap(),
        )
    }

    /** Tracker writes of a reviewed sync, from the preview screen to the WebView that runs them. */
    val pendingWrites = mutableMapOf<ExternalService, List<StatusChange>>()

    companion object {
        const val KEY = "librarySyncState"
    }
}
