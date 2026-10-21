package it.maicol07.gamerlogue.ui.views.profile

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Platform
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.ApiErrorCodes
import it.maicol07.gamerlogue.data.IgdbLookup
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.data.UserStore
import it.maicol07.gamerlogue.data.saveSessionUser
import it.maicol07.gamerlogue.extensions.jsonApiErrorCode
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/**
 * A user's profile: the user, their platforms and, for another user, their library counts. The tabs
 * have their own view models, loaded only when shown.
 */
@KoinViewModel
class ProfileViewModel(@InjectedParam private val userId: String) : StateViewModel<ProfileViewModel.UiState>(
    UiState()
) {
    data class UiState(
        val user: User? = null,
        val loading: Boolean = true,
        val failed: Boolean = false,
        /** A block in either direction hides the profile; [blockedByMe] tells whether the user can lift it. */
        val blocked: Boolean = false,
        val blockedByMe: Boolean = false,
        val isSelf: Boolean = false,
        val following: Boolean = false,
        val followersCount: Int = 0,
        val followPending: Boolean = false,
        val platforms: List<Platform> = emptyList(),
        /** All-time library entries per status, empty for an empty library; another user's profile only. */
        val libraryCounts: Map<String, Int> = emptyMap(),
        /** A pull to refresh is running. */
        val refreshing: Boolean = false,
        val tab: ProfileTab = ProfileTab.ACTIVITY,
    ) {
        /** Another user's private profile: only the header is visible. */
        val contentHidden: Boolean get() = !isSelf && user?.isPrivate == true

        val visibleTabs: List<ProfileTab>
            get() = if (isSelf) ProfileTab.entries - ProfileTab.LIBRARY else ProfileTab.entries
    }

    private val authProvider by inject<AuthTokenProvider>()
    private val userStore by inject<UserStore>()
    private val api by inject<ProfileApi>()
    private val lookup by inject<IgdbLookup>()

    fun selectTab(tab: ProfileTab) = update { copy(tab = tab) }

    /** [load], showing the pull to refresh indicator until it is done. */
    fun refresh() = viewModelScope.launch {
        update { copy(refreshing = true) }
        load().join()
        update { copy(refreshing = false) }
    }

    /** (Re)loads the profile; called whenever the screen shows, so an edit made elsewhere appears. */
    fun load() = viewModelScope.launch {
        val isSelf = userId == authProvider.session.value.userId
        update { copy(loading = user == null, isSelf = isSelf) }
        val result = safeRequest(setOf(ApiErrorCodes.USER_BLOCKED)) { User.find(userId).data }
        val user = result.get()
        if (user == null) {
            val blocked = result.getError()?.jsonApiErrorCode == ApiErrorCodes.USER_BLOCKED
            val blockedByMe = blocked && isBlockedByMe()
            update { copy(loading = false, failed = !blocked, blocked = blocked, blockedByMe = blockedByMe) }
            return@launch
        }
        if (isSelf) userStore.saveSessionUser(authProvider, user)
        update {
            copy(
                user = user,
                loading = false,
                failed = false,
                blocked = false,
                following = user.isFollowedByMe,
                followersCount = user.followersCount,
            )
        }
        if (!state.contentHidden) loadDetails(user)
    }

    /** The owned platforms, for the header, and another user's library counts, for its tab. */
    private suspend fun loadDetails(user: User) {
        val platforms = if (user.platformsIds.isEmpty()) {
            emptyList()
        } else {
            safeRequest { lookup.platforms() }.get().orEmpty().filter { it.id.toInt() in user.platformsIds }
        }
        // The owner has the library destination; visitors get the counts here, none for an empty library
        // (the server sends every status, zeros included).
        val libraryCounts = if (state.isSelf) {
            emptyMap()
        } else {
            safeRequest(ApiErrorCodes.HIDDEN_CONTENT) { api.stats(userId, null).statusCounts }.get()
                ?.takeIf { counts -> counts.values.any { it > 0 } }
                .orEmpty()
        }
        update { copy(platforms = platforms, libraryCounts = libraryCounts) }
    }

    /** Whether the hidden profile is one the current user blocked, rather than one that blocked them. */
    private suspend fun isBlockedByMe(): Boolean = safeRequest { api.blockOf(userId) }.get() != null

    fun toggleFollow() = viewModelScope.launch {
        val wasFollowing = state.following
        update { copy(followPending = true) }
        val result = safeRequest { if (wasFollowing) api.unfollow(userId) else api.follow(userId) }
        update {
            if (result.isOk) {
                copy(
                    followPending = false,
                    following = !wasFollowing,
                    followersCount = followersCount + if (wasFollowing) -1 else 1,
                )
            } else {
                copy(followPending = false)
            }
        }
    }

    fun block() = viewModelScope.launch {
        if (safeRequest { api.block(userId) }.isOk) load()
    }

    fun unblock() = viewModelScope.launch {
        if (safeRequest { api.unblock(userId) }.isOk) load()
    }
}

enum class ProfileTab {
    ACTIVITY,

    /** Another user's profile only: the owner has the library destination. */
    LIBRARY,
    REVIEWS,
    LISTS,
    STATS,
}
