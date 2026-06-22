package it.maicol07.gamerlogue.ui.views.library.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Platform
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__dates
import gamerlogue.sharedui.generated.resources.library__edition
import gamerlogue.sharedui.generated.resources.library__end_date
import gamerlogue.sharedui.generated.resources.library__hours
import gamerlogue.sharedui.generated.resources.library__owned
import gamerlogue.sharedui.generated.resources.library__platforms
import gamerlogue.sharedui.generated.resources.library__played_time
import gamerlogue.sharedui.generated.resources.library__rating
import gamerlogue.sharedui.generated.resources.library__review
import gamerlogue.sharedui.generated.resources.library__start_date
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import it.maicol07.gamerlogue.extensions.roundTo
import it.maicol07.gamerlogue.ui.components.DatePickerFieldDialog
import it.maicol07.gamerlogue.ui.components.NumericField
import it.maicol07.gamerlogue.ui.components.game.Image
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
internal fun OwnedSwitch(
    owned: Boolean,
    onOwnedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.library__owned),
            style = MaterialTheme.typography.titleMedium
        )
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
}

@Composable
internal fun LazyItemScope.EditionSection() {
    Text(
        text = stringResource(Res.string.library__edition),
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
internal fun LazyItemScope.PlatformSection(
    game: Game,
    viewModel: AddToLibrarySheetViewModel
) {
    Text(
        text = stringResource(Res.string.library__platforms),
        style = MaterialTheme.typography.titleMedium
    )
    PlatformChips(
        platforms = game.platforms,
        selectedPlatforms = viewModel.selectedPlatforms,
        onPlatformClick = {
            viewModel.togglePlatformSelection(it)
        }
    )
}

@Composable
private fun PlatformChips(
    platforms: List<Platform>,
    selectedPlatforms: List<Platform>,
    onPlatformClick: (Platform) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (platform in platforms) {
            val platformName = platform.name
            FilterChip(
                leadingIcon = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AnimatedVisibility(platform in selectedPlatforms) {
                            Icon(
                                imageVector = Icons.CheckW500Rounded,
                                contentDescription = null
                            )
                        }
                        platform.Image(Modifier.height(24.dp))
                    }
                },
                selected = platform in selectedPlatforms,
                onClick = { onPlatformClick(platform) },
                label = { Text(platformName) }
            )
        }
    }
}

@Composable
internal fun DatesSection(
    viewModel: AddToLibrarySheetViewModel,
    showEndDate: Boolean
) {
    Text(
        text = stringResource(Res.string.library__dates),
        style = MaterialTheme.typography.titleMedium
    )
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
        suffix = stringResource(Res.string.library__hours),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun RatingSection(viewModel: AddToLibrarySheetViewModel) {
    Text(
        text = stringResource(
            Res.string.library__rating,
            viewModel.rating?.toDouble()?.roundTo(1) ?: 0f
        ),
        style = MaterialTheme.typography.titleMedium
    )
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
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        lineLimits = TextFieldLineLimits.MultiLine()
    )
}
