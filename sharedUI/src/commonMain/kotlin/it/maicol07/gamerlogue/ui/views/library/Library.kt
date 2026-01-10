package it.maicol07.gamerlogue.ui.views.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game_card__hours_played
import gamerlogue.sharedui.generated.resources.library__section_all
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarW500Rounded
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.ui.components.ConnectedButtonGroup
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Library(
    viewModel: LibraryViewModel = koinViewModel(),
    onGameClick: (Game) -> Unit = {}
) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    LaunchedEffect(Unit) {
        viewModel.loadLibraryEntries()
    }
    ConnectedButtonGroup(
        listOf(null) + GameLibraryStatus.entries,
        checked = { viewModel.selectedSection == it },
        onCheckedChange = { section, checked ->
            if (checked) viewModel.selectSection(section)
        },
        toggleButtonText = { stringResource(it?.displayName ?: Res.string.library__section_all) },
        toggleButtonIcon = { it?.icon }
    )

    if (viewModel.libraryLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
    } else {
        val sectionLibraryEntries = retain(viewModel.selectedSection) {
            if (viewModel.selectedSection == null) {
                viewModel.libraryGames.values
                    .flatMap { map -> map.entries }
                    .associate { it.key to it.value }
            } else {
                viewModel.libraryGames[viewModel.selectedSection]!!
            }
        }
        if (sectionLibraryEntries.isEmpty()) {
            EmptyLibraryState(section = viewModel.selectedSection)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
//                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(sectionLibraryEntries.entries.toList()) { (game, entry) ->
                    LibraryGameCard(
                        game = game,
                        entry = entry,
                        onClick = { onGameClick(game) }
                    )
                }
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
            val text = when (section) {
                null -> "Your library is empty. Add games to get started!"
                GameLibraryStatus.PLAYING -> "No games currently playing."
                GameLibraryStatus.COMPLETED -> "No completed games yet."
                GameLibraryStatus.PAUSED -> "No paused games."
                GameLibraryStatus.ABANDONED -> "No abandoned games."
                GameLibraryStatus.BACKLOG -> "Your backlog is empty."
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LazyGridItemScope.LibraryGameCard(
    game: Game,
    entry: LibraryEntry,
    onClick: () -> Unit
) = Box(Modifier.animateItem()) {
    // Cover
    game.CoverImage(
        Modifier
            .clickable(onClick = onClick)
            .clip(MaterialTheme.shapes.large)
    )

    // Overlay info
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Rating
        entry.rating?.let { rating ->
            GameCardBadge(
                Icons.StarW500Rounded,
                "%.1f".sprintf(rating)
            )
        }
        // Played time
        entry.playedTime?.let { hours ->
            GameCardBadge(
                Icons.StarW500Rounded,
                stringResource(Res.string.game_card__hours_played, hours)
            )
        }
    }
}

@Composable
private fun GameCardBadge(
    icon: ImageVector,
    text: String
) = Badge(
    modifier = Modifier.wrapContentWidth(),
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.75f)
) {
    Row(
        Modifier.padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            Modifier.size(14.dp)
        )
        Text(text)
    }
}
