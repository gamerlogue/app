package it.maicol07.gamerlogue.ui.views.library.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__dates
import gamerlogue.sharedui.generated.resources.library__dates_description
import gamerlogue.sharedui.generated.resources.library__edition
import gamerlogue.sharedui.generated.resources.library__edition_description
import gamerlogue.sharedui.generated.resources.library__end_date
import gamerlogue.sharedui.generated.resources.library__hours
import gamerlogue.sharedui.generated.resources.library__owned
import gamerlogue.sharedui.generated.resources.library__owned_description
import gamerlogue.sharedui.generated.resources.library__platforms
import gamerlogue.sharedui.generated.resources.library__platforms_description
import gamerlogue.sharedui.generated.resources.library__played_time
import gamerlogue.sharedui.generated.resources.library__played_time_description
import gamerlogue.sharedui.generated.resources.library__rating
import gamerlogue.sharedui.generated.resources.library__rating_description
import gamerlogue.sharedui.generated.resources.library__review
import gamerlogue.sharedui.generated.resources.library__review_description
import gamerlogue.sharedui.generated.resources.library__standard_edition
import gamerlogue.sharedui.generated.resources.library__start_date
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DevicesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HourglassTopW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Inventory2W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RateReviewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StyleW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveShape
import it.maicol07.gamerlogue.extensions.roundTo
import it.maicol07.gamerlogue.ui.components.DatePickerFieldDialog
import it.maicol07.gamerlogue.ui.components.NumericField
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.components.game.bottomScrim
import it.maicol07.gamerlogue.ui.components.layout.SegmentedListLayout
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OwnedSwitch(
    owned: Boolean,
    onOwnedChange: (Boolean) -> Unit
) {
    SegmentedListLayout(Modifier.fillMaxWidth()) {
        SegmentedListItem(
            checked = owned,
            onCheckedChange = onOwnedChange,
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LazyItemScope.EditionSection(
    game: Game,
    viewModel: AddToLibrarySheetViewModel
) {
    if (!viewModel.editionsLoading && viewModel.editions.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.StyleW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(Res.string.library__edition),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = stringResource(Res.string.library__edition_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    if (viewModel.editionsLoading) {
        LoadingIndicator()
        return
    }
    // Plain horizontalScroll, not LazyRow: a handful of covers doesn't need virtualization, and
    // LazyRow's own scroll/beyond-bounds machinery is the suspected cause of the earlier bug where
    // taps stopped landing once the sheet was fully expanded.
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val gameId = game.id.toInt()
        EditionCover(
            game = game,
            label = stringResource(Res.string.library__standard_edition),
            selected = gameId in viewModel.selectedEditions,
            onClick = { viewModel.toggleEditionSelection(gameId) }
        )
        for (edition in viewModel.editions) {
            val editionId = edition.id.toInt()
            EditionCover(
                game = edition,
                label = edition.version_title ?: edition.name.orEmpty(),
                selected = editionId in viewModel.selectedEditions,
                onClick = { viewModel.toggleEditionSelection(editionId) }
            )
        }
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LazyItemScope.PlatformSection(
    game: Game,
    viewModel: AddToLibrarySheetViewModel
) {
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
        game.platforms.forEachIndexed { index, platform ->
            val checked = platform in viewModel.selectedPlatforms
            SegmentedListItem(
                checked = checked,
                onCheckedChange = { viewModel.togglePlatformSelection(platform) },
                shapes = ListItemDefaults.segmentedShapes(index, game.platforms.lastIndex),
                colors = ListItemDefaults.expressiveSegmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
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
            initialDate = viewModel.startDate?.toEpochMilliseconds(),
            modifier = Modifier.weight(1f)
        ) {
            viewModel.startDate = it?.let { Instant.fromEpochMilliseconds(it) }
        }
        AnimatedVisibility(visible = showEndDate, modifier = Modifier.weight(1f)) {
            DatePickerFieldDialog(
                label = stringResource(Res.string.library__end_date),
                initialDate = viewModel.endDate?.epochSeconds,
                modifier = Modifier.fillMaxWidth()
            ) {
                viewModel.endDate = it?.let { Instant.fromEpochMilliseconds(it) }
            }
        }
    }
}

@Composable
internal fun PlayedTimeField(viewModel: AddToLibrarySheetViewModel) {
    NumericField(
        state = viewModel.playedTime,
        label = stringResource(Res.string.library__played_time),
        leadingIcon = Icons.HourglassTopW500Rounded,
        suffix = stringResource(Res.string.library__hours),
        supportingText = {
            Text(stringResource(Res.string.library__played_time_description))
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun RatingSection(viewModel: AddToLibrarySheetViewModel) {
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
                text = stringResource(
                    Res.string.library__rating,
                    viewModel.rating?.toDouble()?.roundTo(1) ?: 0f
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = stringResource(Res.string.library__rating_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Slider(
        value = viewModel.rating?.toFloat() ?: 0f,
        onValueChange = { viewModel.rating = it },
        valueRange = 0f..10f,
        steps = 0,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun ReviewSection(viewModel: AddToLibrarySheetViewModel) {
    TextField(
        state = viewModel.review,
        label = { Text(stringResource(Res.string.library__review)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.RateReviewW500Rounded,
                contentDescription = null
            )
        },
        supportingText = {
            Text(stringResource(Res.string.library__review_description))
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        lineLimits = TextFieldLineLimits.MultiLine()
    )
}
