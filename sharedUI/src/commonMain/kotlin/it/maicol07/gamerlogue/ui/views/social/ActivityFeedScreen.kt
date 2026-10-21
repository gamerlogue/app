package it.maicol07.gamerlogue.ui.views.social

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.social__feed
import gamerlogue.sharedui.generated.resources.social__feed_empty
import gamerlogue.sharedui.generated.resources.social__feed_empty_hint
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.GroupW500Rounded
import it.maicol07.gamerlogue.core.PagedState
import it.maicol07.gamerlogue.data.Activity
import it.maicol07.gamerlogue.extensions.gameId
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.layout.LoadMoreEffect
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.layout.loadingItem
import it.maicol07.gamerlogue.ui.navigation.ListPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** The activities of the users the current one follows, opened from Discover. */
@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun ActivityFeedView() {
    val navigationState = LocalNavigationState.current
    ScreenScaffold(title = Res.string.social__feed) {
        AuthenticatedContent {
            val viewModel = koinViewModel<ActivityViewModel>(key = "feed") { parametersOf(null) }
            val activities by viewModel.activities.state.collectAsStateWithLifecycle()
            val games by viewModel.games.collectAsStateWithLifecycle()
            val listState = rememberLazyListState()
            LaunchedEffect(Unit) { viewModel.activities.ensureLoaded() }
            LoadMoreEffect(listState, viewModel.activities::loadMore)
            Box(Modifier.fillMaxSize()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    activityItems(activities, games, navigationState.backStack::add)
                    if (activities.isEmpty) item { FeedEmptyState() }
                }
                AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}

/** The loaded activities, followed by a loading row while the next page arrives. */
fun LazyListScope.activityItems(state: PagedState<Activity>, games: Map<Int, Game>, onNavigate: (NavKey) -> Unit) {
    items(state.items, key = { "activity-${it.id}" }) { activity ->
        ActivityItem(activity, activity.gameId?.let(games::get), onNavigate)
    }
    loadingItem(state.loading)
}

@Composable
internal fun FeedEmptyState() = EmptyState(
    icon = Icons.GroupW500Rounded,
    title = stringResource(Res.string.social__feed_empty),
    hint = stringResource(Res.string.social__feed_empty_hint),
    modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding),
)
