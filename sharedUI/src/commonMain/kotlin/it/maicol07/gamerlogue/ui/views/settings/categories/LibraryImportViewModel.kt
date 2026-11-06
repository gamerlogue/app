package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.extensions.igdb.baseGameId
import it.maicol07.gamerlogue.extensions.igdb.isBaseGame
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.GameMatcher
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.SyncChange
import it.maicol07.gamerlogue.services.SyncSide
import it.maicol07.gamerlogue.services.TrackerSync
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
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
    private val librarySnapshot = viewModelScope.async(start = CoroutineStart.LAZY) { librarySync.allEntries().toMutableList() }

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
        // The store page URL for this game (for the "open store" link), or null if unknown.
        val sourceUrl: String? = null,
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
        /** [ImportMode.SYNC] second step: the reviewed changes, once the matches are confirmed. */
        val plan: List<PlanRow>? = null,
        /** The reviewed wishlist or library sync has outgoing writes left, to run in the WebView. */
        val pushPending: Boolean = false,
    )

    /** One change of the sync plan, labelled with its IGDB [game]; [choice] is the side kept (conflicts
     *  start on Gamerlogue's) and [conflict] whether the user may switch it. */
    data class PlanRow(val change: SyncChange, val game: Game?, val choice: SyncSide, val conflict: Boolean)


    init {
        val refs = ImportHandoff.take(service)
        update {
            copy(
                loading = false,
                matching = refs.isNotEmpty(),
                total = refs.size,
                rows = refs.map {
                    Row(it, game = null, included = false, sourceUrl = connector.storeUrl(it.uid))
                },
            )
        }
        viewModelScope.launch {
            if (refs.isEmpty()) return@launch
            // The whole library is only needed to flag games already in it, so it loads alongside the matching
            // instead of holding it up: rows matched before it arrives get flagged (and deselected) once it does.
            // A sync touches games already in the library too, so none is "already present" there.
            val existingLoad = if (mode == ImportMode.ID_SYNC) {
                null
            } else {
                launch {
                    val entries = librarySnapshot.await()
                    if (mode == ImportMode.SYNC) return@launch
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
            included = game != null && !present && confident,
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
            matcher.gamesByIds(listOf(id)).firstOrNull { it.id.toInt() == id && it.isBaseGame }?.let { chooseMatch(index, it) }
        }
    }

    fun chooseMatch(game: Game) {
        val index = state.editingIndex ?: return
        chooseMatch(index, game)
    }

    private fun chooseMatch(index: Int, game: Game) = update {
        // A manual pick is a confirmation → confident.
        copy(
            rows = rows.mapIndexed { i, row -> if (i == index) row.withMatch(game, confident = true) else row },
            editingIndex = editingIndex.takeUnless { it == index },
        )
    }

    /** Persist the selected games — or, for [ImportMode.SYNC], plan the sync over the confirmed matches. */
    fun confirm() = viewModelScope.launch {
        update { copy(importing = true) }
        val included = state.rows.filter { it.included }
        if (mode == ImportMode.ID_SYNC) {
            ImportHandoff.put(service, state.rows.map { row ->
                row.ref.copy(gameId = row.game?.takeIf { row.included && row.confident }?.id?.toInt())
            })
            update { copy(importing = false, importedCount = 0, pushPending = true) }
            return@launch
        }
        if (mode == ImportMode.SYNC) {
            val remote = included.mapNotNull { row -> row.game?.let { it.baseGameId to row.ref } }.toMap()
            val plan = trackerSync.plan(connector, remote, librarySnapshot.await())
            // Games equal on both sides are a fact now, not a choice: record them (and their confirmed matches)
            // straight away, so they're remembered even if the plan is closed without applying.
            trackerSync.record(service, plan.agreed)
            val games = matcher.gamesByIds(plan.changes.map { it.gameId }).associateBy { it.id.toInt() }
            val rows = plan.changes.map { c ->
                PlanRow(c, games[c.gameId], choice = c.winner ?: SyncSide.LOCAL, conflict = c.winner == null)
            }
            update { copy(importing = false, plan = rows) }
            return@launch
        }
        val games = included.mapNotNull { it.game }
        val entries = librarySnapshot.await()
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
            val wishlistIds = state.rows.filter { it.confident }.mapNotNull { it.game?.baseGameId }.toSet()
            val outgoing = librarySync.computeWishlistPush(connector, emptyList(), entries, wishlistIds)
            librarySync.pendingWishlistImports[service] = LibrarySync.WishlistResult(count, outgoing)
        }
        update { copy(importing = false, importedCount = count, pushPending = canPushWishlist) }
    }

    fun choose(index: Int, side: SyncSide) = update {
        copy(plan = plan?.mapIndexed { i, row -> if (i == index && row.conflict) row.copy(choice = side) else row })
    }

    /** Apply the reviewed plan: tracker → Gamerlogue now, Gamerlogue → tracker handed to the WebView. */
    fun applySync() = viewModelScope.launch {
        val rows = state.plan ?: return@launch
        update { copy(importing = true) }
        val entries = librarySnapshot.await()
        val remote = state.rows.filter { it.included }.mapNotNull { row ->
            row.game?.let { it.baseGameId to row.ref.copy(gameId = it.id.toInt()) }
        }.toMap()
        val pulled = trackerSync.pull(service, rows.filter { it.choice == SyncSide.REMOTE }.map { it.change }, entries, remote)
        val outgoing = trackerSync.outgoing(rows.filter { it.choice == SyncSide.LOCAL }.map { it.change }, entries)
        trackerSync.pendingWrites[service] = outgoing
        update { copy(importing = false, importedCount = pulled, pushPending = outgoing.isNotEmpty()) }
    }
}
