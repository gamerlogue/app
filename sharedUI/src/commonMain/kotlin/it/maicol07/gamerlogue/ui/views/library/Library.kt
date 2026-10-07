package it.maicol07.gamerlogue.ui.views.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game_card__hours_played
import gamerlogue.sharedui.generated.resources.library__empty_all
import gamerlogue.sharedui.generated.resources.library__section_all
import gamerlogue.sharedui.generated.resources.nav__library
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
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

private typealias LibrarySection = Pair<GameLibraryStatus, Map<Game, LibraryEntry>>

private val HeroShapeSize = 96.dp
private val HeroIconSize = 48.dp
private val HeaderIconContainerSize = 36.dp
private val HeaderIconSize = 20.dp

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
) = Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
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

    // One section when a status is selected, every non-empty status otherwise.
    val sections = remember(uiState.games, uiState.selectedSection) {
        val selected = uiState.selectedSection
        uiState.games
            .filterKeys { selected == null || it == selected }
            .map { (status, entries) -> status to entries }
            .filter { (_, entries) -> entries.isNotEmpty() }
    }
    when {
        uiState.loading -> CenteredBox { ContainedLoadingIndicator() }
        sections.isEmpty() -> EmptyLibraryState(section = uiState.selectedSection)
        else -> LibraryGrid(
            sections = sections,
            showHeaders = uiState.selectedSection == null,
            onGameClick = onGameClick
        )
    }
}

@Composable
private fun LibraryGrid(
    sections: List<LibrarySection>,
    showHeaders: Boolean,
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
            for ((status, entries) in sections) {
                if (showHeaders) {
                    item(key = "header_${status.name}", span = { GridItemSpan(maxLineSpan) }) {
                        LibrarySectionHeader(status, entries.size)
                    }
                }
                gameItems(status, entries, onGameClick)
            }
        }
        AppVerticalScrollbar(gridState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

private fun LazyGridScope.gameItems(
    status: GameLibraryStatus,
    entries: Map<Game, LibraryEntry>,
    onGameClick: (Game) -> Unit
) = items(
    entries.entries.toList(),
    // Prefixed with the status: a game listed under two statuses would otherwise clash.
    key = { (game, _) -> "${status.name}_${game.id}" }
) { (game, entry) ->
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

/** Status title with its icon set in an expressive shape, plus how many games it holds. */
@Composable
private fun LibrarySectionHeader(status: GameLibraryStatus, count: Int) = Row(
    modifier = Modifier.fillMaxWidth().padding(top = Dimens.ItemGap).semantics { heading() },
    horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
    verticalAlignment = Alignment.CenterVertically
) {
    ExpressiveIcon(
        icon = status.icon,
        shape = status.iconShape,
        containerSize = HeaderIconContainerSize,
        iconSize = HeaderIconSize,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    )
    Text(
        text = stringResource(status.displayName),
        style = MaterialTheme.typography.titleLargeEmphasized,
        modifier = Modifier.weight(1f)
    )
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Hero empty state: the screen's single expressive moment. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyLibraryState(
    section: GameLibraryStatus?
) = CenteredBox {
    Column(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
    ) {
        ExpressiveIcon(
            icon = section?.icon ?: Icons.JoystickW500Rounded,
            shape = section?.iconShape ?: MaterialShapes.Cookie9Sided,
            containerSize = HeroShapeSize,
            iconSize = HeroIconSize,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = stringResource(section?.emptyMessage ?: Res.string.library__empty_all),
            style = MaterialTheme.typography.titleLargeEmphasized,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveIcon(
    icon: ImageVector,
    shape: RoundedPolygon,
    containerSize: Dp,
    iconSize: Dp,
    containerColor: Color,
    contentColor: Color
) = Box(
    modifier = Modifier.size(containerSize).background(containerColor, shape.toShape()),
    contentAlignment = Alignment.Center
) {
    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(iconSize))
}

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

@Composable
private fun CenteredBox(content: @Composable () -> Unit) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) { content() }
