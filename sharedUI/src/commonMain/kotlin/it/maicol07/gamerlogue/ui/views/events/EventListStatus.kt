package it.maicol07.gamerlogue.ui.views.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.events__error
import gamerlogue.sharedui.generated.resources.events__retry
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CelebrationW500Rounded
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val StatusIconSize = 96.dp
private val StatusGlyphSize = 48.dp

/** Full-screen state for an empty list or a failed first load, the latter with its retry. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EventsStatus(text: StringResource, modifier: Modifier, onRetry: (() -> Unit)?) = Column(
    modifier = modifier.padding(Dimens.ScreenPadding),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box(
        modifier = Modifier
            .size(StatusIconSize)
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie9Sided.toShape()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.CelebrationW500Rounded,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(StatusGlyphSize),
        )
    }
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    onRetry?.let { RetryButton(it) }
}

/** A failed later page, as the last row of the list. */
@Composable
internal fun PageError(modifier: Modifier, onRetry: () -> Unit) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
) {
    Text(
        text = stringResource(Res.string.events__error),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f),
    )
    RetryButton(onRetry)
}

@Composable
private fun RetryButton(onRetry: () -> Unit) = FilledTonalButton(onClick = onRetry, shapes = ButtonDefaults.shapes()) {
    Text(stringResource(Res.string.events__retry))
}
