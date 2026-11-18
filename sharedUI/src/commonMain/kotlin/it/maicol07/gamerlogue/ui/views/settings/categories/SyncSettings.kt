package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.ui.expressive.SettingsButtonGroup
import com.alorma.compose.settings.ui.expressive.SettingsMenuLink
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.sync__history
import gamerlogue.sharedui.generated.resources.sync__history_summary
import gamerlogue.sharedui.generated.resources.sync__period
import gamerlogue.sharedui.generated.resources.sync__period_hours
import gamerlogue.sharedui.generated.resources.sync__period_off
import gamerlogue.sharedui.generated.resources.sync__period_summary
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HistoryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.TimerW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.ui.components.SectionIcon
import org.jetbrains.compose.resources.stringResource

private val SyncPeriods = listOf(0, 3, 6, 12, 24)

/** How often the enabled syncs run on their own, and the way to the history of every sync. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SyncSettings(periodHours: Int, onPeriodSelected: (Int) -> Unit, onOpenHistory: () -> Unit) {
    val labels = SyncPeriods.associateWith {
        if (it == 0) stringResource(Res.string.sync__period_off) else stringResource(Res.string.sync__period_hours, it)
    }
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        SettingsButtonGroup(
            title = { Text(stringResource(Res.string.sync__period)) },
            items = SyncPeriods,
            selectedItem = periodHours,
            onItemSelected = onPeriodSelected,
            icon = {
                SectionIcon(
                    Icons.TimerW500Rounded,
                    MaterialShapes.Cookie4Sided,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
            },
            itemTitleMap = { labels.getValue(it) },
            colors = ListItemDefaults.expressiveSegmentedColors(),
            shapes = ListItemDefaults.segmentedShapes(0, 2),
        )
        SettingsMenuLink(
            title = { Text(stringResource(Res.string.sync__history)) },
            subtitle = { Text(stringResource(Res.string.sync__history_summary)) },
            icon = {
                SectionIcon(
                    Icons.HistoryW500Rounded,
                    MaterialShapes.Clover4Leaf,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
            },
            onClick = onOpenHistory,
            colors = ListItemDefaults.expressiveSegmentedColors(),
            shapes = ListItemDefaults.segmentedShapes(1, 2),
        )
        Text(
            stringResource(Res.string.sync__period_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}
