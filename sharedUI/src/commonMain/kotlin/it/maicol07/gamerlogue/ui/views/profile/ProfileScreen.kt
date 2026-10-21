package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.nav__profile
import gamerlogue.sharedui.generated.resources.profile__private
import gamerlogue.sharedui.generated.resources.profile__private_hint
import gamerlogue.sharedui.generated.resources.profile__tab_activity
import gamerlogue.sharedui.generated.resources.profile__tab_library
import gamerlogue.sharedui.generated.resources.profile__tab_lists
import gamerlogue.sharedui.generated.resources.profile__tab_reviews
import gamerlogue.sharedui.generated.resources.profile__tab_stats
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LockW500Rounded
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.data.InteractionTarget
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.ui.components.layout.AppTopBar
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.layout.LoadMoreEffect
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.social.ReportDialog
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.LoginView
import it.maicol07.gamerlogue.ui.views.list.GamePick
import it.maicol07.gamerlogue.ui.views.list.GamePickEffect
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/** The signed-in user's own profile, the top-level destination; the login screen when signed out. */
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun ProfileView() {
    val navigationState = LocalNavigationState.current
    val authProvider = koinInject<AuthTokenProvider>()
    val session by authProvider.session.collectAsStateWithLifecycle()
    val userId = session.userId
    if (userId == null) {
        ScreenScaffold(
            title = Res.string.nav__profile,
            actions = { SettingsAction { navigationState.backStack.add(RootNavTree.Settings) } }
        ) {
            LoginView()
        }
    } else {
        UserProfileScreen(userId)
    }
}

/** Any user's profile, opened from a feed, a comment, a follower list… */
@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun UserProfileView(userId: String) = UserProfileScreen(userId)

@Composable
private fun UserProfileScreen(userId: String) {
    val viewModels = rememberProfileViewModels(userId)
    val uiState by viewModels.profile.uiState.collectAsStateWithLifecycle()
    val navigationState = LocalNavigationState.current
    val editViewModel = koinViewModel<EditProfileViewModel>()
    var reportOpen by remember { mutableStateOf(false) }
    // Saveable: picking the banner leaves for the game list and comes back to the open editor.
    var editOpen by rememberSaveable { mutableStateOf(false) }

    // Every time the profile shows again (back from a review, a list…), so what changed meanwhile appears.
    LifecycleResumeEffect(Unit) {
        viewModels.reload()
        onPauseOrDispose {}
    }
    // Only the own profile asks for a banner: another profile on screen must not take its pick.
    if (uiState.isSelf) {
        GamePickEffect(BannerPickRequest) { ids ->
            ids.firstOrNull()?.let(editViewModel::pickBannerGame)
            editOpen = true
        }
    }
    val showTabs = uiState.user != null && !uiState.contentHidden
    LaunchedEffect(uiState.tab, showTabs) { if (showTabs) viewModels.ensureLoaded(uiState.tab) }

    ScreenScaffold(
        topBar = {
            ProfileTopBar(uiState, viewModels.profile, onEdit = { editOpen = true }, onReport = { reportOpen = true })
        }
    ) {
        ProfileContent(userId, uiState, viewModels, showTabs)
    }

    if (reportOpen) ReportDialog(InteractionTarget.USER, userId, onDismiss = { reportOpen = false })
    if (editOpen) {
        EditProfileSheet(
            editViewModel,
            onDismiss = { editOpen = false },
            onSaved = viewModels::reload,
            onPickBanner = {
                editOpen = false
                val pick = GamePick(BannerPickRequest, multiple = false)
                navigationState.backStack.add(RootNavTree.GameList(null, null, null, null, null, null, pick))
            },
        )
    }
}

/** The [GamePick] request under which the game list hands back the banner's game. */
private const val BannerPickRequest = "profile-banner"

@Composable
private fun ProfileTopBar(
    uiState: ProfileViewModel.UiState,
    viewModel: ProfileViewModel,
    onEdit: () -> Unit,
    onReport: () -> Unit,
) = AppTopBar(
    title = if (uiState.isSelf) stringResource(Res.string.nav__profile) else uiState.user?.displayName,
    actions = {
        if (uiState.isSelf) {
            SelfActions(onEdit)
        } else if (uiState.user != null || uiState.blocked) {
            OtherUserMenu(
                blocked = uiState.blocked,
                blockedByMe = uiState.blockedByMe,
                onBlock = viewModel::block,
                onUnblock = viewModel::unblock,
                onReport = onReport,
            )
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ProfileContent(
    userId: String,
    uiState: ProfileViewModel.UiState,
    viewModels: ProfileViewModels,
    showTabs: Boolean,
) {
    val navigationState = LocalNavigationState.current
    val tabs = viewModels.collectTabs()
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    LoadMoreEffect(listState) { if (showTabs) viewModels.loadMore(uiState.tab) }
    PullToRefreshBox(
        isRefreshing = uiState.refreshing,
        onRefresh = viewModels::refresh,
        state = pullState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullState,
                isRefreshing = uiState.refreshing,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            uiState.user?.let { user ->
                headerItem(userId, user, uiState, viewModels.profile, navigationState.backStack::add)
            }
            profileBody(userId, uiState, viewModels, tabs, navigationState.backStack::add)
        }
        AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

private fun LazyListScope.headerItem(
    userId: String,
    user: User,
    uiState: ProfileViewModel.UiState,
    viewModel: ProfileViewModel,
    onNavigate: (NavKey) -> Unit,
) = item(key = "header") {
    ProfileHeader(
        user = user,
        state = uiState,
        onToggleFollow = viewModel::toggleFollow,
        onConnections = { kind -> onNavigate(RootNavTree.UserConnections(userId, kind)) },
    )
}

/** Everything under the header: a status message, or the tabs and the selected tab's content. */
private fun LazyListScope.profileBody(
    userId: String,
    uiState: ProfileViewModel.UiState,
    viewModels: ProfileViewModels,
    tabs: ProfileTabsData,
    onNavigate: (NavKey) -> Unit,
) {
    when {
        uiState.loading -> item { ProfileLoading() }
        uiState.blocked -> item {
            ProfileBlocked(canUnblock = uiState.blockedByMe, onUnblock = viewModels.profile::unblock)
        }
        uiState.failed -> item { ProfileLoadError(onRetry = viewModels.profile::load) }
        uiState.contentHidden -> item {
            ProfileMessage(Icons.LockW500Rounded, Res.string.profile__private, Res.string.profile__private_hint)
        }
        uiState.user != null -> {
            stickyHeader(key = "tabs") {
                ProfileTabs(uiState.visibleTabs, uiState.tab, viewModels.profile::selectTab)
            }
            selectedTab(userId, uiState, viewModels, tabs, onNavigate)
        }
    }
}

private fun LazyListScope.selectedTab(
    userId: String,
    uiState: ProfileViewModel.UiState,
    viewModels: ProfileViewModels,
    tabs: ProfileTabsData,
    onNavigate: (NavKey) -> Unit,
) = when (uiState.tab) {
    ProfileTab.ACTIVITY -> activityTab(tabs.activities, tabs.activityGames, onNavigate)
    ProfileTab.LIBRARY -> libraryTab(uiState.libraryCounts, userId, onNavigate)
    ProfileTab.REVIEWS -> reviewsTab(userId, tabs.reviews, tabs.reviewGames, onNavigate)
    ProfileTab.LISTS -> listsTab(
        tabs.lists,
        uiState.isSelf,
        onCreate = { form -> viewModels.lists.create(form) { onNavigate(RootNavTree.UserListDetail(it)) } },
        onNavigate = onNavigate,
    )
    ProfileTab.STATS -> statsTab(tabs.stats, viewModels.stats::selectYear)
}

/** The profile's sections, pinned under the bar while the content scrolls. */
@Composable
private fun ProfileTabs(tabs: List<ProfileTab>, selected: ProfileTab, onSelect: (ProfileTab) -> Unit) =
    PrimaryScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0),
        edgePadding = Dimens.ScreenPadding,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(top = Dimens.ItemGap),
    ) {
        tabs.forEach { tab ->
            Tab(selected = tab == selected, onClick = { onSelect(tab) }, text = { Text(stringResource(tab.label)) })
        }
    }

private val ProfileTab.label: StringResource
    get() = when (this) {
        ProfileTab.ACTIVITY -> Res.string.profile__tab_activity
        ProfileTab.LIBRARY -> Res.string.profile__tab_library
        ProfileTab.REVIEWS -> Res.string.profile__tab_reviews
        ProfileTab.LISTS -> Res.string.profile__tab_lists
        ProfileTab.STATS -> Res.string.profile__tab_stats
    }
