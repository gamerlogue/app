package it.maicol07.gamerlogue.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedButtonGroup(
    options: List<T>,
    checked: (T) -> Boolean,
    onCheckedChange: (T, Boolean) -> Unit,
    toggleButtonText: @Composable (T) -> String,
    toggleButtonIcon: (T) -> ImageVector? = { null },
    toggleButtonEnabled: (T) -> Boolean = { true },
    showChecks: Boolean = false,
    toggleButtonModifier: (T) -> Modifier = { Modifier },
    rowModifier: Modifier = Modifier,
    multiple: Boolean = false
) {
    FlowRow(
        modifier = rowModifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        for ((index, type) in options.withIndex()) {
            ToggleButton(
                checked = checked(type),
                onCheckedChange = { onCheckedChange(type, it) },
                enabled = toggleButtonEnabled(type),
                modifier = toggleButtonModifier(
                    type
                ).semantics { role = if (multiple) Role.Checkbox else Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                AnimatedVisibility(showChecks && checked(type)) {
                    Row {
                        ButtonIcon(
                            Icons.CheckW500Rounded,
                            spacing = ToggleButtonDefaults.IconSpacing,
                            size = ToggleButtonDefaults.IconSize
                        )
                    }
                }

                val icon = toggleButtonIcon(type)
                if (icon != null) {
                    ButtonIcon(
                        icon,
                        spacing = ToggleButtonDefaults.IconSpacing,
                        size = ToggleButtonDefaults.IconSize
                    )
                }

                Text(toggleButtonText(type))
            }
        }
    }
}
