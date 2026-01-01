package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import gamerlogue.composeapp.generated.resources.Res
import gamerlogue.composeapp.generated.resources.nav__profile
import it.maicol07.gamerlogue.auth.LocalAuthTokenProvider
import org.jetbrains.compose.resources.stringResource

@Composable
fun Profile() {
    val authTokenProvider = LocalAuthTokenProvider.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.nav__profile),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Il tuo profilo e impostazioni",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = {
            authTokenProvider.setToken(null)
        }) {
            Text("Logout")
        }
    }
}
