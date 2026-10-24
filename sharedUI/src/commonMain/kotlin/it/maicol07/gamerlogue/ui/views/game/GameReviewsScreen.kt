package it.maicol07.gamerlogue.ui.views.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__reviews_average
import gamerlogue.sharedui.generated.resources.game__reviews_count
import gamerlogue.sharedui.generated.resources.game__reviews_empty
import gamerlogue.sharedui.generated.resources.game__reviews_title
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RateReviewW500Rounded
import it.maicol07.gamerlogue.extensions.averageRating
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.RatingBadge
import it.maicol07.gamerlogue.ui.components.layout.LoadMoreEffect
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.layout.loadingItem
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import it.maicol07.gamerlogue.ui.views.game.components.GameReviewItem
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun GameReviewsView(gameId: Int) = ScreenScaffold(title = Res.string.game__reviews_title) {
    AuthenticatedContent {
        val navigationState = LocalNavigationState.current
        val viewModel = koinViewModel<GameReviewsViewModel> { parametersOf(gameId) }
        val state by viewModel.reviews.state.collectAsStateWithLifecycle()
        val stats by viewModel.stats.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()
        LaunchedEffect(Unit) { viewModel.reviews.ensureLoaded() }
        LoadMoreEffect(listState, viewModel.reviews::loadMore)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
        ) {
            stats?.let { item(key = "stats") { ReviewStatsCard(it) } }
            itemsIndexed(state.items, key = { _, entry -> "review-${entry.id}" }) { index, entry ->
                GameReviewItem(
                    entry = entry,
                    title = entry.user?.displayName.orEmpty(),
                    shapes = ListItemDefaults.segmentedShapes(index, state.items.size),
                    onClick = {
                        navigationState.backStack.add(RootNavTree.ReviewDetail(entry.id.orEmpty(), entry.userId))
                    }
                )
            }
            loadingItem(state.loading)
            if (state.isEmpty) {
                item(key = "empty") {
                    EmptyState(Icons.RateReviewW500Rounded, stringResource(Res.string.game__reviews_empty), hint = null)
                }
            }
        }
    }
}

/** How many reviews there are and their mean rating, as the screen's highlight. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReviewStatsCard(stats: ReviewStats) = Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    shape = MaterialTheme.shapes.extraLarge,
    modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.SectionGap)
) {
    Row(
        Modifier.padding(Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RatingBadge(
            rating = stats.averageRating,
            shape = MaterialShapes.Cookie9Sided,
            size = StatsBadgeSize,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            textStyle = MaterialTheme.typography.headlineSmallEmphasized
        )
        Column {
            Text(
                stringResource(Res.string.game__reviews_average),
                style = MaterialTheme.typography.titleMediumEmphasized
            )
            Text(
                pluralStringResource(Res.plurals.game__reviews_count, stats.total, stats.total),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private val StatsBadgeSize = 80.dp
