package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.lists__count_games
import gamerlogue.sharedui.generated.resources.lists__empty
import gamerlogue.sharedui.generated.resources.lists__new
import gamerlogue.sharedui.generated.resources.profile__activity_empty
import gamerlogue.sharedui.generated.resources.profile__reviews_empty
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AddW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FormatListBulletedW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.HistoryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LockW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RateReviewW500Rounded
import it.maicol07.gamerlogue.core.PagedState
import it.maicol07.gamerlogue.data.Activity
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.UserList
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.ui.components.layout.loadingItem
import it.maicol07.gamerlogue.ui.components.social.ReviewCard
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.social.activityItems
import it.maicol07.gamerlogue.ui.views.userlist.UserListForm
import it.maicol07.gamerlogue.ui.views.userlist.UserListFormDialog
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.activityTab(state: PagedState<Activity>, games: Map<Int, Game>, onNavigate: (NavKey) -> Unit) {
    activityItems(state, games, onNavigate)
    if (state.isEmpty) item { ProfileMessage(Icons.HistoryW500Rounded, Res.string.profile__activity_empty, null) }
}

/** [userId]'s reviews, each opening its page. */
internal fun LazyListScope.reviewsTab(
    userId: String,
    state: PagedState<LibraryEntry>,
    games: Map<Int, Game>,
    onNavigate: (NavKey) -> Unit,
) {
    itemsIndexed(state.items, key = { _, entry -> "review-${entry.id}" }) { _, entry ->
        ReviewCard(
            entry,
            games[entry.gameId],
            onClick = { onNavigate(RootNavTree.ReviewDetail(entry.id.orEmpty(), userId)) },
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemGap / 2)
        )
    }
    loadingItem(state.loading)
    if (state.isEmpty) item { ProfileMessage(Icons.RateReviewW500Rounded, Res.string.profile__reviews_empty, null) }
}

internal fun LazyListScope.listsTab(
    state: PagedState<UserList>,
    isSelf: Boolean,
    onCreate: (UserListForm) -> Unit,
    onNavigate: (NavKey) -> Unit,
) {
    if (isSelf) item(key = "new-list") { NewListButton(onCreate) }
    itemsIndexed(state.items, key = { _, list -> "list-${list.id}" }) { index, list ->
        UserListRow(list, index, state.items.size) { onNavigate(RootNavTree.UserListDetail(list.id.orEmpty())) }
    }
    loadingItem(state.loading)
    if (state.isEmpty && !isSelf) {
        item {
            ProfileMessage(
                Icons.FormatListBulletedW500Rounded,
                Res.string.lists__empty,
                null
            )
        }
    }
}

@Composable
private fun NewListButton(onCreate: (UserListForm) -> Unit) {
    var dialogOpen by remember { mutableStateOf(false) }
    FilledTonalButton(
        onClick = { dialogOpen = true },
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemGap)
    ) {
        Icon(Icons.AddW500Rounded, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Text(stringResource(Res.string.lists__new), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
    }
    if (dialogOpen) {
        UserListFormDialog(initial = null, onSave = onCreate, onDelete = null, onDismiss = { dialogOpen = false })
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun UserListRow(list: UserList, index: Int, count: Int, onClick: () -> Unit) = SegmentedListItem(
    onClick = onClick,
    shapes = ListItemDefaults.expressiveSegmentedShapes(index, count),
    colors = ListItemDefaults.expressiveSegmentedColors(),
    modifier = Modifier.padding(
        start = Dimens.ScreenPadding,
        end = Dimens.ScreenPadding,
        bottom = ListItemDefaults.SegmentedGap
    ),
    leadingContent = { Icon(Icons.FormatListBulletedW500Rounded, contentDescription = null) },
    supportingContent = {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
            Text(pluralStringResource(Res.plurals.lists__count_games, list.itemsCount, list.itemsCount))
            if (list.isPrivate) Icon(Icons.LockW500Rounded, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    },
) { Text(list.name) }
