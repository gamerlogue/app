package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.services.ExternalGameRef
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.GameMatcher
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ServiceConnector
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Which library the preview imports into: owned games or the wishlist (→ backlog). */
@Serializable
enum class ImportMode { OWNED, WISHLIST }

/**
 * Hands the games list read from a store's WebView to the import-preview screen, which is in a
 * different nav entry. ponytail: plain read-once map, like [it.maicol07.gamerlogue.ui.views.game.GameHandoff].
 */
object ImportHandoff {
    private val pending = mutableMapOf<ExternalService, List<ExternalGameRef>>()
    fun put(service: ExternalService, refs: List<ExternalGameRef>) { pending[service] = refs }
    fun take(service: ExternalService): List<ExternalGameRef> = pending.remove(service).orEmpty()
}

/**
 * Drives the editable mapping preview for a library import: matches each store game to an IGDB game,
 * lets the user toggle inclusion or swap the match via name search, then persists the chosen games.
 */
class LibraryImportViewModel(
    val service: ExternalService,
    private val mode: ImportMode,
    private val connectors: Map<ExternalService, ServiceConnector>,
    private val matcher: GameMatcher,
    private val librarySync: LibrarySync,
) : StateViewModel<LibraryImportViewModel.UiState>(UiState()) {

    private val connector: ServiceConnector = connectors.getValue(service)

    /** IGDB game ids already in the user's library/backlog — those rows don't need importing. */
    private var existingIds: Set<Int> = emptySet()

    /**
     * A store game and its (editable) chosen IGDB match, with the other [candidates] to pick from.
     * [alreadyPresent] = the match is already in the library (no need to add; the mapping can still
     * be changed in case it's wrong).
     */
    data class Row(
        val ref: ExternalGameRef,
        val game: Game?,
        val candidates: List<Game>,
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
        val searching: Boolean = false,
        val searchResults: List<Game> = emptyList(),
    )

    init {
        val refs = ImportHandoff.take(service)
        update {
            copy(
                loading = false,
                matching = refs.isNotEmpty(),
                total = refs.size,
                rows = refs.map {
                    Row(it, game = null, candidates = emptyList(), included = false, sourceUrl = connector.storeUrl(it.uid))
                },
            )
        }
        viewModelScope.launch {
            existingIds = librarySync.existingGameIds(ownedOnly = mode == ImportMode.OWNED)
            // Stream matches in as each batch resolves so the list fills progressively.
            matcher.match(connector, refs) { batch ->
                val byUid = batch.associateBy { it.ref.uid }
                update {
                    copy(
                        processed = processed + batch.size,
                        rows = rows.map { row ->
                            byUid[row.ref.uid]?.let { m -> row.withMatch(m.game, m.candidates, m.confident) } ?: row
                        },
                    )
                }
            }
            update { copy(matching = false) }
        }
    }

    /**
     * Apply a match to a row: flag already-present games and pre-select only confident, new matches.
     * Name-fallback guesses ([confident] = false) stay unselected until the user confirms them.
     */
    private fun Row.withMatch(game: Game?, candidates: List<Game>, confident: Boolean): Row {
        val present = game != null && game.id.toInt() in existingIds
        return copy(
            game = game,
            candidates = candidates,
            alreadyPresent = present,
            confident = confident,
            included = game != null && !present && confident,
        )
    }

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

    // Prefill the search dialog with the auto-found candidates so a pick needs no extra typing.
    fun startEdit(index: Int) = update {
        copy(editingIndex = index, searchResults = rows.getOrNull(index)?.candidates.orEmpty())
    }
    fun cancelEdit() = update { copy(editingIndex = null, searchResults = emptyList()) }

    fun search(query: String) = viewModelScope.launch {
        update { copy(searching = true) }
        val results = matcher.searchByName(query)
        update { copy(searching = false, searchResults = results) }
    }

    fun chooseMatch(game: Game) = update {
        val index = editingIndex ?: return@update this
        // A manual pick is a confirmation → confident.
        copy(
            rows = rows.mapIndexed { i, row -> if (i == index) row.withMatch(game, row.candidates, confident = true) else row },
            editingIndex = null,
            searchResults = emptyList(),
        )
    }

    fun confirm() = viewModelScope.launch {
        update { copy(importing = true) }
        val games = state.rows.filter { it.included }.mapNotNull { it.game }
        val count = when (mode) {
            ImportMode.OWNED -> librarySync.importOwned(games)
            ImportMode.WISHLIST -> librarySync.importWishlist(games)
        }
        update { copy(importing = false, importedCount = count) }
    }
}
