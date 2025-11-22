package it.maicol07.gamerlogue.ui.views.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.CarouselItemScope
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.composeapp.generated.resources.Res
import gamerlogue.composeapp.generated.resources.home__see_all
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowForwardW500Rounded
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun Home(viewModel: HomeViewModel = koinViewModel()) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        for (section in HomeViewModel.HomeSectionType.entries) {
            val games by derivedStateOf { viewModel.games[section] ?: emptyList() }
            homeSection(
                title = section.sectionTitle,
                icon = section.icon,
                list = games,
                loading = viewModel.loading[section] == true,
                navigateTo = {
                    if (it is Game) {
                        viewModel.navigateToGame(it)
                        return@homeSection
                    }
                    viewModel.navigateToList(section)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.homeSection(
    title: StringResource,
    icon: ImageVector,
    list: List<Game>,
    loading: Boolean,
    navigateTo: (Game?) -> Unit
) {
    item {
        HomeSectionHeader(
            title = title,
            icon = icon,
            onSeeAllClick = {
                navigateTo(null)
            }
        )

        if (loading) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
                repeat(4) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator()
                        }
                    }
                }
            }
        } else {
            HorizontalMultiBrowseCarousel(
                state = rememberCarouselState { list.count() },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                preferredItemWidth = 150.dp,
                itemSpacing = 12.dp,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) { i ->
                val showTitle = carouselItemDrawInfo.size > 200
                val game = list[i]
                HomeSectionCarouselItem(
                    game = game,
                    showTitle = showTitle,
                    onItemClick = navigateTo
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeSectionHeader(
    title: StringResource,
    icon: ImageVector,
    onSeeAllClick: () -> Unit
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        },
        headlineContent = {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleMedium
            )
        },
        trailingContent = {
            FilledTonalIconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = onSeeAllClick,
            ) {
                Icon(Icons.ArrowForwardW500Rounded, stringResource(Res.string.home__see_all))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CarouselItemScope.HomeSectionCarouselItem(
    game: Game,
    showTitle: Boolean,
    onItemClick: (Game) -> Unit
) {
    Box(contentAlignment = Alignment.BottomStart) {
        val coverModifier = Modifier
            .maskClip(MaterialTheme.shapes.large)
            .let { base ->
                if (showTitle) {
                    base.drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                                startY = size.height * 0.4f,
                                endY = size.height
                            )
                        )
                    }
                } else {
                    base
                }
            }

        game.CoverImage(
            coverModifier.clickable {
                onItemClick(game)
            }
        )

        AnimatedVisibility(showTitle) {
            Text(
                text = game.name,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    shadow = Shadow(
                        Color.Black.copy(alpha = 0.6f),
                        Offset(0f, 2f),
                        blurRadius = 2f
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                maxLines = 1
            )
        }
    }
}
