package it.maicol07.gamerlogue.ui.views.social

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.michaelbull.result.get
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.nav__notifications
import gamerlogue.sharedui.generated.resources.notifications__comment_reacted
import gamerlogue.sharedui.generated.resources.notifications__comment_replied
import gamerlogue.sharedui.generated.resources.notifications__empty
import gamerlogue.sharedui.generated.resources.notifications__list_commented
import gamerlogue.sharedui.generated.resources.notifications__list_liked
import gamerlogue.sharedui.generated.resources.notifications__mark_all_read
import gamerlogue.sharedui.generated.resources.notifications__new_follower
import gamerlogue.sharedui.generated.resources.notifications__review_commented
import gamerlogue.sharedui.generated.resources.notifications__review_liked
import gamerlogue.sharedui.generated.resources.notifications__unknown
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.NotificationsW500Rounded
import it.maicol07.gamerlogue.data.AppNotification
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.extensions.displayTimestamp
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.layout.LoadMoreEffect
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.layout.loadingItem
import it.maicol07.gamerlogue.ui.components.user.UserAvatar
import it.maicol07.gamerlogue.ui.navigation.ListPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun NotificationsView(viewModel: NotificationsViewModel = koinViewModel()) {
    val navigationState = LocalNavigationState.current
    ScreenScaffold(
        title = Res.string.nav__notifications,
        actions = {
            TextButton(
                onClick = viewModel::markAllRead,
                shapes = ButtonDefaults.shapes(),
            ) { Text(stringResource(Res.string.notifications__mark_all_read)) }
        }
    ) {
        AuthenticatedContent {
            val state by viewModel.notifications.state.collectAsStateWithLifecycle()
            val listState = rememberLazyListState()
            LaunchedEffect(Unit) { viewModel.notifications.refresh() }
            LoadMoreEffect(listState, viewModel.notifications::loadMore)
            if (state.isEmpty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        Icons.NotificationsW500Rounded,
                        stringResource(Res.string.notifications__empty),
                        hint = null,
                        modifier = Modifier.padding(Dimens.ScreenPadding)
                    )
                }
                return@AuthenticatedContent
            }
            Box(Modifier.fillMaxSize()) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(
                        state.items,
                        key = { _, notification -> "notification-${notification.id}" }
                    ) { index, notification ->
                        NotificationRow(notification, index, state.items.size) {
                            viewModel.open(notification, navigationState.backStack::add)
                        }
                    }
                    loadingItem(state.loading)
                }
                AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NotificationRow(notification: AppNotification, index: Int, count: Int, onClick: () -> Unit) {
    val actor = notification.actor()
    val unread = !notification.isRead
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.expressiveSegmentedShapes(index, count),
        colors = ListItemDefaults.expressiveSegmentedColors(
            containerColor = if (unread) MaterialTheme.colorScheme.secondaryContainer else Color.Unspecified
        ),
        modifier = Modifier.fillMaxWidth().padding(
            start = Dimens.ScreenPadding,
            end = Dimens.ScreenPadding,
            bottom = ListItemDefaults.SegmentedGap
        ),
        leadingContent = { UserAvatar(actor, 40.dp) },
        supportingContent = displayTimestamp(notification.createdAt)?.let { { Text(it) } },
    ) {
        Text(notification.describe(actor?.displayName.orEmpty()))
    }
}

@Composable
private fun AppNotification.describe(actor: String): String = when (notificationType) {
    "new_follower" -> stringResource(Res.string.notifications__new_follower, actor)
    "review_liked" -> stringResource(Res.string.notifications__review_liked, actor)
    "review_commented" -> stringResource(Res.string.notifications__review_commented, actor)
    "list_liked" -> stringResource(Res.string.notifications__list_liked, actor)
    "list_commented" -> stringResource(Res.string.notifications__list_commented, actor)
    "comment_replied" -> stringResource(Res.string.notifications__comment_replied, actor)
    "comment_reacted" -> stringResource(Res.string.notifications__comment_reacted, actor, text("reaction").orEmpty())
    else -> stringResource(Res.string.notifications__unknown, actor)
}

internal fun AppNotification.text(key: String): String? = data[key]?.toString()

/** The actor from the data, as a [User] so the avatar and name render like everywhere else. */
private fun AppNotification.actor(): User? {
    val actor = data["actor"] as? Map<*, *> ?: return null
    return User().apply {
        id = actor["id"]?.toString()
        nickname = actor["nickname"]?.toString()
        picture = actor["picture"]?.toString()
    }
}
