package it.maicol07.gamerlogue.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipBox(
    tooltip: @Composable TooltipScope.() -> Unit,
    modifier: Modifier = Modifier,
    position: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    state: TooltipState = rememberTooltipState(),
    onDismissRequest: (() -> Unit)? = null,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    hasAction: Boolean = false,
    content: @Composable () -> Unit
) = TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(position),
    tooltip = tooltip,
    state = state,
    onDismissRequest = onDismissRequest,
    focusable = focusable,
    enableUserInput = enableUserInput,
    hasAction = hasAction,
    modifier = modifier,
    content = content
)
