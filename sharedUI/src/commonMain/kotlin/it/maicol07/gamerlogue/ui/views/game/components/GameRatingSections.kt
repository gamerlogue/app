package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameTimeToBeat
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__age_ratings_title
import gamerlogue.sharedui.generated.resources.game__rating_description
import gamerlogue.sharedui.generated.resources.game__ratings_igdb_critics
import gamerlogue.sharedui.generated.resources.game__ratings_igdb_user
import gamerlogue.sharedui.generated.resources.game__ratings_title
import gamerlogue.sharedui.generated.resources.game__time_to_beat_completionist
import gamerlogue.sharedui.generated.resources.game__time_to_beat_hastly
import gamerlogue.sharedui.generated.resources.game__time_to_beat_main
import gamerlogue.sharedui.generated.resources.game_time_to_beat
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FamilyStarW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarShineW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TimerW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.displayTitle
import it.maicol07.gamerlogue.extensions.igdb.formattedCoverUrl
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.theme.Dimens
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.stringResource

private const val RatingScale = 10
private const val MaxIgdbRating = 100.0
private const val RatingTrackAlpha = 0.16f
private val RatingGaugeSize = 96.dp
private val RatingGaugeStroke = 6.dp

private val ConnectedTileGap = 2.dp
private val ConnectedOuterCorner = 24.dp
private val ConnectedInnerCorner = 6.dp

/** Seconds above which a time-to-beat value is a duration rather than a plain hour count. */
private const val SecondsThreshold = 300
private const val SecondsPerHour = 3600
private const val SecondsPerMinute = 60

@Composable
internal fun GameRatings(game: Game) {
    val ratings = remember(game) {
        listOfNotNull(
            (Res.string.game__ratings_igdb_user to game.rating to game.rating_count)
                .takeIf { game.rating > 0.0 },
            (Res.string.game__ratings_igdb_critics to game.aggregated_rating to game.aggregated_rating_count)
                .takeIf { game.aggregated_rating > 0.0 }
        )
    }
    if (ratings.isEmpty()) return

    GameSection(stringResource(Res.string.game__ratings_title), Icons.StarShineW500Rounded) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for ((pair, count) in ratings) {
                    val (labelRes, value) = pair
                    RatingGauge(stringResource(labelRes), value, count)
                }
            }
        }
    }
}

/** An IGDB score (0-100) as a wavy ring around its value out of [RatingScale]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RatingGauge(label: String, value: Double, count: Int) {
    val score = "%.1f".sprintf(value / RatingScale)
    val description = stringResource(Res.string.game__rating_description, label, score, count)
    val stroke = with(LocalDensity.current) { Stroke(width = RatingGaugeStroke.toPx(), cap = StrokeCap.Round) }
    // One announcement for the whole gauge; the ring's own progress semantics would read as a running task.
    Column(
        Modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(RatingGaugeSize), contentAlignment = Alignment.Center) {
            CircularWavyProgressIndicator(
                progress = { (value / MaxIgdbRating).toFloat() },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = RatingTrackAlpha),
                stroke = stroke,
                trackStroke = stroke,
                // A score is not a running task: keep the wave still.
                waveSpeed = 0.dp
            )
            Text(score, style = MaterialTheme.typography.headlineMediumEmphasized)
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
        if (count > 0) {
            Text("($count)", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun GameAgeRatings(game: Game) {
    if (game.age_ratings.isEmpty()) return

    val validRatings = remember(game) {
        game.age_ratings.mapNotNull { ageRating ->
            val title = ageRating.displayTitle()
            val logoUrl = ageRating.formattedCoverUrl()
            if (title != null || logoUrl != null) {
                ageRating to (title ?: "")
            } else {
                null
            }
        }.distinctBy { it.second.ifEmpty { it.first.id.toString() } }
    }
    if (validRatings.isEmpty()) return

    GameSection(stringResource(Res.string.game__age_ratings_title), Icons.FamilyStarW500Rounded) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            modifier = Modifier.fillMaxWidth()
        ) {
            for ((ageRating, title) in validRatings) {
                val logoUrl = ageRating.formattedCoverUrl()
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (!logoUrl.isNullOrBlank()) {
                            RemoteImage(
                                url = logoUrl,
                                contentDescription = title,
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.InfoW500Rounded,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GameTimeToBeatSection(timeToBeat: GameTimeToBeat?) {
    if (timeToBeat == null) return
    // Shortest to longest, so the connected tiles read as a progression.
    val entries = listOfNotNull(
        formatTimeToBeat(timeToBeat.hastily?.toInt())?.let { Res.string.game__time_to_beat_hastly to it },
        formatTimeToBeat(timeToBeat.normally?.toInt())?.let { Res.string.game__time_to_beat_main to it },
        formatTimeToBeat(timeToBeat.completely?.toInt())?.let { Res.string.game__time_to_beat_completionist to it },
    )
    if (entries.isEmpty()) return

    GameSection(stringResource(Res.string.game_time_to_beat), Icons.TimerW500Rounded) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(ConnectedTileGap)
        ) {
            entries.forEachIndexed { index, (label, value) ->
                TimeToBeatTile(
                    label = stringResource(label),
                    value = value,
                    shape = connectedTileShape(first = index == 0, last = index == entries.lastIndex),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}

/** Rounded outer ends, tight inner joints: the connected-group look of expressive button groups. */
private fun connectedTileShape(first: Boolean, last: Boolean) = RoundedCornerShape(
    topStart = if (first) ConnectedOuterCorner else ConnectedInnerCorner,
    bottomStart = if (first) ConnectedOuterCorner else ConnectedInnerCorner,
    topEnd = if (last) ConnectedOuterCorner else ConnectedInnerCorner,
    bottomEnd = if (last) ConnectedOuterCorner else ConnectedInnerCorner,
)

@Composable
private fun TimeToBeatTile(label: String, value: String, shape: Shape, modifier: Modifier) = Surface(
    color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    shape = shape,
    modifier = modifier
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleLargeEmphasized)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun formatTimeToBeat(valNum: Int?): String? {
    if (valNum == null || valNum <= 0) return null
    return if (valNum > SecondsThreshold) {
        val hrs = valNum / SecondsPerHour
        val mins = (valNum % SecondsPerHour) / SecondsPerMinute
        when {
            hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
            hrs > 0 -> "${hrs}h"
            else -> "${mins}m"
        }
    } else {
        "${valNum}h"
    }
}
