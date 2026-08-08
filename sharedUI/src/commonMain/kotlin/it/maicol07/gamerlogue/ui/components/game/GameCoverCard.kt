package it.maicol07.gamerlogue.ui.components.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game

/**
 * A game cover with an optional overlaid title and a metadata badge (rating, release date, …).
 * Scope-free so both the Discover carousel and the GameList grid render an identical card — the
 * caller supplies the clip via [modifier] (carousel: `maskClip`, grid: `clip`).
 */
@Composable
fun GameCoverCard(
    game: Game,
    metadata: List<String>,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
    sizeModifier: Modifier = Modifier.width(CoverWidth).height(CoverHeight),
    onClick: (Game) -> Unit
) {
    Box(
        modifier = modifier
            .then(sizeModifier)
            .clickable { onClick(game) },
        contentAlignment = Alignment.BottomStart
    ) {
        val coverModifier = if (showTitle) Modifier.bottomScrim() else Modifier
        // The cover fills the card so the overlaid title is clipped to the card, not to 150.dp.
        game.CoverImage(coverModifier, sizeModifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (badge in metadata) {
                Surface(
                    color = Color.Black.copy(alpha = MetadataScrimAlpha),
                    contentColor = Color.White,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showTitle,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = game.name,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    shadow = Shadow(Color.Black.copy(alpha = TitleShadowAlpha), Offset(0f, 2f), blurRadius = 2f)
                ),
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                maxLines = 1
            )
        }
    }
}

private const val MetadataScrimAlpha = 0.55f
private const val TitleShadowAlpha = 0.6f
private const val ScrimAlpha = 0.7f
private const val ScrimStart = 0.35f

/** Darkens the bottom of an image so white text overlaid on it stays legible. */
fun Modifier.bottomScrim() = drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = ScrimAlpha)),
            startY = size.height * ScrimStart,
            endY = size.height
        )
    )
}
