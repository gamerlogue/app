package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.core.PagedState
import it.maicol07.gamerlogue.data.Activity
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.UserList
import it.maicol07.gamerlogue.ui.views.social.ActivityViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The view models behind a profile: the profile itself and one per tab, each loading only when its tab shows. */
internal class ProfileViewModels(
    val profile: ProfileViewModel,
    val reviews: ProfileReviewsViewModel,
    val lists: ProfileListsViewModel,
    val stats: ProfileStatsViewModel,
    val activity: ActivityViewModel,
) {
    fun ensureLoaded(tab: ProfileTab) = when (tab) {
        // Loaded with the profile.
        ProfileTab.LIBRARY -> Unit
        ProfileTab.ACTIVITY -> activity.activities.ensureLoaded()
        ProfileTab.REVIEWS -> reviews.reviews.ensureLoaded()
        ProfileTab.LISTS -> lists.lists.ensureLoaded()
        ProfileTab.STATS -> stats.ensureLoaded()
    }

    fun loadMore(tab: ProfileTab) = when (tab) {
        ProfileTab.ACTIVITY -> activity.activities.loadMore()
        ProfileTab.REVIEWS -> reviews.reviews.loadMore()
        ProfileTab.LISTS -> lists.lists.loadMore()
        ProfileTab.LIBRARY, ProfileTab.STATS -> Unit
    }

    /** Reloads the profile and every tab already loaded, e.g. when the screen shows again after a new review. */
    fun reload() {
        profile.load()
        reloadTabs()
    }

    /** [reload] for a pull to refresh, with its indicator. */
    fun refresh() {
        profile.refresh()
        reloadTabs()
    }

    // The tabs not loaded yet load fresh when they show.
    private fun reloadTabs() {
        listOf(activity.activities, reviews.reviews, lists.lists).filter { it.isStarted }.forEach { it.refresh() }
        stats.reload()
    }
}

/** What the tabs show, collected from their view models. */
internal data class ProfileTabsData(
    val reviews: PagedState<LibraryEntry>,
    val reviewGames: Map<Int, Game>,
    val lists: PagedState<UserList>,
    val stats: ProfileStatsViewModel.UiState,
    val activities: PagedState<Activity>,
    val activityGames: Map<Int, Game>,
)

// Keyed on the user: signing in as someone else keeps the same top-level entry.
@Composable
internal fun rememberProfileViewModels(userId: String) = ProfileViewModels(
    profile = koinViewModel(key = "profile-$userId") { parametersOf(userId) },
    reviews = koinViewModel(key = "reviews-$userId") { parametersOf(userId) },
    lists = koinViewModel(key = "lists-$userId") { parametersOf(userId) },
    stats = koinViewModel(key = "stats-$userId") { parametersOf(userId) },
    activity = koinViewModel(key = "activity-$userId") { parametersOf(userId) },
)

@Composable
internal fun ProfileViewModels.collectTabs(): ProfileTabsData {
    val reviewsState by this.reviews.reviews.state.collectAsStateWithLifecycle()
    val reviewGames by this.reviews.games.collectAsStateWithLifecycle()
    val listsState by this.lists.lists.state.collectAsStateWithLifecycle()
    val statsState by this.stats.uiState.collectAsStateWithLifecycle()
    val activities by this.activity.activities.state.collectAsStateWithLifecycle()
    val activityGames by this.activity.games.collectAsStateWithLifecycle()
    return ProfileTabsData(reviewsState, reviewGames, listsState, statsState, activities, activityGames)
}
