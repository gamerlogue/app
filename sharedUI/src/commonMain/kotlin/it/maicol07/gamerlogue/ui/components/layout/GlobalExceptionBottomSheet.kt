package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.exception__action_close
import gamerlogue.sharedui.generated.resources.exception__action_dismiss
import gamerlogue.sharedui.generated.resources.exception__details_copy
import gamerlogue.sharedui.generated.resources.exception__details_hide
import gamerlogue.sharedui.generated.resources.exception__details_show
import gamerlogue.sharedui.generated.resources.exception__fallback_message
import gamerlogue.sharedui.generated.resources.exception__generic_error
import gamerlogue.sharedui.generated.resources.exception__hint_network
import gamerlogue.sharedui.generated.resources.exception__title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ContentCopyW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ErrorW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowRightW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LightbulbW500Rounded
import it.maicol07.gamerlogue.AppEnvironment
import it.maicol07.gamerlogue.BuildConfig
import it.maicol07.gamerlogue.clipEntryFor
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.ui.components.ButtonIcon
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlobalExceptionBottomSheet(
    errorState: ExceptionReporter.ErrorState,
    reporter: ExceptionReporter,
) {
    val e = errorState.error
    val showTechnicalDetails = BuildConfig.APP_ENV == AppEnvironment.LOCAL

    val fallbackMessage = stringResource(Res.string.exception__fallback_message)
    val genericError = stringResource(Res.string.exception__generic_error)
    val message = if (showTechnicalDetails) e.message ?: fallbackMessage else fallbackMessage
    val errorType = if (showTechnicalDetails) e::class.simpleName ?: genericError else genericError
    val details = remember(e) { if (showTechnicalDetails) e.stackTraceToString() else "" }

    val hint = if (e.looksLikeNetworkFailure()) stringResource(Res.string.exception__hint_network) else null

    var showDetails by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(SheetValue.Hidden)
    val scope = rememberCoroutineScope()

    /** Lets the sheet animate out before the reporter state changes and removes this composable. */
    fun hideThen(onHidden: () -> Unit) = scope.launch {
        sheetState.hide()
        onHidden()
    }

    ModalBottomSheet({ reporter.dismissSheet() }, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(
                        imageVector = Icons.ErrorW500Rounded,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .padding(10.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(Res.string.exception__title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = errorType,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )

            // Network hint
            if (hint != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.LightbulbW500Rounded,
                        null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Technical details
            if (showTechnicalDetails) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            shapes = ButtonDefaults.shapes(),
                            onClick = { showDetails = !showDetails }
                        ) {
                            val rotate by animateFloatAsState(
                                if (showDetails) 90f else 0f,
                                label = "RotateDetailsArrow"
                            )
                            ButtonIcon(
                                imageVector = Icons.KeyboardArrowRightW500Rounded,
                                contentDescription = null,
                                modifier = Modifier.rotate(rotate)
                            )
                            Text(
                                if (showDetails) {
                                    stringResource(Res.string.exception__details_hide)
                                } else {
                                    stringResource(Res.string.exception__details_show)
                                }
                            )
                        }

                        AnimatedVisibility(showDetails) {
                            TooltipBox(
                                TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
                                { PlainTooltip { Text(stringResource(Res.string.exception__details_copy)) } },
                                rememberTooltipState()
                            ) {
                                val clipboard = LocalClipboard.current
                                FilledIconButton(
                                    shapes = IconButtonDefaults.shapes(),
                                    onClick = {
                                        scope.launch {
                                            // TODO: multiplatform clipboard — https://youtrack.jetbrains.com/issue/CMP-7624
                                            clipboard.setClipEntry(clipEntryFor(details))
                                        }
                                    }
                                ) {
                                    Icon(Icons.ContentCopyW500Rounded, stringResource(Res.string.exception__details_copy))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(showDetails) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectionContainer {
                                Text(
                                    text = details,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                )
                            }
                        }
                    }
                }
            }

            // Actions: closing keeps the error reachable from the top bar, dismissing drops it.
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { hideThen(reporter::dismissSheet) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(Res.string.exception__action_close))
                }

                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { hideThen(reporter::clearError) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(Res.string.exception__action_dismiss))
                }
            }
        }
    }
}

private fun Throwable.looksLikeNetworkFailure(): Boolean {
    val text = "${this::class.simpleName.orEmpty()} ${message.orEmpty()}"
    return listOf("timeout", "network", "connect").any { text.contains(it, ignoreCase = true) }
}
