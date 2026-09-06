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
import gamerlogue.sharedui.generated.resources.calendar__placeholder
import gamerlogue.sharedui.generated.resources.nav__calendar
import io.github.fopwoc.nav3ksp.annotation.Branch
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import org.jetbrains.compose.resources.stringResource

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun Calendar() = ScreenScaffold(title = Res.string.nav__calendar) {
    AuthenticatedContent {
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
                text = stringResource(Res.string.calendar__placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
