package it.maicol07.gamerlogue.ui.views.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__dates
import gamerlogue.sharedui.generated.resources.library__dates_description
import gamerlogue.sharedui.generated.resources.library__edit_review
import gamerlogue.sharedui.generated.resources.library__edition
import gamerlogue.sharedui.generated.resources.library__edition_description
import gamerlogue.sharedui.generated.resources.library__end_date
import gamerlogue.sharedui.generated.resources.library__hours
import gamerlogue.sharedui.generated.resources.library__owned
import gamerlogue.sharedui.generated.resources.library__owned_description
import gamerlogue.sharedui.generated.resources.library__platform_owned
import gamerlogue.sharedui.generated.resources.library__platforms
import gamerlogue.sharedui.generated.resources.library__platforms_description
import gamerlogue.sharedui.generated.resources.library__played_time
import gamerlogue.sharedui.generated.resources.library__played_time_description
import gamerlogue.sharedui.generated.resources.library__rating
import gamerlogue.sharedui.generated.resources.library__rating_description
import gamerlogue.sharedui.generated.resources.library__review_description
import gamerlogue.sharedui.generated.resources.library__standard_edition
import gamerlogue.sharedui.generated.resources.library__start_date
import gamerlogue.sharedui.generated.resources.library__write_review
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DevicesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HourglassW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Inventory2W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowDownW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RateReviewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StyleW500Rounded
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.MAX_RATING
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.ui.components.DatePickerFieldDialog
import it.maicol07.gamerlogue.ui.components.NumericField
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.currentRatingScale
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.components.game.bottomScrim
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import it.maicol07.gamerlogue.ui.components.reviewPreview
import it.maicol07.gamerlogue.ui.components.social.MarkdownText
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OwnedSwitch(
    owned: Boolean,
    enabled: Boolean,
    onOwnedChange: (Boolean) -> Unit
) {
    SegmentedListLayout(Modifier.fillMaxWidth()) {
        SegmentedListItem(
            checked = owned,
            onCheckedChange = onOwnedChange,
            enabled = enabled,
            shapes = ListItemDefaults.segmentedShapes(0, 1),
            colors = ListItemDefaults.expressiveSegmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            leadingContent = {
                Icon(
                    imageVector = Icons.Inventory2W500Rounded,
                    contentDescription = null
                )
            },
            supportingContent = {
                Text(
                    text = stringResource(Res.string.library__owned_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Switch(
                    checked = owned,
                    onCheckedChange = onOwnedChange,
                    enabled = enabled,
                    thumbContent = {
                        if (owned) {
                            Icon(
                                imageVector = Icons.CheckW500Rounded,
                                contentDescription = stringResource(Res.string.library__owned)
                            )
                        }
                    }
                )
            }
        ) {
            Text(
                text = stringResource(Res.string.library__owned),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * The edition picker as an expandable segmented list: the header sums up the picked editions, and
 * their covers, a choice rarely changed, unfold under it on demand.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LazyItemScope.EditionSection(
    game: Game,
    viewModel: AddToLibrarySheetViewModel
) {
    if (!viewModel.editionsLoading && viewModel.editions.isEmpty()) return
    // Opened from an edition's page, the standard edition is its base game.
    val baseGame = game.version_parent ?: game
    val standardLabel = stringResource(Res.string.library__standard_edition)
    val editions = remember(baseGame, viewModel.editions, standardLabel) {
        listOf(baseGame to standardLabel) + viewModel.editions.map { it to it.version_title.ifEmpty { it.name } }
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val colors = ListItemDefaults.expressiveSegmentedColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    SegmentedListLayout(Modifier.fillMaxWidth()) {
        EditionHeader(
            expanded = expanded,
            loading = viewModel.editionsLoading,
            summary = editions.filter { (edition, _) -> edition.id.toInt() in viewModel.selectedEditions }
                .joinToString { it.second },
            shapes = ListItemDefaults.segmentedShapes(0, if (expanded) 2 else 1),
            colors = colors,
            onToggle = { expanded = !expanded }
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
            exit = shrinkVertically(MaterialTheme.motionScheme.fastSpatialSpec()) +
                fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
        ) {
            // Keeps its bottom shape while collapsing; only the header rounds off at once.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ListItemDefaults.segmentedShapes(1, 2).shape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                EditionCovers(editions, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditionHeader(
    expanded: Boolean,
    loading: Boolean,
    summary: String,
    shapes: ListItemShapes,
    colors: ListItemColors,
    onToggle: () -> Unit
) {
    val arrowTurn by animateFloatAsState(
        if (expanded) HALF_TURN_DEGREES else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec()
    )
    SegmentedListItem(
        onClick = onToggle,
        enabled = !loading,
        shapes = shapes,
        colors = colors,
        leadingContent = { Icon(Icons.StyleW500Rounded, contentDescription = null) },
        supportingContent = {
            Text(
                summary.ifEmpty { stringResource(Res.string.library__edition_description) },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            if (loading) {
                LoadingIndicator(Modifier.size(24.dp))
            } else {
                Icon(
                    Icons.KeyboardArrowDownW500Rounded,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer { rotationZ = arrowTurn }
                )
            }
        }
    ) {
        Text(stringResource(Res.string.library__edition), style = MaterialTheme.typography.titleMedium)
    }
}

// Plain horizontalScroll, not LazyRow: a handful of covers doesn't need virtualization, and
// LazyRow's own scroll/beyond-bounds machinery is the suspected cause of the earlier bug where
// taps stopped landing once the sheet was fully expanded.
@Composable
private fun EditionCovers(editions: List<Pair<Game, String>>, viewModel: AddToLibrarySheetViewModel) = Row(
    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    for ((edition, label) in editions) {
        val editionId = edition.id.toInt()
        EditionCover(
            game = edition,
            label = label,
            selected = editionId in viewModel.selectedEditions,
            onClick = { viewModel.toggleEditionSelection(editionId) }
        )
    }
}

// Plain RemoteImage rather than GameCoverCard/Game.CoverImage: those tag the cover as a shared
// element for the list -> detail nav transition, which crashes ("layouts are not part of the same
// hierarchy") when placed inside a ModalBottomSheet's separate layout hierarchy.
@Composable
private fun EditionCover(game: Game, label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    val coverModifier = Modifier.width(150.dp).height(200.dp)
    // Box has no intrinsic width of its own; inside the horizontalScroll Row (unbounded width) it
    // would otherwise grow to fit the label's fillMaxWidth instead of clipping it with ellipsis.
    Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.BottomStart) {
        RemoteImage(
            game.cover?.let { igdbImageUrl(it.image_id, IgdbImageSize.COVER_BIG) }
                ?: "https://placehold.net/default.png",
            contentDescription = game.name,
            modifier = coverModifier
                .clip(shape)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    }
                )
                .clickable { onClick() }
                .bottomScrim(),
            loadingModifier = coverModifier
        )
        AnimatedVisibility(
            visible = selected,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
        ) {
            Icon(
                imageVector = Icons.CheckW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(4.dp)
            )
        }
        Text(
            text = label,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
            maxLines = 1
        )
    }
}

private const val HALF_TURN_DEGREES = 180f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LazyItemScope.PlatformSection(viewModel: AddToLibrarySheetViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.DevicesW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(Res.string.library__platforms),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = stringResource(Res.string.library__platforms_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    SegmentedListLayout(Modifier.fillMaxWidth()) {
        viewModel.platforms.forEachIndexed { index, platform ->
            val checked = platform in viewModel.selectedPlatforms
            val owned = platform.id in viewModel.ownedPlatforms
            val containerColor = with(MaterialTheme.colorScheme) { if (owned) secondaryContainer else surfaceVariant }
            SegmentedListItem(
                checked = checked,
                onCheckedChange = { viewModel.togglePlatformSelection(platform) },
                shapes = ListItemDefaults.segmentedShapes(index, viewModel.platforms.lastIndex),
                colors = ListItemDefaults.expressiveSegmentedColors(containerColor = containerColor),
                supportingContent = if (owned) ({ Text(stringResource(Res.string.library__platform_owned)) }) else null,
                leadingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { viewModel.togglePlatformSelection(platform) })
                        platform.Image(Modifier.height(24.dp))
                    }
                },
            ) {
                Text(platform.name)
            }
        }
    }
}

@Composable
internal fun DatesSection(
    viewModel: AddToLibrarySheetViewModel,
    showEndDate: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.library__dates),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(Res.string.library__dates_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DatePickerFieldDialog(
            label = stringResource(Res.string.library__start_date),
            initialDate = viewModel.startDate,
            modifier = Modifier.weight(1f)
        ) {
            viewModel.startDate = it
        }
        AnimatedVisibility(visible = showEndDate, modifier = Modifier.weight(1f)) {
            DatePickerFieldDialog(
                label = stringResource(Res.string.library__end_date),
                initialDate = viewModel.endDate,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.endDate = it
            }
        }
    }
}

@Composable
internal fun PlayedTimeField(viewModel: AddToLibrarySheetViewModel) {
    NumericField(
        state = viewModel.playedTime,
        label = stringResource(Res.string.library__played_time),
        leadingIcon = Icons.HourglassW500Rounded,
        suffix = stringResource(Res.string.library__hours),
        supportingText = {
            Text(stringResource(Res.string.library__played_time_description))
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun RatingSection(rating: Float?, onRatingChange: (Float) -> Unit) {
    val scale = currentRatingScale()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.StarW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(Res.string.library__rating, scale.format(rating ?: 0f), scale.max),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = stringResource(Res.string.library__rating_description, scale.max),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    val sliderState = remember {
        SliderState(rating ?: 0f, steps = MAX_RATING - 1, trackRange = 0f..MAX_RATING.toFloat())
    }
    Slider(
        state = sliderState,
        onValueChange = {
            sliderState.value = it
            onRatingChange(it)
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/** The entry to the review editor, with a preview of the review [entry] already has. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReviewRow(entry: LibraryEntry?, enabled: Boolean, onClick: () -> Unit) = SegmentedListLayout(
    Modifier.fillMaxWidth()
) {
    val hasReview = entry?.hasReview == true
    SegmentedListItem(
        onClick = onClick,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        colors = ListItemDefaults.expressiveSegmentedColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        leadingContent = { Icon(Icons.RateReviewW500Rounded, contentDescription = null) },
        supportingContent = {
            MarkdownText(
                if (hasReview) reviewPreview(entry) else stringResource(Res.string.library__review_description),
                style = LocalTextStyle.current,
                maxLines = 2
            )
        },
        trailingContent = { Icon(Icons.KeyboardArrowRightW500Rounded, contentDescription = null) }
    ) {
        Text(stringResource(if (hasReview) Res.string.library__edit_review else Res.string.library__write_review))
    }
}
