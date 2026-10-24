package it.maicol07.gamerlogue.ui.components.social

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.common_cancel
import gamerlogue.sharedui.generated.resources.common_ok
import gamerlogue.sharedui.generated.resources.report__note
import gamerlogue.sharedui.generated.resources.report__reason_harassment
import gamerlogue.sharedui.generated.resources.report__reason_hate
import gamerlogue.sharedui.generated.resources.report__reason_impersonation
import gamerlogue.sharedui.generated.resources.report__reason_inappropriate
import gamerlogue.sharedui.generated.resources.report__reason_other
import gamerlogue.sharedui.generated.resources.report__reason_spam
import gamerlogue.sharedui.generated.resources.report__reason_spoiler
import gamerlogue.sharedui.generated.resources.report__send
import gamerlogue.sharedui.generated.resources.report__sent
import gamerlogue.sharedui.generated.resources.report__title
import gamerlogue.sharedui.generated.resources.social__like
import gamerlogue.sharedui.generated.resources.social__spoiler_reveal
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FavoriteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FavoriteW500RoundedFill
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.VisibilityOffW500Rounded
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import it.maicol07.gamerlogue.data.InteractionTarget
import it.maicol07.gamerlogue.data.Report
import it.maicol07.gamerlogue.data.ReportReason
import it.maicol07.gamerlogue.data.ReportSchema
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** A like toggle with its count. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LikeButton(
    liked: Boolean,
    count: Int,
    /** The like or unlike is being sent: an indicator replaces the heart and the button waits. */
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) =
    ToggleButton(
        checked = liked,
        onCheckedChange = { onClick() },
        enabled = enabled && !loading,
        contentPadding = ButtonDefaults.ExtraSmallContentPadding,
        modifier = modifier,
        shapes = ToggleButtonDefaults.shapesFor(ButtonDefaults.ExtraSmallContainerHeight),
    ) {
        AnimatedContent(loading, transitionSpec = { fadeIn() togetherWith fadeOut() }) { busy ->
            if (busy) {
                LoadingIndicator(Modifier.size(ButtonDefaults.ExtraSmallIconSize), color = LocalContentColor.current)
            } else {
                Icon(
                    if (liked) Icons.FavoriteW500RoundedFill else Icons.FavoriteW500Rounded,
                    contentDescription = stringResource(Res.string.social__like),
                    modifier = Modifier.size(ButtonDefaults.ExtraSmallIconSize)
                )
            }
        }
        Text(count.toString(), modifier = Modifier.padding(start = ButtonDefaults.ExtraSmallIconSpacing))
    }

/** The Markdown [text], hidden behind a tap-to-reveal chip when it is marked as a [spoiler]. */
@Composable
fun SpoilerText(
    text: String,
    spoiler: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) = SpoilerContent(text, spoiler, modifier) { MarkdownText(text, style, modifier, maxLines) }

/** [markdown] as written with the rich text editor of comments and reviews. */
@OptIn(ExperimentalRichTextApi::class)
@Composable
fun MarkdownText(markdown: String, style: TextStyle, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    val state = remember(markdown) { RichTextState().setMarkdown(markdown) }
    RichText(state, style = style, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/**
 * [content], hidden behind a tap-to-reveal chip when it is marked as a [spoiler]; [key] is what it
 * shows, so another text starts hidden again. The chip fades into the content as the space grows.
 */
@Composable
fun SpoilerContent(key: String, spoiler: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    var revealed by rememberSaveable(key, spoiler) { mutableStateOf(!spoiler) }
    AnimatedContent(revealed, transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform() }) { shown ->
        if (shown) content() else SpoilerChip(modifier) { revealed = true }
    }
}

@Composable
private fun SpoilerChip(modifier: Modifier, onReveal: () -> Unit) = Surface(
    onClick = onReveal,
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
    shape = MaterialTheme.shapes.small,
    modifier = modifier,
) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.VisibilityOffW500Rounded, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(stringResource(Res.string.social__spoiler_reveal), style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Reports [id] of [target] for moderation. The confirmation replaces the form inside the dialog,
 * rather than a snackbar, since the dialog's scope ends as soon as it closes.
 */
@Composable
fun ReportDialog(target: InteractionTarget, id: String, onDismiss: () -> Unit) {
    val reporter = koinInject<ExceptionReporter>()
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf(ReportReason.SPAM) }
    val note = rememberTextFieldState()
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.report__title)) },
        text = {
            if (sent) {
                Text(stringResource(Res.string.report__sent))
            } else {
                ReportForm(reason, onReasonChange = { reason = it }, note = note)
            }
        },
        confirmButton = {
            if (sent) {
                TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(Res.string.common_ok))
                }
            } else {
                TextButton(
                    enabled = !sending,
                    onClick = {
                        sending = true
                        scope.launch {
                            val result = reporter.safeRequest {
                                Report().apply {
                                    reportableType = target.apiType
                                    reportableId = id
                                    this.reason = reason.apiName
                                    this.note = note.text.toString().trim().take(ReportSchema.MAX_NOTE_LENGTH).ifBlank { null }
                                }.save()
                            }
                            sending = false
                            sent = result.isOk
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                ) { Text(stringResource(Res.string.report__send)) }
            }
        },
        dismissButton = {
            if (!sent) {
                TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        },
    )
}

@Composable
private fun ReportForm(reason: ReportReason, onReasonChange: (ReportReason) -> Unit, note: TextFieldState) =
    Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
        ReportReason.entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(option == reason, role = Role.RadioButton) { onReasonChange(option) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option == reason, onClick = null)
                Text(
                    stringResource(option.label),
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp)
                )
            }
        }
        TextField(
            state = note,
            label = { Text(stringResource(Res.string.report__note)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }

private val ReportReason.label: StringResource
    get() = when (this) {
        ReportReason.SPAM -> Res.string.report__reason_spam
        ReportReason.HARASSMENT -> Res.string.report__reason_harassment
        ReportReason.HATE_SPEECH -> Res.string.report__reason_hate
        ReportReason.SPOILER -> Res.string.report__reason_spoiler
        ReportReason.INAPPROPRIATE -> Res.string.report__reason_inappropriate
        ReportReason.IMPERSONATION -> Res.string.report__reason_impersonation
        ReportReason.OTHER -> Res.string.report__reason_other
    }
