package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameTimeToBeat
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__age_ratings_title
import gamerlogue.sharedui.generated.resources.game__ratings_igdb_critics
import gamerlogue.sharedui.generated.resources.game__ratings_igdb_user
import gamerlogue.sharedui.generated.resources.game__ratings_title
import gamerlogue.sharedui.generated.resources.game__time_to_beat_completionist
import gamerlogue.sharedui.generated.resources.game__time_to_beat_hastly
import gamerlogue.sharedui.generated.resources.game__time_to_beat_main
import gamerlogue.sharedui.generated.resources.game_time_to_beat
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ScheduleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarShineW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.displayTitle
import it.maicol07.gamerlogue.extensions.igdb.formattedCoverUrl
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.theme.Dimens
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.stringResource

private const val RatingScale = 10

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

    Column(
        Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        Text(stringResource(Res.string.game__ratings_title), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
            for ((pair, count) in ratings) {
                val (labelRes, value) = pair
                RatingTile(stringResource(labelRes), value, count, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RatingTile(label: String, value: Double, count: Int, modifier: Modifier) = Card(modifier) {
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
        if (count > 0) {
            Text(
                text = "($count)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
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

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
    ) {
        Text(
            text = stringResource(Res.string.game__age_ratings_title),
            style = MaterialTheme.typography.titleMedium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for ((ageRating, title) in validRatings) {
                val logoUrl = ageRating.formattedCoverUrl()
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium
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
    val normally = formatTimeToBeat(timeToBeat.normally?.toInt())
    val completely = formatTimeToBeat(timeToBeat.completely?.toInt())
    val hastily = formatTimeToBeat(timeToBeat.hastily?.toInt())

    if (normally == null && completely == null && hastily == null) return

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
    ) {
        Text(
            text = stringResource(Res.string.game_time_to_beat),
            style = MaterialTheme.typography.titleMedium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (normally != null) {
                TimeToBeatCard(label = stringResource(Res.string.game__time_to_beat_main), value = normally)
            }
            if (completely != null) {
                TimeToBeatCard(label = stringResource(Res.string.game__time_to_beat_completionist), value = completely)
            }
            if (hastily != null) {
                TimeToBeatCard(label = stringResource(Res.string.game__time_to_beat_hastly), value = hastily)
            }
        }
    }
}

@Composable
private fun TimeToBeatCard(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.ScheduleW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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
