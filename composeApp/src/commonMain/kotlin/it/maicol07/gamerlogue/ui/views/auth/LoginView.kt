package it.maicol07.gamerlogue.ui.views.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gamerlogue.composeapp.generated.resources.Res
import gamerlogue.composeapp.generated.resources.auth__login
import gamerlogue.composeapp.generated.resources.auth__login_required_message
import gamerlogue.composeapp.generated.resources.auth__login_required_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LoginW500Rounded
import it.maicol07.gamerlogue.auth.rememberAuthenticationHandler
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginView() = Column(
    Modifier.fillMaxSize().padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val authHandler = rememberAuthenticationHandler()

    Icon(
        modifier = Modifier.size(96.dp),
        imageVector = Icons.LoginW500Rounded,
        contentDescription = stringResource(Res.string.auth__login_required_title),
        tint = MaterialTheme.colorScheme.primary
    )

    Text(
        text = stringResource(Res.string.auth__login_required_title),
        style = MaterialTheme.typography.headlineSmall
    )
    Text(text = stringResource(Res.string.auth__login_required_message))
    Button(
        shapes = ButtonDefaults.shapes(),
        onClick = {
            authHandler.login()
        }
    ) {
        Text(stringResource(Res.string.auth__login))
    }
}
