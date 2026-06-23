package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Artwork
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Genre
import at.released.igdbclient.model.Screenshot
import at.released.igdbclient.model.Theme
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__description_title
import gamerlogue.sharedui.generated.resources.game__genres_title
import gamerlogue.sharedui.generated.resources.game__no_description
import gamerlogue.sharedui.generated.resources.game__ratings_igdb_critics
import gamerlogue.sharedui.generated.resources.game__ratings_igdb_user
import gamerlogue.sharedui.generated.resources.game__ratings_title
import gamerlogue.sharedui.generated.resources.game__themes_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarShineW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.icon
import it.maicol07.gamerlogue.extensions.igdb.localizedName
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.components.imageviewer.FullscreenImageViewer
import it.maicol07.gamerlogue.ui.theme.Dimens
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.stringResource

const val Ratio169 = 16f / 9f

/** Renders the scrollable content of the Game detail screen for a loaded [game]. */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
internal fun LazyListScope.gameDetailContent(game: Game) {
    item { GameHeader(game) }
    item { GameRatings(game) }
    item { GameGenresAndThemes(game) }
    item { GameMedia(game) }
    item { GameDescription(game) }
    item { GameDetailsList(game) }
    item { Spacer(Modifier.height(12.dp)) }
}

private const val RatingScale = 10

@Composable
private fun GameRatings(game: Game) {
    val ratings = remember(game) {
        listOf(
            Res.string.game__ratings_igdb_user to game.rating,
            Res.string.game__ratings_igdb_critics to game.aggregated_rating
        ).filter { it.second > 0.0 }
    }
    if (ratings.isEmpty()) return

    Column(
        Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        Text(stringResource(Res.string.game__ratings_title), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
            for ((labelRes, value) in ratings) {
                RatingTile(stringResource(labelRes), value, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RatingTile(label: String, value: Double, modifier: Modifier) = Card(modifier) {
    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Icon(
                Icons.StarShineW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = "%.1f".sprintf(value / RatingScale),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "/$RatingScale",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GameGenresAndThemes(game: Game) {
    val data = remember(game) {
        mapOf(
            Res.string.game__genres_title to game.genres,
            Res.string.game__themes_title to game.themes
        )
    }
    for ((title, values) in data) {
        if (values.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
            ) {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleMedium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (value in values) {
                        val (name, icon) = when (value) {
                            is Genre -> value.localizedName to value.icon
                            is Theme -> value.localizedName to value.icon
                            else -> "" to null
                        }

                        AssistChip(
                            onClick = {},
                            leadingIcon = icon?.let {
                                {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = null
                                    )
                                }
                            },
                            label = { Text(name) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameMedia(game: Game) {
    val items = remember(game) {
        buildList {
            addAll(game.videos)
            addAll(game.artworks)
            addAll(game.screenshots)
        }
    }

    var showViewer by remember { mutableStateOf(false) }
    var initialViewerIndex by remember { mutableStateOf(0) }

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.count() },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        preferredItemWidth = 200.dp,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) { i ->
        val item = items[i]
        val modifier = Modifier
            .maskClip(MaterialTheme.shapes.large)
            .aspectRatio(Ratio169)
            .clickable {
                initialViewerIndex = i
                showViewer = true
            }
        when (item) {
            is Artwork -> item.Image(modifier)
            is Screenshot -> item.Image(modifier)
            else -> {}
        }
    }

    if (showViewer) {
        FullscreenImageViewer(
            imagesCount = items.size,
            initialPage = initialViewerIndex,
            onDismissRequest = { showViewer = false },
            imageContent = { page, modifier ->
                when (val item = items[page]) {
                    is Artwork -> item.Image(modifier.aspectRatio(Ratio169))
                    is Screenshot -> item.Image(modifier.aspectRatio(Ratio169))
                    else -> {}
                }
            },
            thumbnailContent = { page, modifier ->
                when (val item = items[page]) {
                    is Artwork -> item.Image(modifier.aspectRatio(Ratio169))
                    is Screenshot -> item.Image(modifier.aspectRatio(Ratio169))
                    else -> {}
                }
            }
        )
    }
}

@Composable
private fun GameDescription(game: Game) {
    val summary = game.summary.takeIf { it.isNotBlank() } ?: stringResource(Res.string.game__no_description)
    Column(
        Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(stringResource(Res.string.game__description_title), style = MaterialTheme.typography.titleMedium)
        Text(summary)
    }
}
