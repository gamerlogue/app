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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game_card__hours_played
import gamerlogue.sharedui.generated.resources.library__empty_abandoned
import gamerlogue.sharedui.generated.resources.library__empty_all
import gamerlogue.sharedui.generated.resources.library__empty_backlog
import gamerlogue.sharedui.generated.resources.library__empty_completed
import gamerlogue.sharedui.generated.resources.library__empty_paused
import gamerlogue.sharedui.generated.resources.library__empty_playing
import gamerlogue.sharedui.generated.resources.library__section_all
import it.maicol07.gamerlogue.ui.components.ConnectedButtonGroup
import it.maicol07.gamerlogue.ui.components.game.CoverAspectRatio
import it.maicol07.gamerlogue.ui.components.game.CoverWidth
import it.maicol07.gamerlogue.ui.components.game.GameCoverCard
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.theme.Dimens
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Library(
    viewModel: LibraryViewModel = koinViewModel(),
    onGameClick: (Game) -> Unit = {}
) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadLibraryEntries()
    }
    ConnectedButtonGroup(
        rowModifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        options = listOf(null) + GameLibraryStatus.entries,
        checked = { uiState.selectedSection == it },
        onCheckedChange = { section, checked ->
            if (checked) viewModel.selectSection(section)
        },
        toggleButtonText = { stringResource(it?.displayName ?: Res.string.library__section_all) },
        toggleButtonIcon = { it?.icon }
    )

    if (uiState.loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
    } else {
        val sectionLibraryEntries = if (uiState.selectedSection == null) {
            uiState.games.values
                .flatMap { map -> map.entries }
                .associate { it.key to it.value }
        } else {
            uiState.games[uiState.selectedSection].orEmpty()
        }
        if (sectionLibraryEntries.isEmpty()) {
            EmptyLibraryState(section = uiState.selectedSection)
        } else {
            val gridState = rememberLazyGridState()
            Box {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = CoverWidth),
                    contentPadding = PaddingValues(Dimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
                    verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
                ) {
                    // No item key: the "all" section flattens several status maps, so the same game
                    // can show up twice and a duplicate key would crash the grid.
                    items(sectionLibraryEntries.entries.toList()) { (game, entry) ->
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
    }
}

@Composable
private fun EmptyLibraryState(
    section: GameLibraryStatus?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val text = stringResource(
                when (section) {
                    null -> Res.string.library__empty_all
                    GameLibraryStatus.PLAYING -> Res.string.library__empty_playing
                    GameLibraryStatus.COMPLETED -> Res.string.library__empty_completed
                    GameLibraryStatus.PAUSED -> Res.string.library__empty_paused
                    GameLibraryStatus.ABANDONED -> Res.string.library__empty_abandoned
                    GameLibraryStatus.BACKLOG -> Res.string.library__empty_backlog
                }
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
