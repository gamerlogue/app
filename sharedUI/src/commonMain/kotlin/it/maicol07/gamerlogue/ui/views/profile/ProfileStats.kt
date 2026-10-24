package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.profile__load_error
import gamerlogue.sharedui.generated.resources.stats__all_time
import gamerlogue.sharedui.generated.resources.stats__average_rating
import gamerlogue.sharedui.generated.resources.stats__completed
import gamerlogue.sharedui.generated.resources.stats__completed_by_year
import gamerlogue.sharedui.generated.resources.stats__hours
import gamerlogue.sharedui.generated.resources.stats__owned
import gamerlogue.sharedui.generated.resources.stats__platforms
import gamerlogue.sharedui.generated.resources.stats__platforms_hint
import gamerlogue.sharedui.generated.resources.stats__ratings
import gamerlogue.sharedui.generated.resources.stats__ratings_count
import gamerlogue.sharedui.generated.resources.stats__reviews
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AndroidWifi3BarAlertW500Rounded
import it.maicol07.gamerlogue.data.MAX_RATING
import it.maicol07.gamerlogue.data.UserStats
import it.maicol07.gamerlogue.ui.components.RatingScale
import it.maicol07.gamerlogue.ui.components.currentRatingScale
import it.maicol07.gamerlogue.ui.components.layout.loadingItem
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val BarHeight = 12.dp
private val BarLabelWidth = 96.dp
private val BarCorner = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
private val TileMinWidth = 140.dp

/** Year filter, headline numbers, ratings, platforms and completions per year. */
internal fun LazyListScope.statsTab(state: ProfileStatsViewModel.UiState, onSelectYear: (Int?) -> Unit) {
    item(key = "stats-years") { YearChips(state, onSelectYear) }
    val stats = state.stats
    when {
        // A reload keeps the stats on screen.
        state.loading && stats == null -> loadingItem(true)
        stats == null -> if (state.failed) {
            item { ProfileMessage(Icons.AndroidWifi3BarAlertW500Rounded, Res.string.profile__load_error, null) }
        }
        else -> {
            item(key = "stats-tiles") { StatTiles(stats) }
            if (stats.ratingHistogram.any { it > 0 }) {
                item(key = "stats-ratings") {
                    ProfileSection(Res.string.stats__ratings) { RatingsCard(stats) }
                }
            }
            if (stats.platforms.isNotEmpty()) {
                item(key = "stats-platforms") {
                    ProfileSection(Res.string.stats__platforms) {
                        Bars(
                            stats.platforms.sortedByDescending { it.entriesCount }.map { platform ->
                                (state.platforms[platform.platformId]?.name ?: "#${platform.platformId}") to platform.entriesCount
                            }
                        )
                        Text(
                            stringResource(Res.string.stats__platforms_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
                        )
                    }
                }
            }
            if (state.year == null && stats.completedByYear.isNotEmpty()) {
                item(key = "stats-years-completed") {
                    ProfileSection(Res.string.stats__completed_by_year) {
                        Bars(stats.completedByYear.entries.sortedByDescending { it.key }.map { it.key to it.value })
                    }
                }
            }
        }
    }
}

@Composable
private fun YearChips(state: ProfileStatsViewModel.UiState, onSelectYear: (Int?) -> Unit) = Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(
        rememberScrollState()
    ).padding(horizontal = Dimens.ScreenPadding),
    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
) {
    FilterChip(
        selected = state.year == null,
        onClick = { onSelectYear(null) },
        label = { Text(stringResource(Res.string.stats__all_time)) }
    )
    (state.years + listOfNotNull(state.year)).distinct().sortedDescending().forEach { year ->
        FilterChip(selected = state.year == year, onClick = { onSelectYear(year) }, label = { Text(year.toString()) })
    }
}

/** The headline numbers: hours, completed and owned games, reviews; the average rating heads [RatingsCard]. */
@Composable
private fun StatTiles(stats: UserStats) = FlowRow(
    modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding),
    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
    verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
    maxItemsInEachRow = 2,
) {
    val tileModifier = Modifier.weight(1f).widthIn(min = TileMinWidth)
    StatTile(stats.totalPlayedTime.toString(), stringResource(Res.string.stats__hours), tileModifier)
    StatTile(
        stats.statusCounts[GameLibraryStatus.COMPLETED.name]?.toString() ?: "0",
        stringResource(Res.string.stats__completed),
        tileModifier
    )
    StatTile(stats.reviewsCount.toString(), stringResource(Res.string.stats__reviews), tileModifier)
    StatTile(stats.ownedCount.toString(), stringResource(Res.string.stats__owned), tileModifier)
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) = Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = MaterialTheme.shapes.large,
    modifier = modifier,
) {
    Column(Modifier.padding(Dimens.ScreenPadding)) {
        Text(value, style = MaterialTheme.typography.headlineMediumEmphasized)
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * One series as horizontal bars, longest = full width: label on the left, the count after the bar.
 * A single hue, so no legend; the numbers are in text color, not the bar's.
 */
@Composable
private fun Bars(values: List<Pair<String, Int>>) = Column(
    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
    verticalArrangement = Arrangement.spacedBy(6.dp),
) {
    val max = values.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1
    values.forEach { (label, count) ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $count" },
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(BarLabelWidth)
            )
            Box(Modifier.weight(1f).height(BarHeight)) {
                if (count > 0) {
                    Box(
                        Modifier
                            .fillMaxWidth(count.toFloat() / max)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary, BarCorner)
                    )
                }
            }
            Text(count.toString(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * The average rating as the hero number in an expressive shape, over the distribution as columns: one
 * hue, no legend, only the tallest column (or the tapped one) labelled with its count.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RatingsCard(stats: UserStats) = Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = MaterialTheme.shapes.extraLarge,
    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
) {
    Column(Modifier.padding(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap)
        ) {
            Box(
                Modifier
                    .size(AverageShapeSize)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stats.averageRating?.let { currentRatingScale().format(it) } ?: "–",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column {
                Text(
                    stringResource(Res.string.stats__average_rating),
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
                val total = stats.ratingHistogram.sum()
                Text(
                    pluralStringResource(Res.plurals.stats__ratings_count, total, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RatingColumns(stats.ratingHistogram)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RatingColumns(histogram: List<Int>) {
    val max = histogram.maxOrNull()?.takeIf { it > 0 } ?: 1
    val scale = currentRatingScale()
    var selected by remember { mutableStateOf<Int?>(null) }
    // The columns grow in once, when the card first shows.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap / 2)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(RatingChartHeight),
            horizontalArrangement = Arrangement.spacedBy(RatingColumnGap),
            verticalAlignment = Alignment.Bottom,
        ) {
            histogram.forEachIndexed { bucket, count ->
                val fraction by animateFloatAsState(
                    targetValue = if (shown) count.toFloat() / max else 0f,
                    animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                )
                RatingColumn(
                    label = ratingBucketLabel(bucket, scale),
                    count = count,
                    fraction = fraction,
                    highlighted = selected?.let { it == bucket } ?: (count == max),
                    onClick = { selected = bucket.takeIf { it != selected } },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(RatingColumnGap)) {
            histogram.indices.forEach { bucket ->
                Text(
                    (bucket * RatingBucketSize * scale.max / MAX_RATING).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** One bucket: its column from the baseline and, when [highlighted], its count above it. */
@Composable
private fun RowScope.RatingColumn(
    label: String,
    count: Int,
    fraction: Float,
    highlighted: Boolean,
    onClick: () -> Unit
) =
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $count" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (highlighted && count > 0) Text(count.toString(), style = MaterialTheme.typography.labelMedium, maxLines = 1)
        val color = when {
            count == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
            highlighted -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.primary.copy(alpha = DimmedColumnAlpha)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction.coerceAtLeast(MinColumnFraction) * ColumnHeightShare)
                .background(color, ColumnShape)
        )
    }

private val AverageShapeSize = 72.dp
private val RatingChartHeight = 120.dp
private val RatingColumnGap = 2.dp
private val ColumnShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
private const val MinColumnFraction = 0.03f

/** The share of the chart's height the tallest column takes, leaving room for its count above. */
private const val ColumnHeightShare = 0.85f
private const val DimmedColumnAlpha = 0.45f

/** The server buckets ratings by tens on its 0–100 scale; the last bucket holds 100 alone. */
private fun ratingBucketLabel(bucket: Int, scale: RatingScale): String = if (bucket == LastRatingBucket) {
    "★ ${scale.format(MAX_RATING)}"
} else {
    "★ ${scale.format(bucket * RatingBucketSize)}–${scale.format(bucket * RatingBucketSize + RatingBucketSize - 1)}"
}

private const val RatingBucketSize = 10
private const val LastRatingBucket = 10
