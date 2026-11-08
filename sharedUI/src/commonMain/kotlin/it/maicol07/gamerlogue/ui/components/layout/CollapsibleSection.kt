package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.KeyboardArrowDownW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes

private const val HALF_TURN_DEGREES = 180f

/**
 * One collapsible section: a header segment that expands/collapses it, with its rows segmented below it; no-op
 * when there are no rows. [header] draws the header with the shapes and item modifier it is given; [row] draws a
 * row at its segment position (the header is position 0) out of the section's segment count.
 */
internal fun <T> LazyListScope.collapsibleSection(
    key: String,
    rows: List<T>,
    rowKey: (T) -> Any,
    expanded: Boolean,
    header: @Composable (shapes: ListItemShapes, modifier: Modifier) -> Unit,
    row: @Composable (item: T, position: Int, count: Int) -> Unit,
) {
    if (rows.isEmpty()) return
    val count = if (expanded) rows.size + 1 else 1
    item(key = key) {
        header(ListItemDefaults.segmentedShapes(index = 0, count = count), Modifier.animateItem().padding(top = 8.dp))
    }
    if (!expanded) return
    itemsIndexed(rows, key = { _, item -> rowKey(item) }) { i, item ->
        Box(
            Modifier.animateItem(
                fadeInSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
                placementSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                fadeOutSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            ),
        ) { row(item, i + 1, count) }
    }
}

/** A collapsible section's header segment: optional icon, title and supporting text, with the [ExpandArrow]. */
@Composable
internal fun CollapsibleSectionHeader(
    title: String,
    icon: ImageVector?,
    supporting: String?,
    expanded: Boolean,
    shapes: ListItemShapes,
    onToggle: () -> Unit,
    modifier: Modifier,
) {
    SegmentedListItem(
        onClick = onToggle,
        shapes = shapes,
        colors = ListItemDefaults.expressiveSegmentedColors(),
        modifier = modifier,
        leadingContent = icon?.let { { Icon(it, contentDescription = null) } },
        supportingContent = supporting?.let { { Text(it) } },
        trailingContent = { ExpandArrow(expanded, Modifier) },
    ) { Text(title, style = MaterialTheme.typography.titleMedium) }
}

/** A section's arrow: down when collapsed, turning up as it expands. */
@Composable
internal fun ExpandArrow(expanded: Boolean, modifier: Modifier) {
    val arrowTurn by animateFloatAsState(
        if (expanded) HALF_TURN_DEGREES else 0f,
        MaterialTheme.motionScheme.defaultSpatialSpec(),
    )
    Icon(
        Icons.KeyboardArrowDownW500Rounded,
        contentDescription = null,
        modifier = modifier.graphicsLayer { rotationZ = arrowTurn },
    )
}

/**
 * A collapsible group's header shapes: a lone segment, whose bottom corners morph into the top segment's ones as
 * the group [open]s. The instance never changes, since M3 rebuilds a list item's ripple when its shapes change:
 * swapping them on the toggling tap would cancel that tap's ripple.
 */
@Composable
internal fun rememberCollapsibleHeaderShapes(open: Boolean): ListItemShapes {
    val single = ListItemDefaults.expressiveSegmentedShapes(0, 1)
    val top = ListItemDefaults.segmentedShapes(0, 2)
    val progress = animateFloatAsState(if (open) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec())
    return remember(single, top) {
        val closed = (single.shape as CornerBasedShape).bottomStart
        val joined = (top.shape as CornerBasedShape).bottomStart
        // Read at draw time, so the corners follow the animation without recomposing or replacing the shape.
        val bottom = object : CornerSize {
            override fun toPx(shapeSize: Size, density: Density): Float =
                lerp(closed.toPx(shapeSize, density), joined.toPx(shapeSize, density), progress.value)
        }
        top.copy(shape = (top.shape as RoundedCornerShape).copy(bottomStart = bottom, bottomEnd = bottom))
    }
}
