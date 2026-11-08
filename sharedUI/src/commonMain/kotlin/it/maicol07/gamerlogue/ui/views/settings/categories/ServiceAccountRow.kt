package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__service_open_profile
import gamerlogue.sharedui.generated.resources.settings__service_refresh_profile
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RefreshW500Rounded
import it.maicol07.gamerlogue.services.ServiceProfile
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.TooltipBox
import org.jetbrains.compose.resources.stringResource

/**
 * A connected service's account: small avatar + username, a refresh button and, when [onOpenProfile] is
 * non-null, a button opening the account's public page.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ServiceAccountRow(
    profile: ServiceProfile?,
    onRefreshProfile: () -> Unit,
    onOpenProfile: (() -> Unit)?,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
) {
    profile?.avatarUrl?.let { avatar ->
        RemoteImage(
            url = avatar,
            contentDescription = profile.username,
            modifier = Modifier.size(20.dp).clip(CircleShape),
            loadingModifier = Modifier.size(20.dp).clip(CircleShape),
        )
    }
    profile?.username?.let { username ->
        Text(username, style = MaterialTheme.typography.bodyMedium)
    }
    IconButton(
        onClick = onRefreshProfile,
        modifier = Modifier.size(28.dp),
        shapes = IconButtonDefaults.shapes(),
    ) {
        Icon(
            Icons.RefreshW500Rounded,
            contentDescription = stringResource(Res.string.settings__service_refresh_profile),
            modifier = Modifier.size(18.dp),
        )
    }
    onOpenProfile?.let {
        val openProfile = stringResource(Res.string.settings__service_open_profile)
        TooltipBox({ PlainTooltip { Text(openProfile) } }) {
            IconButton(onClick = it, modifier = Modifier.size(28.dp), shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.OpenInNewW500Rounded, contentDescription = openProfile, modifier = Modifier.size(18.dp))
            }
        }
    }
}
