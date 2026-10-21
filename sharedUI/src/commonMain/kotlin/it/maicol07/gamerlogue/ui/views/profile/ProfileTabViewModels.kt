package it.maicol07.gamerlogue.ui.views.profile

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Platform
import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.core.BaseViewModel
import it.maicol07.gamerlogue.core.PageLoader
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.ApiErrorCodes
import it.maicol07.gamerlogue.data.IgdbLookup
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.data.UserList
import it.maicol07.gamerlogue.data.UserStats
import it.maicol07.gamerlogue.extensions.fetchPage
import it.maicol07.gamerlogue.extensions.userReviews
import it.maicol07.gamerlogue.ui.views.userlist.UserListForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/** The profile's reviews tab: the user's reviewed entries, newest review first, with their games. */
@KoinViewModel
class ProfileReviewsViewModel(@InjectedParam private val userId: String) : BaseViewModel() {
    private val lookup by inject<IgdbLookup>()

    val games: StateFlow<Map<Int, Game>>
        field = MutableStateFlow(emptyMap())

    val reviews = PageLoader(viewModelScope) { page ->
        safeRequest(ApiErrorCodes.HIDDEN_CONTENT) {
            LibraryEntry.userReviews(userId).fetchPage(page).also { result ->
                val found = lookup.games(result.items.map { it.gameId })
                games.update { it + found }
            }
        }.get()
    }
}

/** The profile's lists tab. */
@KoinViewModel
class ProfileListsViewModel(@InjectedParam private val userId: String) : BaseViewModel() {
    val lists = PageLoader(viewModelScope) { page ->
        safeRequest(ApiErrorCodes.HIDDEN_CONTENT) { UserList.where("user", userId).fetchPage(page) }.get()
    }

    /** Creates a list from [form] and hands its id to [onCreated]. */
    fun create(form: UserListForm, onCreated: (String) -> Unit) = viewModelScope.launch {
        val list = UserList().apply { form.applyTo(this) }
        if (safeRequest { list.save() }.isOk) {
            lists.refresh()
            list.id?.let(onCreated)
        }
    }
}

/** The profile's statistics tab, for all time or one year. */
@KoinViewModel
class ProfileStatsViewModel(@InjectedParam private val userId: String) : StateViewModel<ProfileStatsViewModel.UiState>(
    UiState()
) {
    data class UiState(
        /** Null for all time. */
        val year: Int? = null,
        val stats: UserStats? = null,
        /** The years with any activity, from the all-time stats, newest first. */
        val years: List<Int> = emptyList(),
        val platforms: Map<Int, Platform> = emptyMap(),
        val loading: Boolean = false,
        val failed: Boolean = false,
    )

    private val api by inject<ProfileApi>()
    private val lookup by inject<IgdbLookup>()

    /** Loads the current year's stats unless they are already there. */
    fun ensureLoaded() {
        if (state.stats == null && !state.loading) selectYear(state.year)
    }

    /** Reloads the shown year's stats, if they were loaded at all. */
    fun reload() {
        if (state.stats != null) selectYear(state.year)
    }

    fun selectYear(year: Int?) = viewModelScope.launch {
        update { copy(year = year, loading = true, failed = false) }
        val stats = safeRequest(ApiErrorCodes.HIDDEN_CONTENT) { api.stats(userId, year) }.get()
        val platforms = stats?.platforms?.takeIf { it.isNotEmpty() }
            ?.let { safeRequest { lookup.platforms() }.get() }
            ?.associateBy { it.id.toInt() }
            .orEmpty()
        update {
            copy(
                stats = stats,
                loading = false,
                failed = stats == null,
                platforms = platforms,
                years = if (year == null && stats != null) stats.activeYears() else years,
            )
        }
    }

    private fun UserStats.activeYears(): List<Int> =
        (completedByYear.keys + startedByYear.keys).mapNotNull(String::toIntOrNull).distinct().sortedDescending()
}
