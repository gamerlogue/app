package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import at.released.igdbclient.model.Platform
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.profile__library_empty
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Inventory2W500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val PlatformIconSize = 18.dp

/** Another user's library: the entries per status, each opening their games in that status. */
internal fun LazyListScope.libraryTab(counts: Map<String, Int>, userId: String, onNavigate: (NavKey) -> Unit) {
    if (counts.isEmpty()) {
        item(key = "library-empty") {
            ProfileMessage(Icons.Inventory2W500Rounded, Res.string.profile__library_empty, null)
        }
        return
    }
    item(key = "library") {
        StatusRows(counts, Modifier.padding(Dimens.ScreenPadding)) { status ->
            onNavigate(RootNavTree.GameList(null, null, null, null, status, userId, null))
        }
    }
}

/** A titled block of a tab. */
@Composable
internal fun ProfileSection(title: StringResource, content: @Composable () -> Unit) = Column(
    modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.ItemGap),
    verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMediumEmphasized,
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
    )
    content()
}

@Composable
internal fun PlatformChips(platforms: List<Platform>, modifier: Modifier = Modifier) = FlowRow(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
    verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
) {
    platforms.forEach { platform ->
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = CircleShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                platform.Image(Modifier.size(PlatformIconSize))
                val label = platform.abbreviation.takeIf { it.isNotBlank() } ?: platform.name
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusRows(counts: Map<String, Int>, modifier: Modifier, onClick: (GameLibraryStatus) -> Unit) = Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
) {
    val statuses = GameLibraryStatus.entries
    statuses.forEachIndexed { index, status ->
        SegmentedListItem(
            onClick = { onClick(status) },
            shapes = ListItemDefaults.expressiveSegmentedShapes(index, statuses.size),
            colors = ListItemDefaults.expressiveSegmentedColors(),
            leadingContent = { Icon(status.icon, contentDescription = null) },
            trailingContent = {
                Text((counts[status.name] ?: 0).toString(), style = MaterialTheme.typography.titleMediumEmphasized)
            },
        ) { Text(stringResource(status.displayName)) }
    }
}
