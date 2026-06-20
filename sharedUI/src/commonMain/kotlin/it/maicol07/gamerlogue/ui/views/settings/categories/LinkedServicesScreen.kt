package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__linked_services_disclaimer
import gamerlogue.sharedui.generated.resources.settings__service_epic
import gamerlogue.sharedui.generated.resources.settings__service_gog
import gamerlogue.sharedui.generated.resources.settings__service_playstation
import gamerlogue.sharedui.generated.resources.settings__service_steam
import gamerlogue.sharedui.generated.resources.settings__service_xbox
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AddW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.EpicgamesSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.GogdotcomSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.PlaystationSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.SteamSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.icons.XboxSvgl
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import org.jetbrains.compose.resources.stringResource
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons as MaterialSymbolsIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.Icons as SvglIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LinkedServicesScreen() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.settings__linked_services_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(16.dp)
            )
        }

        // List of services
        ServiceListItem(
            stringResource(Res.string.settings__service_steam),
            Icons.SteamSimpleIcons,
            0,
            isConnected = false
        )
        ServiceListItem(
            stringResource(Res.string.settings__service_playstation),
            Icons.PlaystationSimpleIcons,
            1,
            isConnected = false
        )
        ServiceListItem(stringResource(Res.string.settings__service_xbox), SvglIcons.XboxSvgl, 2, isConnected = false)
        ServiceListItem(
            stringResource(Res.string.settings__service_gog),
            Icons.GogdotcomSimpleIcons,
            3,
            isConnected = false
        )
        ServiceListItem(
            stringResource(Res.string.settings__service_epic),
            Icons.EpicgamesSimpleIcons,
            4,
            isConnected = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ServiceListItem(name: String, icon: ImageVector, index: Int, isConnected: Boolean) = SegmentedListItem(
    colors = ListItemDefaults.expressiveSegmentedColors(),
    shapes = ListItemDefaults.segmentedShapes(index = index, count = 5),
    leadingContent = {
        Image(
            icon,
            contentDescription = null,
            Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
        )
    },
    trailingContent = {
        IconButton(onClick = { /* Handle connection/disconnection logic */ }) {
            Icon(
                imageVector = if (isConnected) MaterialSymbolsIcons.CheckW500Rounded else MaterialSymbolsIcons.AddW500Rounded,
                contentDescription = if (isConnected) "Connected" else "Connect"
            )
        }
    },
    onClick = { /* Handle connection/disconnection logic */ }
) { Text(name) }
