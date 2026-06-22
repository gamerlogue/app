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
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.auth__logout
import gamerlogue.sharedui.generated.resources.nav__profile
import gamerlogue.sharedui.generated.resources.profile__subtitle
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun Profile() {
    val authTokenProvider = koinInject<AuthTokenProvider>()

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
            text = stringResource(Res.string.profile__subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = {
            authTokenProvider.updateToken(null)
        }) {
            Text(stringResource(Res.string.auth__logout))
        }
    }
}
