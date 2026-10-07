package it.maicol07.gamerlogue.ui.views.discover

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.home__empty_section
import gamerlogue.sharedui.generated.resources.home__section_error
import gamerlogue.sharedui.generated.resources.home__see_all
import gamerlogue.sharedui.generated.resources.search__global_hint
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowForwardW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.extensions.igdb.gamesNavKey
import it.maicol07.gamerlogue.ui.components.SectionIcon
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.search.GameSearchButton
import it.maicol07.gamerlogue.ui.navigation.DiscoverPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.events.EventsViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

internal val CardWidth = 150.dp
internal val CardHeight = 200.dp
internal val CardCorner = 16.dp
internal val CardPressedCorner = 32.dp

private val SectionSpacing = 28.dp
private const val PLACEHOLDER_COUNT = 4

@Branch(RootTree::class, metadata = DiscoverPaneMetadata::class)
@Composable
fun DiscoverView(
    viewModel: DiscoverViewModel = koinViewModel(),
    eventsViewModel: EventsViewModel = koinViewModel { parametersOf(EventsViewModel.PREVIEW_PAGE_SIZE) }
) {
    val navigationState = LocalNavigationState.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val eventsState by eventsViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Box {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
            contentPadding = PaddingValues(bottom = Dimens.ScreenPadding)
        ) {
            // The first section is always the immersive hero.
            DiscoverSection.entries.forEachIndexed { index, section ->
                discoverSection(
                    section = section,
                    state = uiState.sections.getValue(section),
                    hero = index == 0,
                    onGameClick = { navigationState.backStack.add(it.detailNavKey) },
                    onSeeAllClick = { navigationState.backStack.add(RootNavTree.GameList(section, null, null, null, null)) }
                )
            }
            eventsSection(
                state = eventsState,
                onEventClick = { navigationState.backStack.add(it.gamesNavKey) },
                onSeeAllClick = { navigationState.backStack.add(RootNavTree.EventList) }
            )
        }
        AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        FloatingSearchBar(listState) {
            GameSearchButton(
                placeholder = stringResource(Res.string.search__global_hint),
                onClick = { navigationState.backStack.add(RootNavTree.GameList(null, null, null, null, null)) }
            )
        }
    }
}

/**
 * The search bar floats over the page instead of sitting in a top bar, so the hero artwork runs
 * under it and the status bar: transparent on the artwork, opaque once the hero is gone.
 */
@Composable
private fun FloatingSearchBar(listState: LazyListState, searchBar: @Composable () -> Unit) {
    val heroScrolledAway by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val background by animateColorAsState(
        targetValue = if (heroScrolledAway) MaterialTheme.colorScheme.surface else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )
    Box(Modifier.fillMaxWidth().background(background)) { searchBar() }
}

private fun LazyListScope.discoverSection(
    section: DiscoverSection,
    state: DiscoverViewModel.SectionUiState,
    hero: Boolean,
    onGameClick: (Game) -> Unit,
    onSeeAllClick: () -> Unit
) = item(key = section.name) {
    if (hero) {
        ImmersiveHero(section, state, onGameClick, onSeeAllClick)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
            SectionHeader(
                title = section.sectionTitle,
                icon = section.icon,
                iconShape = section.iconShape,
                iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onSeeAllClick = onSeeAllClick
            )
            when {
                state.loading -> SectionLoading(CardWidth, CardHeight)
                state.error -> SectionMessage(Res.string.home__section_error)
                state.games.isEmpty() -> SectionMessage(Res.string.home__empty_section)
                section == DiscoverSection.MOST_LOVED -> RankedCarousel(state.games, onGameClick)
                else -> GameCarousel(section, state.games, onGameClick)
            }
        }
    }
}

@Composable
internal fun SectionHeader(
    title: StringResource,
    icon: ImageVector,
    iconShape: RoundedPolygon,
    iconContainerColor: Color,
    iconColor: Color,
    onSeeAllClick: () -> Unit
) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap)
) {
    SectionIcon(icon, iconShape, iconContainerColor, iconColor)
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleLargeEmphasized,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
    )
    SeeAllButton(onSeeAllClick)
}

@Composable
internal fun SeeAllButton(onClick: () -> Unit) = FilledTonalButton(
    onClick = onClick,
    shapes = ButtonDefaults.shapes(),
    contentPadding = ButtonDefaults.ExtraSmallContentPadding,
    modifier = Modifier.heightIn(min = ButtonDefaults.ExtraSmallContainerHeight)
) {
    Text(stringResource(Res.string.home__see_all), style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.width(ButtonDefaults.ExtraSmallIconSpacing))
    Icon(
        Icons.ArrowForwardW500Rounded,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.ExtraSmallIconSize)
    )
}

/** Placeholder cards with a single indicator on top, instead of one spinner per card. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SectionLoading(itemWidth: Dp, itemHeight: Dp) =
    Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding)
        ) {
            repeat(PLACEHOLDER_COUNT) {
                Box(
                    Modifier
                        .width(itemWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
                )
            }
        }
        ContainedLoadingIndicator()
    }

@Composable
internal fun SectionMessage(text: StringResource) = Text(
    text = stringResource(text),
    style = MaterialTheme.typography.bodyMedium,
    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val DiscoverSection.iconShape: RoundedPolygon
    get() = when (this) {
        DiscoverSection.POPULAR -> MaterialShapes.SoftBurst
        DiscoverSection.MOST_LOVED -> MaterialShapes.Clover4Leaf
        DiscoverSection.RECENTLY_RELEASED -> MaterialShapes.Sunny
        DiscoverSection.UPCOMING -> MaterialShapes.Cookie6Sided
    }
