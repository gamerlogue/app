package it.maicol07.gamerlogue.ui.views.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.nav__calendar
import org.jetbrains.compose.resources.stringResource

@Composable
fun Calendar() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.nav__calendar),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Qui vedrai le prossime uscite e i tuoi eventi",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
