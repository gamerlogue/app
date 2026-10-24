package it.maicol07.gamerlogue.ui.views.library.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.getGames
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Platform
import com.github.michaelbull.result.get
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__error_select_status
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.core.BaseViewModel
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.LibraryEntrySchema.CompletionStatus
import it.maicol07.gamerlogue.extensions.igdb.baseGameId
import it.maicol07.gamerlogue.extensions.igdb.isReleased
import it.maicol07.gamerlogue.ui.components.ReviewMode
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import kotlin.math.roundToInt

@KoinViewModel
class AddToLibrarySheetViewModel(
    @InjectedParam private val game: Game,
    @InjectedParam private val existingEntry: LibraryEntry?,
    /** The user's own platforms: listed first, and preselected on a new entry when only one fits. */
    @InjectedParam ownedPlatformIds: List<Int>,
) : BaseViewModel() {
    private val igdb by inject<IgdbClient>()
    private val libraryChanges by inject<LibraryChanges>()
    private val preferences by inject<AppPreferences>()

    /** An unreleased game can only be backlogged, and not owned yet. */
    val released = game.isReleased()

    // States
    var selectedStatus by mutableStateOf(existingEntry?.status ?: GameLibraryStatus.BACKLOG.takeUnless { released })
    var completionStatus by mutableStateOf(existingEntry?.completionStatus)
    var owned by mutableStateOf(existingEntry?.owned ?: false)
    // editionsIds/platformsIds throw NoSuchElementException on a never-set attribute (older entries
    // predating this feature) rather than defaulting to an empty list, so guard the read. A brand-new
    // entry defaults to the game the sheet was opened from preselected: the standard edition (the base
    // game itself), or the edition whose page it is, since the entry is the base game's either way.
    val selectedEditions = mutableStateListOf<Int>().apply {
        val existingIds = runCatching { existingEntry?.editionsIds }.getOrNull() ?: emptyList()
        addAll(if (existingEntry == null) listOf(game.id.toInt()) else existingIds)
    }
    var editions by mutableStateOf<List<Game>>(emptyList())
    var editionsLoading by mutableStateOf(true)

    /** The game's platforms, the owned ones first. */
    val platforms: List<Platform> = game.platforms.sortedByDescending { it.id.toInt() in ownedPlatformIds }
    val ownedPlatforms: Set<Long> = game.platforms.filter { it.id.toInt() in ownedPlatformIds }.map { it.id }.toSet()
    val selectedPlatforms = mutableStateListOf<Platform>().apply {
        if (existingEntry == null) {
            // The only owned platform the game runs on is the one it is being added for.
            game.platforms.singleOrNull { it.id in ownedPlatforms }?.let(::add)
        } else {
            addAll(
                runCatching { existingEntry.platformsIds }.getOrNull()?.mapNotNull { platformId ->
                    game.platforms.find { it.id.toInt() == platformId }
                } ?: emptyList()
            )
        }
    }
    var startDate by mutableStateOf(existingEntry?.startLocalDate)
    var endDate by mutableStateOf(existingEntry?.endLocalDate)
    var playedTime = TextFieldState(existingEntry?.playedTime?.toString() ?: "")
    var rating by mutableStateOf(existingEntry?.rating?.toFloat())

    /**
     * Read once: switching the setting does not reshape a sheet already open. A detailed review rates
     * itself through its sections, in the review editor, so the sheet then leaves the rating alone.
     */
    val detailedReview = preferences.reviewMode.value == ReviewMode.DETAILED

    /** Declared after every field it reads, so it captures them as the sheet opened. */
    private val initialForm = form()

    /** A new entry always has to be saved before its review can be written. */
    val hasChanges: Boolean
        get() = existingEntry == null || form() != initialForm

    /** Only a finished game is rated and reviewed: saving with any other status clears both. */
    val clearsReview: Boolean
        get() = selectedStatus in UNRATED_STATUSES && existingEntry != null &&
            (existingEntry.hasReview || existingEntry.rating != null)

    var saveLoading by mutableStateOf(false)
    var deleteLoading by mutableStateOf(false)
    var error by mutableStateOf<StringResource?>(null)

    init {
        viewModelScope.launch {
            val result = safeRequest {
                igdb.getGames {
                    where("version_parent = ${game.baseGameId}")
                    fields("id", "name", "version_title", "cover.image_id")
                    limit(EDITIONS_LIMIT)
                }
            }
            editions = result.get()?.games.orEmpty()
            editionsLoading = false
        }
    }

    fun togglePlatformSelection(platform: Platform) {
        if (selectedPlatforms.contains(platform)) {
            selectedPlatforms.remove(platform)
        } else {
            selectedPlatforms.add(platform)
        }
    }

    fun toggleEditionSelection(editionId: Int) {
        if (selectedEditions.contains(editionId)) {
            selectedEditions.remove(editionId)
        } else {
            selectedEditions.add(editionId)
        }
    }

    fun deleteEntry(onDelete: () -> Unit) = viewModelScope.launch {
        deleteLoading = true
        error = null
        if (existingEntry != null) {
            if (safeRequest { existingEntry.destroy() }.isOk) libraryChanges.notifyChanged()
            onDelete()
        }
        deleteLoading = false
    }

    /** Hands the entry to [onSave] even when the request failed: [safeRequest] already reported it. */
    fun saveEntry(onSave: (LibraryEntry) -> Unit) = viewModelScope.launch {
        save()?.let { (entry, _) -> onSave(entry) }
    }

    /** Saves first, since the review editor only works on an existing entry; [onSaved] gets its id. */
    fun saveForReview(onSaved: (String) -> Unit) = viewModelScope.launch {
        save()?.let { (entry, saved) -> if (saved) entry.id?.let(onSaved) }
    }

    /** Null when no status is picked; otherwise the entry and whether the server accepted it. */
    private suspend fun save(): Pair<LibraryEntry, Boolean>? {
        error = null
        if (selectedStatus == null) {
            error = Res.string.library__error_select_status
            return null
        }
        saveLoading = true
        val entry = getEntryToSave()
        val saved = safeRequest { entry.save() }.isOk
        if (saved) libraryChanges.notifyChanged()
        saveLoading = false
        return entry to saved
    }

    private fun getEntryToSave(): LibraryEntry {
        val isBacklog = selectedStatus == GameLibraryStatus.BACKLOG
        val isPlayingOrPaused = selectedStatus == GameLibraryStatus.PLAYING || selectedStatus == GameLibraryStatus.PAUSED

        val entry = existingEntry ?: LibraryEntry()

        entry.gameId = game.baseGameId
        entry.status = selectedStatus!!
        entry.completionStatus = completionStatus
        entry.owned = owned
        entry.editionsIds = selectedEditions.toList()
        entry.platformsIds = selectedPlatforms.map { it.id.toInt() }
        // Date columns: sent as a plain `yyyy-MM-dd`, the calendar day the user picked.
        entry.startDate = if (isBacklog) null else startDate?.toString()
        entry.endDate = if (isBacklog || isPlayingOrPaused) null else endDate?.toString()
        entry.playedTime = if (isBacklog) null else playedTime.text.toString().ifBlank { null }?.toInt()
        if (selectedStatus in UNRATED_STATUSES) {
            entry.rating = null
            entry.ratingDetails = null
            entry.review = ""
            entry.reviewSpoiler = false
        } else if (!detailedReview) {
            entry.rating = rating?.roundToInt()
        }

        return entry
    }

    private fun form() = FormSnapshot(
        status = selectedStatus,
        completionStatus = completionStatus,
        owned = owned,
        editions = selectedEditions.toSet(),
        platforms = selectedPlatforms.map { it.id }.toSet(),
        startDate = startDate,
        endDate = endDate,
        playedTime = playedTime.text.toString(),
        rating = rating
    )

    /** What the form holds, to tell whether anything changed since the sheet opened. */
    private data class FormSnapshot(
        val status: GameLibraryStatus?,
        val completionStatus: CompletionStatus?,
        val owned: Boolean,
        val editions: Set<Int>,
        val platforms: Set<Long>,
        val startDate: LocalDate?,
        val endDate: LocalDate?,
        val playedTime: String,
        val rating: Float?,
    )

    private companion object {
        const val EDITIONS_LIMIT = 50
        val UNRATED_STATUSES = setOf(GameLibraryStatus.BACKLOG, GameLibraryStatus.PLAYING, GameLibraryStatus.PAUSED)
    }
}
