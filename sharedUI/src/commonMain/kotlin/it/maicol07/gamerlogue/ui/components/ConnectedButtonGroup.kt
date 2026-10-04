package it.maicol07.gamerlogue.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckW500Rounded

@Composable
fun <T> ConnectedButtonGroup(
    options: List<T>,
    checked: (T) -> Boolean,
    onCheckedChange: (T, Boolean) -> Unit,
    toggleButtonText: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    toggleButtonIcon: (T) -> ImageVector? = { null },
    toggleButtonEnabled: (T) -> Boolean = { true },
    showChecks: Boolean = false,
    toggleButtonModifier: (T) -> Modifier = { Modifier },
    multiple: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    /** Arbitrary leading content (e.g. a remote logo); takes precedence over [toggleButtonIcon]. */
    toggleButtonLeading: (@Composable (T) -> Unit)? = null
) {
    FlowRow(
        modifier = modifier,
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
                shapes = connectedShapes(index, options.lastIndex),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = containerColor
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

                if (toggleButtonLeading != null) {
                    toggleButtonLeading(type)
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                } else {
                    toggleButtonIcon(type)?.let { icon ->
                        ButtonIcon(
                            icon,
                            spacing = ToggleButtonDefaults.IconSpacing,
                            size = ToggleButtonDefaults.IconSize
                        )
                    }
                }

                Text(toggleButtonText(type))
            }
        }
    }
}

/** Shapes for the button at [index] of a connected group; a lone button has no neighbours, so stays round. */
@Composable
private fun connectedShapes(index: Int, lastIndex: Int): ToggleButtonShapes = when {
    lastIndex == 0 -> ToggleButtonDefaults.shapes()
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    index == lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}

/**
 * Connected group of plain action buttons: the look of [ConnectedButtonGroup] without toggle
 * semantics, for options that act (open a link) rather than select.
 */
@Composable
fun <T> ConnectedActionButtonGroup(
    options: List<T>,
    onClick: (T) -> Unit,
    buttonText: @Composable (T) -> String,
    buttonIcon: (T) -> ImageVector,
    trailingIcon: ImageVector,
    modifier: Modifier = Modifier,
) = FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
) {
    for ((index, option) in options.withIndex()) {
        val shapes = connectedShapes(index, options.lastIndex)
        Button(
            onClick = { onClick(option) },
            shapes = ButtonDefaults.shapes(shapes.shape, shapes.pressedShape),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            ButtonIcon(buttonIcon(option))
            Text(buttonText(option))
            ButtonIcon(trailingIcon, end = true)
        }
    }
}

@Composable
fun <T> SingleSelectConnectedButtonGroup(
    options: List<T>,
    selected: T?,
    onSelectedChange: (T?) -> Unit,
    toggleButtonText: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    toggleButtonIcon: (T) -> ImageVector? = { null },
    toggleButtonEnabled: (T) -> Boolean = { true },
    showChecks: Boolean = false,
    toggleButtonModifier: (T) -> Modifier = { Modifier },
    deselectable: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest
) {
    ConnectedButtonGroup(
        options = options,
        checked = { selected == it },
        onCheckedChange = { option, isChecked ->
            if (isChecked) {
                onSelectedChange(option)
            } else if (deselectable) {
                onSelectedChange(null)
            }
        },
        toggleButtonText = toggleButtonText,
        toggleButtonIcon = toggleButtonIcon,
        toggleButtonEnabled = toggleButtonEnabled,
        showChecks = showChecks,
        toggleButtonModifier = toggleButtonModifier,
        modifier = modifier,
        multiple = false,
        containerColor = containerColor
    )
}
