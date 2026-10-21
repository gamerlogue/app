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
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.profile__blocked_users
import gamerlogue.sharedui.generated.resources.profile__blocked_users_empty
import gamerlogue.sharedui.generated.resources.profile__followers
import gamerlogue.sharedui.generated.resources.profile__following
import gamerlogue.sharedui.generated.resources.profile__unblock
import gamerlogue.sharedui.generated.resources.social__no_users
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.BlockW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.GroupW500Rounded
import it.maicol07.gamerlogue.core.PageLoader
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.layout.AppVerticalScrollbar
import it.maicol07.gamerlogue.ui.components.layout.LoadMoreEffect
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.layout.loadingItem
import it.maicol07.gamerlogue.ui.components.user.UserRow
import it.maicol07.gamerlogue.ui.navigation.ListPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun UserConnectionsView(userId: String, kind: ConnectionsKind) {
    val viewModel = koinViewModel<ConnectionsViewModel> { parametersOf(userId, kind) }
    ScreenScaffold(
        title = when (kind) {
            ConnectionsKind.FOLLOWERS -> Res.string.profile__followers
            ConnectionsKind.FOLLOWING -> Res.string.profile__following
        }
    ) {
        UserPagedList(viewModel.users, Icons.GroupW500Rounded, stringResource(Res.string.social__no_users))
    }
}

@Branch(RootTree::class, metadata = ListPaneMetadata::class)
@Composable
fun BlockedUsersView(
    viewModel: BlockedUsersViewModel = koinViewModel()
) = ScreenScaffold(title = Res.string.profile__blocked_users) {
    AuthenticatedContent {
        UserPagedList(
            viewModel.users,
            Icons.BlockW500Rounded,
            stringResource(Res.string.profile__blocked_users_empty)
        ) { user ->
            TextButton(onClick = { viewModel.unblock(user) }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.profile__unblock))
            }
        }
    }
}

/** A page-by-page list of users opening their profiles, with an optional [trailing] action per user. */
@Composable
internal fun UserPagedList(
    loader: PageLoader<User>,
    emptyIcon: ImageVector,
    emptyText: String,
    loadOnStart: Boolean = true,
    trailing: (@Composable (User) -> Unit)? = null,
) {
    val navigationState = LocalNavigationState.current
    val state by loader.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    if (loadOnStart) LaunchedEffect(Unit) { loader.ensureLoaded() }
    LoadMoreEffect(listState, loader::loadMore)
    if (state.isEmpty) {
        CenteredEmptyState(emptyIcon, emptyText)
        return
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(state.items, key = { _, user -> "user-${user.id}" }) { index, user ->
                UserRow(
                    user = user,
                    shapes = ListItemDefaults.expressiveSegmentedShapes(index, state.items.size),
                    onClick = { user.id?.let { navigationState.backStack.add(RootNavTree.UserProfile(it)) } },
                    modifier = Modifier.padding(
                        start = Dimens.ScreenPadding,
                        end = Dimens.ScreenPadding,
                        bottom = ListItemDefaults.SegmentedGap
                    ),
                    trailing = { trailing?.invoke(user) },
                )
            }
            loadingItem(state.loading)
        }
        AppVerticalScrollbar(listState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
internal fun CenteredEmptyState(
    icon: ImageVector,
    text: String
) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    EmptyState(icon, text, hint = null, modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding))
}
