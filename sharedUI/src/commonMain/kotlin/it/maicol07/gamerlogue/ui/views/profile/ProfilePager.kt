package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.profile__tab_activity
import gamerlogue.sharedui.generated.resources.profile__tab_library
import gamerlogue.sharedui.generated.resources.profile__tab_lists
import gamerlogue.sharedui.generated.resources.profile__tab_reviews
import gamerlogue.sharedui.generated.resources.profile__tab_stats
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.layout.LoadMoreEffect
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The tabs over a pager of their pages: swiping follows the finger, a tab click scrolls there. */
@Composable
internal fun ProfilePager(
    userId: String,
    uiState: ProfileViewModel.UiState,
    viewModels: ProfileViewModels,
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier,
) {
    val tabs = uiState.visibleTabs
    val data = viewModels.collectTabs()
    val pagerState = rememberPagerState(tabs.indexOf(uiState.tab).coerceAtLeast(0)) { tabs.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState.settledPage) {
        tabs.getOrNull(pagerState.settledPage)?.let(viewModels.profile::selectTab)
    }
    Column(modifier) {
        ProfileTabs(tabs, pagerState.currentPage) { scope.launch { pagerState.animateScrollToPage(it) } }
        HorizontalPager(pagerState, Modifier.weight(1f)) { page ->
            TabPage(tabs[page], userId, uiState, viewModels, data, onNavigate)
        }
    }
}

/** A tab's own list, loading when it first shows, even half-swiped in. */
@Composable
private fun TabPage(
    tab: ProfileTab,
    userId: String,
    uiState: ProfileViewModel.UiState,
    viewModels: ProfileViewModels,
    data: ProfileTabsData,
    onNavigate: (NavKey) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(tab) { viewModels.ensureLoaded(tab) }
    LoadMoreEffect(listState) { viewModels.loadMore(tab) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            tabContent(tab, userId, uiState, viewModels, data, onNavigate)
        }
        AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

private fun LazyListScope.tabContent(
    tab: ProfileTab,
    userId: String,
    uiState: ProfileViewModel.UiState,
    viewModels: ProfileViewModels,
    data: ProfileTabsData,
    onNavigate: (NavKey) -> Unit,
) = when (tab) {
    ProfileTab.ACTIVITY -> activityTab(data.activities, data.activityGames, onNavigate)
    ProfileTab.LIBRARY -> libraryTab(uiState.libraryCounts, userId, onNavigate)
    ProfileTab.REVIEWS -> reviewsTab(userId, data.reviews, data.reviewGames, onNavigate)
    ProfileTab.LISTS -> listsTab(
        data.lists,
        uiState.isSelf,
        onCreate = { form -> viewModels.lists.create(form) { onNavigate(RootNavTree.UserListDetail(it)) } },
        onNavigate = onNavigate,
    )
    ProfileTab.STATS -> statsTab(data.stats, viewModels.stats::selectYear)
}

/** The profile's sections. */
@Composable
private fun ProfileTabs(tabs: List<ProfileTab>, selected: Int, onSelect: (Int) -> Unit) =
    PrimaryScrollableTabRow(
        selectedTabIndex = selected,
        edgePadding = Dimens.ScreenPadding,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(top = Dimens.ItemGap),
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(selected = index == selected, onClick = { onSelect(index) }, text = { Text(stringResource(tab.label)) })
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
