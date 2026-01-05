package it.maicol07.gamerlogue.ui.views.library.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Platform
import it.maicol07.gamerlogue.auth.AuthState
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.gamerlogue.ui.views.library.LibraryViewModel
import it.maicol07.spraypaintkt.JsonApiException
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AddToLibrarySheetViewModel(
    private val game: Game,
    private val existingEntry: LibraryEntry?
) : ViewModel(), KoinComponent {
    val libraryViewModel by inject<LibraryViewModel>()

    // States
    var selectedStatus by mutableStateOf(existingEntry?.status)
    var completionStatus by mutableStateOf(existingEntry?.completionStatus)
    var owned by mutableStateOf(existingEntry?.owned ?: false)
    var selectedEdition by mutableStateOf(existingEntry?.editionId)
    val selectedPlatforms = mutableStateListOf<Platform>().apply {
        addAll(
            existingEntry?.platformsIds?.mapNotNull { platformId ->
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
    var error by mutableStateOf<String?>(null)

    fun togglePlatformSelection(platform: Platform) {
        if (selectedPlatforms.contains(platform)) {
            selectedPlatforms.remove(platform)
        } else {
            selectedPlatforms.add(platform)
        }
    }

    fun deleteEntry(onDelete: () -> Unit) = viewModelScope.launch {
        deleteLoading = true
        error = null
        if (existingEntry != null) {
            libraryViewModel.removeLibraryEntry(existingEntry)
            onDelete()
        }
        deleteLoading = false
    }

    fun saveEntry(onSave: (LibraryEntry) -> Unit) = viewModelScope.launch {
        saveLoading = true
        error = null

        if (selectedStatus == null) {
            error = "Please select a status."
            saveLoading = false
            return@launch
        }

        val entry = getEntryToSave()
        libraryViewModel.updateLibraryEntry(entry)
        onSave(entry)

        saveLoading = false
    }

    private fun getEntryToSave(): LibraryEntry {
        val isBacklog = selectedStatus == GameLibraryStatus.BACKLOG
        val isPlayingOrPaused = selectedStatus == GameLibraryStatus.PLAYING || selectedStatus == GameLibraryStatus.PAUSED

        val entry = existingEntry ?: LibraryEntry()

        entry.gameId = game.id.toInt()
        entry.user = AuthState.currentUser
        entry.status = selectedStatus!!
        entry.completionStatus = completionStatus
        entry.owned = owned
        entry.editionId = selectedEdition
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
}
