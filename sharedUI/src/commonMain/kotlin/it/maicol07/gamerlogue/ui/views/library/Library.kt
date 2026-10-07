package it.maicol07.gamerlogue.ui.views.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game_card__hours_played
import gamerlogue.sharedui.generated.resources.library__empty_all
import gamerlogue.sharedui.generated.resources.library__section_all
import gamerlogue.sharedui.generated.resources.nav__library
import io.github.fopwoc.nav3ksp.annotation.Branch
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.ui.components.ConnectedButtonGroup
import it.maicol07.gamerlogue.ui.components.game.CoverAspectRatio
import it.maicol07.gamerlogue.ui.components.game.CoverWidth
import it.maicol07.gamerlogue.ui.components.game.GameCoverCard
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.ListPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun Library() {
    val navigationState = LocalNavigationState.current
    ScreenScaffold(title = Res.string.nav__library) {
        AuthenticatedContent {
            LibraryContent(onGameClick = { navigationState.backStack.add(it.detailNavKey) })
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryContent(
    viewModel: LibraryViewModel = koinViewModel(),
    onGameClick: (Game) -> Unit
) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadLibraryEntries()
    }
    ConnectedButtonGroup(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        options = listOf(null) + GameLibraryStatus.entries,
        checked = { uiState.selectedSection == it },
        onCheckedChange = { section, checked ->
            if (checked) viewModel.selectSection(section)
        },
        toggleButtonText = { stringResource(it?.displayName ?: Res.string.library__section_all) },
        toggleButtonIcon = { it?.icon }
    )

    val sectionLibraryEntries = remember(uiState.games, uiState.selectedSection) {
        val section = uiState.selectedSection
        if (section != null) {
            uiState.games[section].orEmpty()
        } else {
            uiState.games.values.flatMap { it.toList() }.toMap()
        }
    }
    when {
        uiState.loading -> CenteredBox { LoadingIndicator() }
        sectionLibraryEntries.isEmpty() -> EmptyLibraryState(section = uiState.selectedSection)
        else -> LibraryGrid(entries = sectionLibraryEntries, onGameClick = onGameClick)
    }
}

@Composable
private fun LibraryGrid(
    entries: Map<Game, LibraryEntry>,
    onGameClick: (Game) -> Unit
) {
    val gridState = rememberLazyGridState()
    Box {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = CoverWidth),
            contentPadding = PaddingValues(Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
        ) {
            items(entries.entries.toList(), key = { (game, _) -> game.id }) { (game, entry) ->
                GameCoverCard(
                    game = game,
                    metadata = listOfNotNull(
                        entry.rating?.let { "★ %.1f".sprintf(it) },
                        entry.playedTime?.let { stringResource(Res.string.game_card__hours_played, it) }
                    ),
                    showTitle = true,
                    modifier = Modifier.animateItem().clip(MaterialTheme.shapes.large),
                    sizeModifier = Modifier.fillMaxWidth().aspectRatio(CoverAspectRatio),
                    onClick = onGameClick
                )
            }
        }
        AppVerticalScrollbar(gridState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
private fun EmptyLibraryState(
    section: GameLibraryStatus?
) = CenteredBox {
    Text(
        text = stringResource(section?.emptyMessage ?: Res.string.library__empty_all),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) { content() }
