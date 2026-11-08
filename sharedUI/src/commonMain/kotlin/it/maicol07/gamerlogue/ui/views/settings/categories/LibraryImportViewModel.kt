package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import co.touchlab.kermit.Logger
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.igdb.baseGameId
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.GameMatcher
import it.maicol07.gamerlogue.services.LibraryLoadException
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncChange
import it.maicol07.gamerlogue.services.SyncSide
import it.maicol07.gamerlogue.services.SyncedGame
import it.maicol07.gamerlogue.services.TrackerSync
import it.maicol07.gamerlogue.services.isMatchable
import it.maicol07.gamerlogue.services.onePerGame
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/** Which library the preview imports into: owned games, the wishlist (→ backlog), a tracker's games
 *  (→ the status the tracker gave each) or a two-way [SYNC] with a tracker. */
@Serializable
enum class ImportMode {
    OWNED,
    WISHLIST,
    TRACKED,

    /** Two steps: confirm the tracker → IGDB matches, then review the sync plan and settle conflicts. */
    SYNC,

    /** Confirm tracker → IGDB matches before updating only the tracker metadata. */
    ID_SYNC,
}

/**
 * Hands the games list read from a store's WebView to the import-preview screen, which is in a
 * different nav entry.
 */
object ImportHandoff {
    private val pending = mutableMapOf<ExternalService, List<ExternalGameRef>>()

    fun put(service: ExternalService, refs: List<ExternalGameRef>) {
        pending[service] = refs
    }

    fun take(service: ExternalService): List<ExternalGameRef> = pending.remove(service).orEmpty()
}

/**
 * Drives the editable mapping preview for a library import: matches each store game to an IGDB game,
 * lets the user toggle inclusion or swap the match via name search, then persists the chosen games.
 */
@KoinViewModel
class LibraryImportViewModel(
    @InjectedParam val service: ExternalService,
    @InjectedParam private val mode: ImportMode,
    connectors: Map<ExternalService, ServiceConnector>,
    private val matcher: GameMatcher,
    private val librarySync: LibrarySync,
    private val trackerSync: TrackerSync,
) : StateViewModel<LibraryImportViewModel.UiState>(UiState()) {

    private val connector: ServiceConnector = connectors.getValue(service)

    val canPushWishlist: Boolean get() = mode == ImportMode.WISHLIST && connector.wishlistWrite != null
    val hasNextStep: Boolean get() = mode == ImportMode.SYNC || mode == ImportMode.ID_SYNC || canPushWishlist

    /** IGDB game ids already in the user's library/backlog — those rows don't need importing. */
    private var existingIds: Set<Int> = emptySet()
    private var existingEditions: Map<Int, List<Int>> = emptyMap()

    /** The agreed games of the reviewed sync plan, recorded once the plan is applied (or when it has nothing to apply). */
    private var agreed: Map<Int, SyncedGame> = emptyMap()

    /** A fresh library each time: it is never cached, so a failed load can be retried and a write never runs on a stale one. */
    private suspend fun loadLibrary(): MutableList<LibraryEntry> = librarySync.allEntries().toMutableList()

    private fun onLibraryFailed(e: LibraryLoadException) {
        Logger.w(tag = "LibraryImport", throwable = e) { "library load failed" }
        update { copy(importing = false, libraryFailed = true) }
    }

    /**
     * A store game and its editable IGDB match.
     * [alreadyPresent] = the match is already in the library (no need to add; the mapping can still
     * be changed in case it's wrong).
     */
    data class Row(
        val ref: ExternalGameRef,
        val game: Game?,
        val included: Boolean,
        val alreadyPresent: Boolean = false,
        // True for an exact store-id match or a user-confirmed pick. Name-fallback guesses start false
        // and can't be selected until the user confirms/changes the match.
        val confident: Boolean = false,
        // The user picked this match, so it's remembered for the next matching of this game.
        val picked: Boolean = false,
        // The store page URL for this game (for the "open store" link), or null if unknown.
        val sourceUrl: String? = null,
        // The automatic match for this game hasn't come back yet.
        val searching: Boolean = false,
    )

    data class UiState(
        val loading: Boolean = true,
        val matching: Boolean = false,
        val processed: Int = 0,
        val total: Int = 0,
        val rows: List<Row> = emptyList(),
        val importing: Boolean = false,
        val importedCount: Int? = null,
        val editingIndex: Int? = null,
        /** The library could not be loaded; nothing was written, and confirming again retries the load. */
        val libraryFailed: Boolean = false,
        /** [ImportMode.SYNC] second step: the reviewed changes, once the matches are confirmed. */
        val plan: List<PlanRow>? = null,
        /** The reviewed wishlist or library sync has outgoing writes left, to run in the WebView. */
        val pushPending: Boolean = false,
    )

    /** One change of the sync plan, labelled with its IGDB [game]; [choice] is the side kept (conflicts
     *  start on Gamerlogue's) and [conflict] whether the user may switch it. Any other row can be left out
     *  ([included] = false) instead. */
    data class PlanRow(
        val change: SyncChange,
        val game: Game?,
        val choice: SyncSide,
        val conflict: Boolean,
        val included: Boolean = true,
    )

    init {
        val refs = ImportHandoff.take(service)
        update {
            copy(
                loading = false,
                matching = refs.isNotEmpty(),
                total = refs.size,
                rows = refs.map {
                    Row(it, game = null, included = false, sourceUrl = connector.storeUrl(it.uid), searching = true)
                },
            )
        }
        viewModelScope.launch {
            if (refs.isEmpty()) return@launch
            // The whole library is only needed to flag games already in it, so it loads alongside the matching
            // instead of holding it up: rows matched before it arrives get flagged (and deselected) once it does.
            // A sync touches games already in the library too, so none is "already present" there.
            val existingLoad = if (mode == ImportMode.ID_SYNC || mode == ImportMode.SYNC) {
                null
            } else {
                launch {
                    val entries = try {
                        loadLibrary()
                    } catch (e: LibraryLoadException) {
                        onLibraryFailed(e)
                        return@launch
                    }
                    existingIds = entries.filter { mode != ImportMode.OWNED || it.owned }.mapTo(mutableSetOf()) { it.gameId }
                    existingEditions = entries.associate { it.gameId to runCatching { it.editionsIds }.getOrDefault(emptyList()) }
                    update {
                        copy(
                            rows = rows.map { row ->
                                val present = row.game?.let(::isAlreadyPresent) == true
                                row.copy(alreadyPresent = present, included = row.included && !present)
                            },
                        )
                    }
                }
            }
            // Games synced before keep their confirmed match instead of being matched again.
            val known =
                if (mode == ImportMode.SYNC || mode == ImportMode.ID_SYNC) trackerSync.knownGames(service, refs) else emptyMap()
            update {
                copy(
                    processed = known.size,
                    rows = rows.map { row -> known[row.ref.uid]?.let { row.withMatch(it, confident = true) } ?: row },
                )
            }
            // Stream matches in as each batch resolves so the list fills progressively.
            matcher.match(connector, refs.filter { it.uid !in known }) { batch ->
                val byUid = batch.associateBy { it.ref.uid }
                update {
                    copy(
                        processed = processed + batch.size,
                        rows = rows.map { row ->
                            byUid[row.ref.uid]?.let { m -> row.withMatch(m.game, m.confident) } ?: row
                        },
                    )
                }
            }
            // Confirming stays disabled until the library is in too, so no already-present game is still selected.
            existingLoad?.join()
            update { copy(matching = false) }
        }
    }

    /**
     * Apply a match to a row: flag already-present games and pre-select only confident, new matches.
     * Name-fallback guesses ([confident] = false) stay unselected until the user confirms them.
     */
    private fun Row.withMatch(game: Game?, confident: Boolean): Row {
        val present = game != null && isAlreadyPresent(game)
        return copy(
            game = game,
            alreadyPresent = present,
            confident = confident,
            picked = false,
            included = game != null && !present && confident,
            searching = false,
        )
    }

    private fun isAlreadyPresent(game: Game): Boolean = game.baseGameId in existingIds &&
        (game.version_parent == null || game.id.toInt() in existingEditions[game.baseGameId].orEmpty())

    fun toggleIncluded(index: Int) = update {
        copy(
            rows = rows.mapIndexed { i, row ->
                // Only confident, not-already-present rows are selectable.
                if (i == index && row.confident && !row.alreadyPresent) row.copy(included = !row.included) else row
            },
        )
    }

    /** Select or deselect every selectable row (confident match, not already in the library). */
    fun setAllIncluded(included: Boolean) = update {
        copy(rows = rows.map { it.copy(included = included && it.confident && !it.alreadyPresent) })
    }

    fun startEdit(index: Int) = update { copy(editingIndex = index) }

    fun chooseMatchId(id: Int) {
        val index = state.editingIndex ?: return
        viewModelScope.launch {
            // Same rule as the automatic matching: an edition is accepted through its base game.
            val game = matcher.gamesByIds(listOf(id)).firstOrNull { it.id.toInt() == id && it.isMatchable }
            if (game != null) chooseMatch(index, game) else update { copy(editingIndex = editingIndex.takeUnless { it == index }) }
        }
    }

    fun chooseMatch(game: Game) {
        val index = state.editingIndex ?: return
        chooseMatch(index, game)
    }

    private fun chooseMatch(index: Int, game: Game) = update {
        // A manual pick is a confirmation → confident.
        copy(
            rows = rows.mapIndexed { i, row ->
                if (i == index) row.withMatch(game, confident = true).copy(picked = true) else row
            },
            editingIndex = editingIndex.takeUnless { it == index },
        )
    }

    /** Persist the selected games — or, for [ImportMode.SYNC], plan the sync over the confirmed matches. */
    fun confirm() = viewModelScope.launch {
        try {
            doConfirm()
        } catch (e: LibraryLoadException) {
            onLibraryFailed(e)
        }
    }

    private suspend fun doConfirm() {
        update { copy(importing = true) }
        // Every pick is remembered, selected or not: it says what the game is, not whether to import it.
        matcher.memory.remember(service, picks)
        val included = state.rows.filter { it.included }
        if (mode == ImportMode.ID_SYNC) {
            ImportHandoff.put(service, state.rows.map { row ->
                row.ref.copy(gameId = row.game?.takeIf { row.included && row.confident }?.id?.toInt())
            })
            update { copy(importing = false, importedCount = 0, pushPending = true) }
            return
        }
        if (mode == ImportMode.SYNC) {
            // Games on the tracker under any row, confirmed or not, are never added to it; only the confirmed,
            // selected rows say what their status there is.
            val onTracker = state.rows.mapNotNull { it.game?.baseGameId }.toSet()
            val plan = trackerSync.plan(connector, confirmedRemote(), loadLibrary(), onTracker)
            // Agreed games are recorded (with their confirmed matches) once the plan is applied: recording them now
            // would make this look like a synced library, and the next unattended run would apply unreviewed changes.
            // With nothing to apply there is nothing to approve, so they are recorded right away.
            if (plan.changes.isEmpty()) trackerSync.record(service, plan.agreed) else agreed = plan.agreed
            val games = matcher.gamesByIds(plan.changes.map { it.gameId }).associateBy { it.id.toInt() }
            val rows = plan.changes.map { c ->
                PlanRow(c, games[c.gameId], choice = c.winner ?: SyncSide.LOCAL, conflict = c.winner == null)
            }
            update { copy(importing = false, libraryFailed = false, plan = rows) }
            return
        }
        val games = included.mapNotNull { it.game }
        // Read now rather than at preview time, so what changed in the meantime isn't overwritten.
        val entries = loadLibrary()
        val count = if (games.isEmpty()) 0 else when (mode) {
            ImportMode.OWNED -> librarySync.importOwned(connector, games, entries)
            ImportMode.WISHLIST -> librarySync.importWishlist(connector, games, entries)
            // Only games not in the library yet are selectable, so this just creates them.
            ImportMode.TRACKED -> librarySync.applyStatuses(
                included.mapNotNull { row -> row.game?.let { it to (row.ref.status ?: GameLibraryStatus.BACKLOG) } },
                entries,
            )

            ImportMode.SYNC, ImportMode.ID_SYNC -> error("sync confirmation is handled above")
        }
        if (canPushWishlist) {
            // Every matched row is on the wishlist, whether or not its match was confirmed: same as the automatic path.
            val wishlistIds = state.rows.mapNotNull { it.game?.baseGameId }.toSet()
            val outgoing = librarySync.computeWishlistPush(connector, emptyList(), entries, wishlistIds)
            librarySync.pendingWishlistImports[service] = LibrarySync.WishlistResult(count, outgoing)
        }
        update { copy(importing = false, libraryFailed = false, importedCount = count, pushPending = canPushWishlist) }
    }

    /** The IGDB id the user picked, by store uid. */
    private val picks: Map<String, Int>
        get() = state.rows.filter { it.picked }
            .mapNotNull { row -> row.game?.let { row.ref.uid to it.id.toInt() } }
            .toMap()

    /** Tracker refs by IGDB game from the confirmed, selected rows, each carrying its matched IGDB id. */
    private fun confirmedRemote(): Map<Int, ExternalGameRef> = state.rows.filter { it.included }.mapNotNull { row ->
        row.game?.let { it.baseGameId to row.ref.copy(gameId = it.id.toInt()) }
    }.onePerGame()

    fun choose(index: Int, side: SyncSide) = update {
        copy(plan = plan?.mapIndexed { i, row -> if (i == index && row.conflict) row.copy(choice = side) else row })
    }

    /** Leave a settled (non-conflict) row out of the sync, or put it back. */
    fun togglePlanRow(index: Int) = update {
        copy(plan = plan?.mapIndexed { i, row -> if (i == index && !row.conflict) row.copy(included = !row.included) else row })
    }

    /** Apply the reviewed plan: tracker → Gamerlogue now, Gamerlogue → tracker handed to the WebView. */
    fun applySync() = viewModelScope.launch {
        try {
            doApplySync()
        } catch (e: LibraryLoadException) {
            onLibraryFailed(e)
        }
    }

    private suspend fun doApplySync() {
        val rows = state.plan?.filter { it.included } ?: return
        update { copy(importing = true) }
        // Read now: the plan may have been open for a while.
        val entries = loadLibrary()
        val pulled = trackerSync.pull(service, rows.filter { it.choice == SyncSide.REMOTE }.map { it.change }, entries, confirmedRemote())
        trackerSync.record(service, agreed)
        val outgoing = trackerSync.outgoing(rows.filter { it.choice == SyncSide.LOCAL }.map { it.change }, entries)
        trackerSync.pendingWrites[service] = outgoing
        update { copy(importing = false, libraryFailed = false, importedCount = pulled, pushPending = outgoing.isNotEmpty()) }
    }
}
