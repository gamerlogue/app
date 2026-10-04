package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import it.maicol07.gamerlogue.ui.theme.Dimens

/**
 * Gap above every detail section. Sections carry it themselves rather than the list using
 * `spacedBy`: a section without data renders nothing, and must not leave an empty gap behind.
 */
internal val SectionSpacing = 24.dp

private val SectionIconContainerSize = 36.dp
private val SectionIconSize = 20.dp
private val ChipIconSize = 18.dp

/** Section title with its icon set in an expressive shape; marked as a heading for screen readers. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SectionHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier) = Row(
    modifier.semantics { heading() },
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    Box(
        Modifier
            .size(SectionIconContainerSize)
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Cookie4Sided.toShape()),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(SectionIconSize)
        )
    }
    Text(title, style = MaterialTheme.typography.titleLargeEmphasized)
}

/** A [SectionHeader] over [content], with the screen padding every detail section shares. */
@Composable
internal fun GameSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) = Column(
    Modifier.padding(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding, top = SectionSpacing),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    SectionHeader(title, icon)
    content()
}

/** Title plus a wrapping row of chips, the shape every taxonomy section uses. */
@Composable
internal fun ChipSection(title: String, icon: ImageVector, content: @Composable () -> Unit) =
    GameSection(title, icon) {
        // No vertical gap: tappable chips already sit in a 48dp touch slot, which spaces the rows.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }

/** A metadata value that opens the games sharing it; same pill as [InfoChip], but a button. */
@Composable
internal fun MetadataChip(label: String, icon: ImageVector?, onClick: () -> Unit) = Surface(
    onClick = onClick,
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = CircleShape
) {
    ChipContent(label, icon)
}

/**
 * A read-only label pill. Not a chip component on purpose: those are buttons, and these do nothing
 * when tapped, so they must not be announced as actionable.
 */
@Composable
internal fun InfoChip(label: String, icon: ImageVector?) = Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = CircleShape,
    // Same row height as a tappable chip's touch slot, so read-only rows space out alike.
    modifier = Modifier.minimumInteractiveComponentSize()
) {
    ChipContent(label, icon)
}

@Composable
private fun ChipContent(label: String, icon: ImageVector?) = Row(
    Modifier.padding(start = if (icon != null) 10.dp else 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(ChipIconSize))
    Text(label, style = MaterialTheme.typography.labelLarge)
}
