package it.maicol07.gamerlogue.ui.components.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.activity__unknown_game
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ChatBubbleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FavoriteW500Rounded
import it.maicol07.gamerlogue.data.LibraryEntrySchema
import it.maicol07.gamerlogue.extensions.displayTimestamp
import it.maicol07.gamerlogue.ui.components.currentRatingScale
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

private val ReviewCoverWidth = 56.dp
private val ReviewCoverHeight = 75.dp
private const val ReviewPreviewLines = 4

/** A review in a list: the game, the rating, the start of the text and its like and comment counts. */
@Composable
fun ReviewCard(entry: LibraryEntrySchema, game: Game?, onClick: () -> Unit, modifier: Modifier = Modifier) = Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
) {
    Row(Modifier.padding(Dimens.SectionGap), horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
        game?.CoverImage(
            Modifier.clip(MaterialTheme.shapes.small),
            sizeModifier = Modifier.width(ReviewCoverWidth).height(ReviewCoverHeight)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                game?.name ?: stringResource(Res.string.activity__unknown_game),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
                val scale = currentRatingScale()
                entry.rating?.let { Text("★ ${scale.format(it)}", style = MaterialTheme.typography.labelLarge) }
                displayTimestamp(entry.reviewUpdatedAt)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            SpoilerText(
                entry.review.orEmpty(),
                spoiler = entry.reviewSpoiler,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = ReviewPreviewLines
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
                CountLabel(Icons.FavoriteW500Rounded, entry.likesCount)
                CountLabel(Icons.ChatBubbleW500Rounded, entry.commentsCount)
            }
        }
    }
}

@Composable
internal fun CountLabel(icon: ImageVector, count: Int) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        count.toString(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
