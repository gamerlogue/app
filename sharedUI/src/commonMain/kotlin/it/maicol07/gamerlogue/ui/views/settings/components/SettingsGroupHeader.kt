package it.maicol07.gamerlogue.ui.views.settings.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Title above a segmented group of settings rows. */
@Composable
fun SettingsGroupHeader(title: String, modifier: Modifier = Modifier) = Text(
    title,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier
        .semantics { heading() }
        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
)
