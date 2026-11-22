package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import it.maicol07.gamerlogue.services.ServiceProfile
import it.maicol07.gamerlogue.ui.components.RemoteImage

/** A connected service's account: small avatar + username. */
@Composable
internal fun ServiceAccountRow(profile: ServiceProfile?) = Row(
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
}
