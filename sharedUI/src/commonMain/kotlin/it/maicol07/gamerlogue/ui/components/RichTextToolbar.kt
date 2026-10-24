package it.maicol07.gamerlogue.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.comments__format_bold
import gamerlogue.sharedui.generated.resources.comments__format_code
import gamerlogue.sharedui.generated.resources.comments__format_italic
import gamerlogue.sharedui.generated.resources.comments__format_link
import gamerlogue.sharedui.generated.resources.comments__format_list
import gamerlogue.sharedui.generated.resources.comments__format_numbered
import gamerlogue.sharedui.generated.resources.comments__format_strikethrough
import gamerlogue.sharedui.generated.resources.comments__link_add
import gamerlogue.sharedui.generated.resources.comments__link_url
import gamerlogue.sharedui.generated.resources.common_cancel
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CodeW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FormatBoldW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FormatItalicW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FormatListBulletedW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FormatListNumberedW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FormatStrikethroughW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LinkW500Rounded
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A rich text field whose formatting toolbar slides in while it is in use; the text is written as
 * Markdown, like comments.
 */
@Composable
fun RichTextField(
    state: RichTextState,
    label: String,
    supportingText: String?,
    minHeight: Dp,
    modifier: Modifier
) = Column(
    modifier = modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
    verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap / 2)
) {
    var focused by remember { mutableStateOf(false) }
    RichTextEditor(
        state = state,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).onFocusChanged { focused = it.isFocused },
    )
    AnimatedVisibility(
        focused,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(),
    ) { RichTextFormatToolbar(state) }
}

/**
 * The formatting buttons, checked when the selection already has the style. They never take the
 * focus, which would collapse the selection they are meant to format.
 */
@Composable
fun RichTextFormatToolbar(text: RichTextState) {
    var linkOpen by remember { mutableStateOf(false) }
    val style = text.currentSpanStyle
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap / 2)
    ) {
        FormatButton(
            Icons.FormatBoldW500Rounded,
            Res.string.comments__format_bold,
            style.fontWeight == FontWeight.Bold
        ) { text.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
        FormatButton(
            Icons.FormatItalicW500Rounded,
            Res.string.comments__format_italic,
            style.fontStyle == FontStyle.Italic
        ) { text.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
        FormatButton(
            Icons.FormatStrikethroughW500Rounded,
            Res.string.comments__format_strikethrough,
            style.textDecoration?.contains(TextDecoration.LineThrough) == true
        ) { text.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
        FormatButton(Icons.CodeW500Rounded, Res.string.comments__format_code, text.isCodeSpan) { text.toggleCodeSpan() }
        FormatButton(Icons.LinkW500Rounded, Res.string.comments__format_link, text.isLink) {
            if (text.isLink) text.removeLink() else linkOpen = true
        }
        FormatButton(Icons.FormatListBulletedW500Rounded, Res.string.comments__format_list, text.isUnorderedList) {
            text.toggleUnorderedList()
        }
        FormatButton(Icons.FormatListNumberedW500Rounded, Res.string.comments__format_numbered, text.isOrderedList) {
            text.toggleOrderedList()
        }
    }
    if (linkOpen) LinkDialog(text, onDismiss = { linkOpen = false })
}

@Composable
private fun FormatButton(icon: ImageVector, label: StringResource, checked: Boolean, onClick: () -> Unit) =
    IconToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        shapes = IconButtonDefaults.toggleableShapes(),
        colors = IconButtonDefaults.iconToggleButtonColors(
            checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.focusProperties { canFocus = false },
    ) { Icon(icon, contentDescription = stringResource(label)) }

/** Links the selected text to an address, or inserts the address as a link when nothing is selected. */
@Composable
private fun LinkDialog(text: RichTextState, onDismiss: () -> Unit) {
    val url = rememberTextFieldState("https://")
    val address = url.text.toString().trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.comments__format_link)) },
        text = {
            TextField(
                state = url,
                label = { Text(stringResource(Res.string.comments__link_url)) },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = address.length > "https://".length,
                onClick = {
                    if (text.selection.collapsed) text.addLink(address, address) else text.addLinkToSelection(address)
                    onDismiss()
                },
                shapes = ButtonDefaults.shapes(),
            ) { Text(stringResource(Res.string.comments__link_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.common_cancel))
            }
        },
    )
}
