package it.maicol07.gamerlogue.ui.views.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game_card__hours_played
import gamerlogue.sharedui.generated.resources.home__section_error
import gamerlogue.sharedui.generated.resources.library__empty_all
import gamerlogue.sharedui.generated.resources.nav__library
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.ListPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import it.maicol07.gamerlogue.ui.views.discover.CardHeight
import it.maicol07.gamerlogue.ui.views.discover.CardWidth
import it.maicol07.gamerlogue.ui.views.discover.GameCarousel
import it.maicol07.gamerlogue.ui.views.discover.SectionHeader
import it.maicol07.gamerlogue.ui.views.discover.SectionLoading
import it.maicol07.gamerlogue.ui.views.discover.SectionMessage
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val SectionSpacing = 28.dp

@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun Library() {
    val navigationState = LocalNavigationState.current
    ScreenScaffold(title = Res.string.nav__library) {
        AuthenticatedContent {
            LibraryContent(
                onGameClick = { navigationState.backStack.add(it.detailNavKey) },
                onSeeAllClick = { navigationState.backStack.add(RootNavTree.GameList(null, null, null, null, it)) }
            )
        }
    }
}

/** One Discover-style section per status; empty statuses are hidden once loaded. */
@Composable
private fun LibraryContent(
    viewModel: LibraryViewModel = koinViewModel(),
    onGameClick: (Game) -> Unit,
    onSeeAllClick: (GameLibraryStatus) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadPreviews()
    }
    val sections = uiState.sections.filterValues { it.loading || it.error || it.entries.isNotEmpty() }
    if (sections.isEmpty()) {
        EmptyLibraryState()
        return
    }

    val listState = rememberLazyListState()
    Box {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
            contentPadding = PaddingValues(vertical = Dimens.ScreenPadding)
        ) {
            for ((status, section) in sections) {
                item(key = status.name) {
                    LibrarySection(status, section, onGameClick, onSeeAllClick = { onSeeAllClick(status) })
                }
            }
        }
        AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
private fun LibrarySection(
    status: GameLibraryStatus,
    section: LibraryViewModel.SectionUiState,
    onGameClick: (Game) -> Unit,
    onSeeAllClick: () -> Unit
) = Column(verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
    SectionHeader(
        title = status.displayName,
        icon = status.icon,
        iconShape = status.iconShape,
        iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        onSeeAllClick = onSeeAllClick,
        count = section.count
    )
    when {
        section.loading -> SectionLoading(CardWidth, CardHeight)
        section.error -> SectionMessage(Res.string.home__section_error)
        else -> GameCarousel(
            games = section.entries.keys.toList(),
            onGameClick = onGameClick,
            metadata = { section.entries.getValue(it).cardMetadata() },
            badge = {}
        )
    }
}

/** Hero empty state, shown when every status turns out empty. */
@Composable
private fun EmptyLibraryState() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    EmptyState(
        icon = Icons.JoystickW500Rounded,
        title = stringResource(Res.string.library__empty_all),
        hint = null,
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
    )
}

/** The user's rating and play time, as badges on the game's cover. */
@Composable
internal fun LibraryEntry.cardMetadata(): List<String> = listOfNotNull(
    rating?.let { "★ %.1f".sprintf(it) },
    playedTime?.let { stringResource(Res.string.game_card__hours_played, it) }
)

/** One distinct expressive shape per status, so a section reads by silhouette as well as by label. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val GameLibraryStatus.iconShape: RoundedPolygon
    get() = when (this) {
        GameLibraryStatus.PLAYING -> MaterialShapes.Sunny
        GameLibraryStatus.COMPLETED -> MaterialShapes.Clover4Leaf
        GameLibraryStatus.PAUSED -> MaterialShapes.Cookie6Sided
        GameLibraryStatus.ABANDONED -> MaterialShapes.Cookie4Sided
        GameLibraryStatus.BACKLOG -> MaterialShapes.SoftBurst
    }
