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
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.BaseViewModel
import it.maicol07.gamerlogue.data.LibraryEntry

import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import org.jetbrains.compose.resources.StringResource
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

@KoinViewModel
class AddToLibrarySheetViewModel(
    @InjectedParam private val game: Game,
    @InjectedParam private val existingEntry: LibraryEntry?
) : BaseViewModel() {
    private val authTokenProvider by inject<AuthTokenProvider>()
    private val igdb by inject<IgdbClient>()

    // States
    var selectedStatus by mutableStateOf(existingEntry?.status)
    var completionStatus by mutableStateOf(existingEntry?.completionStatus)
    var owned by mutableStateOf(existingEntry?.owned ?: false)
    // editionsIds/platformsIds throw NoSuchElementException on a never-set attribute (older entries
    // predating this feature) rather than defaulting to an empty list, so guard the read. A brand-new
    // entry defaults to the standard edition (the base game itself) preselected.
    val selectedEditions = mutableStateListOf<Int>().apply {
        val existingIds = runCatching { existingEntry?.editionsIds }.getOrNull() ?: emptyList()
        addAll(if (existingEntry == null) listOf(game.id.toInt()) else existingIds)
    }
    var editions by mutableStateOf<List<Game>>(emptyList())
    var editionsLoading by mutableStateOf(true)
    val selectedPlatforms = mutableStateListOf<Platform>().apply {
        addAll(
            runCatching { existingEntry?.platformsIds }.getOrNull()?.mapNotNull { platformId ->
                game.platforms.find { it.id.toInt() == platformId }
            } ?: emptyList()
        )
    }
    var startDate by mutableStateOf(existingEntry?.startDateAsInstant)
    var endDate by mutableStateOf(existingEntry?.endDateAsInstant)
    var playedTime = TextFieldState(existingEntry?.playedTime?.toString() ?: "")
    var rating by mutableStateOf(existingEntry?.rating)
    var review = TextFieldState(existingEntry?.review ?: "")

    var saveLoading by mutableStateOf(false)
    var deleteLoading by mutableStateOf(false)
    var error by mutableStateOf<StringResource?>(null)

    init {
        viewModelScope.launch {
            val result = safeRequest {
                igdb.getGames {
                    where("version_parent = ${game.id}")
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
            safeRequest { existingEntry.destroy() }
            onDelete()
        }
        deleteLoading = false
    }

    fun saveEntry(onSave: (LibraryEntry) -> Unit) = viewModelScope.launch {
        saveLoading = true
        error = null

        if (selectedStatus == null) {
            error = Res.string.library__error_select_status
            saveLoading = false
            return@launch
        }

        val entry = getEntryToSave()
        safeRequest { entry.save() }
        onSave(entry)

        saveLoading = false
    }

    private fun getEntryToSave(): LibraryEntry {
        val isBacklog = selectedStatus == GameLibraryStatus.BACKLOG
        val isPlayingOrPaused = selectedStatus == GameLibraryStatus.PLAYING || selectedStatus == GameLibraryStatus.PAUSED

        val entry = existingEntry ?: LibraryEntry()

        entry.gameId = game.id.toInt()
        entry.user = authTokenProvider.currentUser
        entry.status = selectedStatus!!
        entry.completionStatus = completionStatus
        entry.owned = owned
        entry.editionsIds = selectedEditions.toList()
        entry.platformsIds = selectedPlatforms.map { it.id.toInt() }
        entry.startDate = if (isBacklog) null else startDate?.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
        entry.endDate = if (isBacklog || isPlayingOrPaused) {
            null
        } else {
            endDate?.format(DateTimeComponents.Formats.ISO_DATE_TIME_OFFSET)
        }
        entry.playedTime = if (isBacklog) null else playedTime.text.toString().ifBlank { null }?.toInt()
        entry.rating = if (isBacklog || isPlayingOrPaused) null else rating
        entry.review = if (isBacklog || isPlayingOrPaused) "" else review.text.toString()

        return entry
    }

    private companion object {
        const val EDITIONS_LIMIT = 50
    }
}
