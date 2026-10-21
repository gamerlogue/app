package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common__more_options
import gamerlogue.sharedui.generated.resources.common_cancel
import gamerlogue.sharedui.generated.resources.nav__settings
import gamerlogue.sharedui.generated.resources.profile__block
import gamerlogue.sharedui.generated.resources.profile__block_confirm_message
import gamerlogue.sharedui.generated.resources.profile__block_confirm_title
import gamerlogue.sharedui.generated.resources.profile__blocked_users
import gamerlogue.sharedui.generated.resources.profile__edit
import gamerlogue.sharedui.generated.resources.profile__report
import gamerlogue.sharedui.generated.resources.profile__unblock
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.MoreVertW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SettingsW500Rounded
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingsAction(onClick: () -> Unit) = IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes()) {
    Icon(Icons.SettingsW500Rounded, contentDescription = stringResource(Res.string.nav__settings))
}

/** The own profile's bar: edit, settings and a menu with the blocked users. */
@Composable
internal fun SelfActions(onEdit: () -> Unit) {
    val navigationState = LocalNavigationState.current
    IconButton(onClick = onEdit, shapes = IconButtonDefaults.shapes()) {
        Icon(Icons.EditW500Rounded, contentDescription = stringResource(Res.string.profile__edit))
    }
    SettingsAction { navigationState.backStack.add(RootNavTree.Settings) }
    OverflowMenu(
        listOf(Res.string.profile__blocked_users to { navigationState.backStack.add(RootNavTree.BlockedUsers) })
    )
}

/** Another user's menu: block (after a confirmation) or unblock, and report. */
@Composable
internal fun OtherUserMenu(
    blocked: Boolean,
    blockedByMe: Boolean,
    onBlock: () -> Unit,
    onUnblock: () -> Unit,
    onReport: () -> Unit,
) {
    var confirmBlock by remember { mutableStateOf(false) }
    val blockEntry = when {
        !blocked -> Res.string.profile__block to { confirmBlock = true }
        blockedByMe -> Res.string.profile__unblock to onUnblock
        else -> null
    }
    OverflowMenu(listOfNotNull(blockEntry, Res.string.profile__report to onReport))
    if (confirmBlock) BlockConfirmDialog(onConfirm = onBlock, onDismiss = { confirmBlock = false })
}

/** A "more" button opening [entries]; each entry closes the menu before running. */
@Composable
private fun OverflowMenu(entries: List<Pair<StringResource, () -> Unit>>) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.MoreVertW500Rounded, contentDescription = stringResource(Res.string.common__more_options))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            entries.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        menuOpen = false
                        action()
                    }
                )
            }
        }
    }
}

@Composable
private fun BlockConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.profile__block_confirm_title)) },
    text = { Text(stringResource(Res.string.profile__block_confirm_message)) },
    confirmButton = {
        TextButton(
            onClick = {
                onDismiss()
                onConfirm()
            },
            shapes = ButtonDefaults.shapes(),
        ) { Text(stringResource(Res.string.profile__block)) }
    },
    dismissButton = {
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(Res.string.common_cancel))
        }
    },
)
