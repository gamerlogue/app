package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__reviews_all
import gamerlogue.sharedui.generated.resources.game__reviews_title
import gamerlogue.sharedui.generated.resources.game__your_review
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RateReviewW500Rounded
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.displayTimestamp
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.ui.components.RatingBadge
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import it.maicol07.gamerlogue.ui.components.reviewPreview
import it.maicol07.gamerlogue.ui.components.social.SpoilerText
import it.maicol07.gamerlogue.ui.components.user.UserAvatar
import it.maicol07.gamerlogue.ui.views.game.ReviewStats
import org.jetbrains.compose.resources.stringResource

/** How many reviews the game page previews before "see all". */
private const val PREVIEW_COUNT = 3

private val AvatarSize = 40.dp
private val BadgeSize = 48.dp

/**
 * A preview of the game's reviews: the signed-in user's own first, then the most liked ones, and a
 * button to all of them. Hidden while there is none.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GameReviewsSection(
    reviews: List<LibraryEntry>,
    stats: ReviewStats?,
    ownEntry: LibraryEntry?,
    onReviewClick: (LibraryEntry) -> Unit,
    onAllReviewsClick: () -> Unit,
) {
    // The listed copy carries the author, which the library entry is loaded without.
    val own = ownEntry?.takeIf { it.hasReview }?.let { entry -> reviews.find { it.id == entry.id } ?: entry }
    val shown = listOfNotNull(own) + reviews.filter { it.id != own?.id }.take(PREVIEW_COUNT)
    if (shown.isEmpty()) return
    GameSection(stringResource(Res.string.game__reviews_title), Icons.RateReviewW500Rounded) {
        SegmentedListLayout(Modifier.fillMaxWidth()) {
            val ownTitle = stringResource(Res.string.game__your_review)
            shown.forEachIndexed { index, entry ->
                GameReviewItem(
                    entry = entry,
                    title = if (entry === own) ownTitle else entry.user?.displayName.orEmpty(),
                    shapes = ListItemDefaults.segmentedShapes(index, shown.size),
                    onClick = { onReviewClick(entry) }
                )
            }
        }
        val total = stats?.total ?: shown.size
        if (total > shown.size) {
            FilledTonalButton(onClick = onAllReviewsClick, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.game__reviews_all, total))
                Icon(
                    Icons.KeyboardArrowRightW500Rounded,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            }
        }
    }
}

/** One review of a game: its author, when it was written, the start of its text and its rating. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GameReviewItem(entry: LibraryEntry, title: String, shapes: ListItemShapes, onClick: () -> Unit) {
    val preview = reviewPreview(entry)
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = ListItemDefaults.expressiveSegmentedColors(),
        leadingContent = { UserAvatar(entry.user, AvatarSize) },
        overlineContent = displayTimestamp(entry.reviewUpdatedAt)?.let { { Text(it) } },
        supportingContent = if (preview.isBlank()) {
            null
        } else {
            { SpoilerText(preview, entry.reviewSpoiler, MaterialTheme.typography.bodyMedium, maxLines = PREVIEW_LINES) }
        },
        trailingContent = entry.rating?.let { rating ->
            {
                RatingBadge(
                    rating = rating,
                    shape = MaterialShapes.Cookie6Sided,
                    size = BadgeSize,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    textStyle = MaterialTheme.typography.labelLargeEmphasized
                )
            }
        }
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private const val PREVIEW_LINES = 3
