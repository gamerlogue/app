package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__service_webview_collapse
import gamerlogue.sharedui.generated.resources.settings__service_webview_expand
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowDownW500Rounded
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val HALF_TURN_DEGREES = 180f

/**
 * The WebView sheet's handle: the standard drag handle plus a button that collapses or expands the sheet —
 * the only way to resize it while it can't be dragged (the WebView is interactive during sign-in).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SheetToggleHandle(sheetState: SheetState) {
    val expanded = sheetState.targetValue == SheetValue.Expanded
    // Expanded, the sheet reaches the top of the (edge-to-edge) window: keep the handle out of the status bar.
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topInset by animateDpAsState(if (expanded) statusBar else 0.dp, MaterialTheme.motionScheme.defaultSpatialSpec())
    // The spatial spring overshoots, briefly going below 0 when collapsing; padding rejects negatives.
    Box(Modifier.fillMaxWidth().padding(top = topInset.coerceAtLeast(0.dp))) {
        SheetHandleContent(sheetState, expanded)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BoxScope.SheetHandleContent(sheetState: SheetState, expanded: Boolean) {
    BottomSheetDefaults.DragHandle(Modifier.align(Alignment.Center))
    val arrowTurn by animateFloatAsState(
        if (expanded) 0f else HALF_TURN_DEGREES,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
    )
    val label = stringResource(
        if (expanded) Res.string.settings__service_webview_collapse else Res.string.settings__service_webview_expand,
    )
    val scope = rememberCoroutineScope()
    TooltipBox(
        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        { PlainTooltip { Text(label) } },
        rememberTooltipState(),
        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
    ) {
        IconButton(
            onClick = { scope.launch { if (expanded) sheetState.partialExpand() else sheetState.expand() } },
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                Icons.KeyboardArrowDownW500Rounded,
                contentDescription = label,
                modifier = Modifier.graphicsLayer { rotationZ = arrowTurn },
            )
        }
    }
}
