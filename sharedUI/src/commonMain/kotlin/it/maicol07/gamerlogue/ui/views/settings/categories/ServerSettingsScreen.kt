package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.expressive.SettingsSwitch
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.auth__cancel
import gamerlogue.sharedui.generated.resources.settings__server
import gamerlogue.sharedui.generated.resources.settings__server_check
import gamerlogue.sharedui.generated.resources.settings__server_igdb_follows
import gamerlogue.sharedui.generated.resources.settings__server_igdb_url
import gamerlogue.sharedui.generated.resources.settings__server_restore
import gamerlogue.sharedui.generated.resources.settings__server_save
import gamerlogue.sharedui.generated.resources.settings__server_sign_out_confirm
import gamerlogue.sharedui.generated.resources.settings__server_sign_out_message
import gamerlogue.sharedui.generated.resources.settings__server_sign_out_title
import gamerlogue.sharedui.generated.resources.settings__server_status_checking
import gamerlogue.sharedui.generated.resources.settings__server_status_igdb_ok
import gamerlogue.sharedui.generated.resources.settings__server_status_igdb_unexpected
import gamerlogue.sharedui.generated.resources.settings__server_status_invalid
import gamerlogue.sharedui.generated.resources.settings__server_status_server_ok
import gamerlogue.sharedui.generated.resources.settings__server_status_server_unexpected
import gamerlogue.sharedui.generated.resources.settings__server_status_unreachable
import gamerlogue.sharedui.generated.resources.settings__server_url
import gamerlogue.sharedui.generated.resources.settings__server_warning
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SettingsBackupRestoreW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WarningW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WebTrafficW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.ui.components.SectionIcon
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun ServerSettingsView(viewModel: ServerSettingsViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The action waiting for the sign-out confirmation, if any.
    var pendingSignOut by remember { mutableStateOf<(() -> Unit)?>(null) }
    val confirmSignOut = { signsOut: Boolean, action: () -> Unit -> if (signsOut) pendingSignOut = action else action() }

    ScreenScaffold(title = Res.string.settings__server) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AdvancedWarning()
            UrlField(
                value = state.serverUrl,
                onValueChange = viewModel::setServerUrl,
                label = Res.string.settings__server_url,
                check = state.serverCheck,
                reachableMessage = Res.string.settings__server_status_server_ok,
                unexpectedMessage = Res.string.settings__server_status_server_unexpected,
            )
            IgdbFollowsServerSwitch(state.igdbApiUrlOverride == null, viewModel::setIgdbFollowsServer)
            // While IGDB follows the server the field shows the derived address, read-only, with its check.
            UrlField(
                value = state.igdbApiUrl,
                onValueChange = viewModel::setIgdbApiUrl,
                label = Res.string.settings__server_igdb_url,
                check = state.igdbCheck,
                reachableMessage = Res.string.settings__server_status_igdb_ok,
                unexpectedMessage = Res.string.settings__server_status_igdb_unexpected,
                enabled = state.igdbApiUrlOverride != null,
            )
            ServerActions(
                canRestore = !state.isDefault || state.isDirty,
                canSave = state.isDirty,
                onRestore = { confirmSignOut(viewModel.restoreSignsOut(), viewModel::restoreDefaults) },
                onCheck = viewModel::check,
                onSave = { confirmSignOut(viewModel.saveSignsOut(), viewModel::save) },
            )
        }
    }

    pendingSignOut?.let { action ->
        SignOutDialog(
            onConfirm = {
                pendingSignOut = null
                action()
            },
            onDismiss = { pendingSignOut = null },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IgdbFollowsServerSwitch(follows: Boolean, onChange: (Boolean) -> Unit) = SettingsSwitch(
    follows,
    title = { Text(stringResource(Res.string.settings__server_igdb_follows)) },
    icon = {
        SectionIcon(
            Icons.WebTrafficW500Rounded,
            MaterialShapes.Cookie6Sided,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
    },
    onCheckedChange = onChange,
    colors = ListItemDefaults.expressiveSegmentedColors(),
    shapes = ListItemDefaults.segmentedShapes(0, 1),
)

/** One row when the three buttons fit, wrapping on narrow windows or long translations. */
@Composable
private fun ServerActions(
    canRestore: Boolean,
    canSave: Boolean,
    onRestore: () -> Unit,
    onCheck: () -> Unit,
    onSave: () -> Unit,
) = FlowRow(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    TextButton(onClick = onRestore, enabled = canRestore, shapes = ButtonDefaults.shapes()) {
        Icon(Icons.SettingsBackupRestoreW500Rounded, null, Modifier.size(ButtonDefaults.IconSize))
        Text(stringResource(Res.string.settings__server_restore), Modifier.padding(start = ButtonDefaults.IconSpacing))
    }
    OutlinedButton(onClick = onCheck, shapes = ButtonDefaults.shapes()) {
        Text(stringResource(Res.string.settings__server_check))
    }
    Button(onClick = onSave, enabled = canSave, shapes = ButtonDefaults.shapes()) {
        Text(stringResource(Res.string.settings__server_save))
    }
}

@Composable
private fun SignOutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
        TextButton(onClick = onConfirm) { Text(stringResource(Res.string.settings__server_sign_out_confirm)) }
    },
    dismissButton = {
        TextButton(onClick = onDismiss) { Text(stringResource(Res.string.auth__cancel)) }
    },
    icon = { Icon(Icons.WarningW500Rounded, null) },
    title = { Text(stringResource(Res.string.settings__server_sign_out_title)) },
    text = { Text(stringResource(Res.string.settings__server_sign_out_message)) },
)

@Composable
private fun AdvancedWarning() = Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ),
) {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.WarningW500Rounded, null)
        Text(stringResource(Res.string.settings__server_warning), style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UrlField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    check: UrlCheck,
    reachableMessage: StringResource,
    unexpectedMessage: StringResource,
    enabled: Boolean = true,
) {
    val message = when (check) {
        UrlCheck.NOT_CHECKED -> null
        UrlCheck.CHECKING -> Res.string.settings__server_status_checking
        UrlCheck.INVALID -> Res.string.settings__server_status_invalid
        UrlCheck.REACHABLE -> reachableMessage
        UrlCheck.UNEXPECTED_RESPONSE -> unexpectedMessage
        UrlCheck.UNREACHABLE -> Res.string.settings__server_status_unreachable
    }
    TextField(
        value,
        onValueChange,
        Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = check in FAILED_CHECKS,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
        trailingIcon = when (check) {
            UrlCheck.CHECKING -> {
                { LoadingIndicator(Modifier.size(24.dp)) }
            }
            UrlCheck.REACHABLE -> {
                { Icon(Icons.CheckCircleW500Rounded, null, tint = MaterialTheme.colorScheme.primary) }
            }
            in FAILED_CHECKS -> {
                { Icon(Icons.ErrorW500Rounded, null) }
            }
            else -> null
        },
        supportingText = message?.let { { Text(stringResource(it)) } },
    )
}

private val FAILED_CHECKS = setOf(UrlCheck.INVALID, UrlCheck.UNEXPECTED_RESPONSE, UrlCheck.UNREACHABLE)
