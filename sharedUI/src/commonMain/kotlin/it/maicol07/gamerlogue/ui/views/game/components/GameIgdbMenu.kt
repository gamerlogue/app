package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common__more_options
import gamerlogue.sharedui.generated.resources.game__igdb_about
import gamerlogue.sharedui.generated.resources.game__igdb_about_body
import gamerlogue.sharedui.generated.resources.game__igdb_about_title
import gamerlogue.sharedui.generated.resources.game__igdb_edit
import gamerlogue.sharedui.generated.resources.game__igdb_open
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.MoreVertW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.ui.components.ButtonIcon
import org.jetbrains.compose.resources.stringResource

private val InfoIconContainerSize = 72.dp
private val InfoIconSize = 36.dp

/**
 * Overflow menu pointing to the game's IGDB page, where its data comes from and can be corrected.
 *
 * ponytail: the edit link is the page URL plus `/edit`, not verifiable from here (IGDB blocks
 * automated requests); if IGDB moves its editor, only [igdbEditUrl] changes.
 */
@Composable
fun GameIgdbMenu(igdbUrl: String) = Box {
    val uriHandler = LocalUriHandler.current
    var expanded by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

    FilledTonalIconButton(onClick = { expanded = true }, shapes = IconButtonDefaults.shapes()) {
        Icon(Icons.MoreVertW500Rounded, contentDescription = stringResource(Res.string.common__more_options))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.game__igdb_open)) },
            leadingIcon = { Icon(Icons.OpenInNewW500Rounded, contentDescription = null) },
            onClick = {
                expanded = false
                uriHandler.openURL(igdbUrl)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.game__igdb_edit)) },
            leadingIcon = { Icon(Icons.EditW500Rounded, contentDescription = null) },
            onClick = {
                expanded = false
                uriHandler.openURL(igdbEditUrl(igdbUrl))
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.game__igdb_about)) },
            leadingIcon = { Icon(Icons.InfoW500Rounded, contentDescription = null) },
            onClick = {
                expanded = false
                showInfo = true
            }
        )
    }

    if (showInfo) {
        IgdbInfoSheet(igdbUrl) { showInfo = false }
    }
}

private fun igdbEditUrl(igdbUrl: String) = "${igdbUrl.trimEnd('/')}/edit"

/** Explains that the data comes from IGDB and can be fixed there, with the same two links. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IgdbInfoSheet(igdbUrl: String, onDismissRequest: () -> Unit) = ModalBottomSheet(onDismissRequest) {
    val uriHandler = LocalUriHandler.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier
                .size(InfoIconContainerSize)
                .background(MaterialTheme.colorScheme.tertiaryContainer, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.InfoW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(InfoIconSize)
            )
        }
        Text(
            stringResource(Res.string.game__igdb_about_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(Res.string.game__igdb_about_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { uriHandler.openURL(igdbUrl) }, shapes = ButtonDefaults.shapes()) {
                ButtonIcon(Icons.OpenInNewW500Rounded)
                Text(stringResource(Res.string.game__igdb_open))
            }
            Button(onClick = { uriHandler.openURL(igdbEditUrl(igdbUrl)) }, shapes = ButtonDefaults.shapes()) {
                ButtonIcon(Icons.EditW500Rounded)
                Text(stringResource(Res.string.game__igdb_edit))
            }
        }
    }
}
