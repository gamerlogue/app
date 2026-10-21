package it.maicol07.gamerlogue.ui.views.userlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.lists__actions
import gamerlogue.sharedui.generated.resources.lists__add_comment
import gamerlogue.sharedui.generated.resources.lists__add_games
import gamerlogue.sharedui.generated.resources.lists__count_games
import gamerlogue.sharedui.generated.resources.lists__edit
import gamerlogue.sharedui.generated.resources.lists__move_down
import gamerlogue.sharedui.generated.resources.lists__move_up
import gamerlogue.sharedui.generated.resources.lists__no_games
import gamerlogue.sharedui.generated.resources.lists__not_available
import gamerlogue.sharedui.generated.resources.profile__report
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AddW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ChatBubbleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.EditW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FlagW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FormatListBulletedW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LockW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlaylistAddW500Rounded
import it.maicol07.gamerlogue.core.PagedState
import it.maicol07.gamerlogue.data.Comment
import it.maicol07.gamerlogue.data.InteractionTarget
import it.maicol07.gamerlogue.data.UserList
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.layout.AppTopBar
import it.maicol07.gamerlogue.ui.components.layout.LoadMoreEffect
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.social.LikeButton
import it.maicol07.gamerlogue.ui.components.social.ReportDialog
import it.maicol07.gamerlogue.ui.components.user.UserAvatar
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import it.maicol07.gamerlogue.ui.views.comments.CommentComposer
import it.maicol07.gamerlogue.ui.views.comments.CommentsViewModel
import it.maicol07.gamerlogue.ui.views.comments.commentsSection
import it.maicol07.gamerlogue.ui.views.list.GamePick
import it.maicol07.gamerlogue.ui.views.list.GamePickEffect
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

/** The menu button's progress past which its plus has turned into a cross. */
private const val HalfOpen = 0.5f

/** Room under the last comment, so the floating button never covers it. */
private val FabClearance = 96.dp

/** The lazy key prefix of the item rows, telling them apart from the header and the comments. */
private const val ItemKeyPrefix = "item-"

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun UserListDetailView(listId: String) {
    val navigationState = LocalNavigationState.current
    val viewModel = koinViewModel<UserListDetailViewModel> { parametersOf(listId) }
    val commentsViewModel = koinViewModel<CommentsViewModel> { parametersOf(InteractionTarget.LIST, listId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val roots by commentsViewModel.roots.state.collectAsStateWithLifecycle()
    val commentsState by commentsViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    GamePickEffect(addGamesRequest(listId), viewModel::addGames)
    val list = uiState.list
    ScreenScaffold(
        topBar = {
            AppTopBar(
                title = list?.name,
                subtitle = list?.let {
                    pluralStringResource(Res.plurals.lists__count_games, uiState.items.size, uiState.items.size)
                },
                actions = {
                    if (list != null) {
                        ListAction(
                            list = list,
                            isOwner = uiState.isOwner,
                            busy = uiState.savingList || uiState.deletingList,
                            onEdit = viewModel::edit,
                            onDelete = { viewModel.delete(onDeleted = navigationState::navigateBack) },
                        )
                    }
                }
            )
        }
    ) {
        AuthenticatedContent {
            when {
                uiState.loading -> ListLoading()
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(Icons.LockW500Rounded, stringResource(Res.string.lists__not_available), hint = null)
                }
                else -> ListContent(
                    list = list,
                    state = uiState,
                    viewModel = viewModel,
                    commentsViewModel = commentsViewModel,
                    roots = roots,
                    commentsState = commentsState,
                )
            }
        }
    }
}

/**
 * The list, its games and comments, under a bar that waves while the list itself is being changed.
 * The composer rises only while a comment is written; the floating button starts one (and, for the
 * owner, adding games) and steps aside meanwhile.
 */
@Composable
private fun ListContent(
    list: UserList,
    state: UserListDetailViewModel.UiState,
    viewModel: UserListDetailViewModel,
    commentsViewModel: CommentsViewModel,
    roots: PagedState<Comment>,
    commentsState: CommentsViewModel.UiState,
) = Box(Modifier.fillMaxSize()) {
    val navigationState = LocalNavigationState.current
    Column(Modifier.fillMaxSize()) {
        BusyBar(state.busy)
        val listState = rememberLazyListState()
        // Only item rows move: the header and the comments around them are not drop targets.
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            val fromId = itemId(from.key)
            val toId = itemId(to.key)
            if (fromId != null && toId != null) viewModel.reorder(fromId, toId)
        }
        LaunchedEffect(Unit) { commentsViewModel.roots.ensureLoaded() }
        LoadMoreEffect(listState, commentsViewModel.roots::loadMore)
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = FabClearance),
            modifier = Modifier.weight(1f)
        ) {
            item(key = "list-header") {
                ListHeader(list, state, onLike = viewModel::toggleLike) {
                    list.userId?.let { navigationState.backStack.add(RootNavTree.UserProfile(it)) }
                }
            }
            if (state.items.isEmpty()) {
                item(key = "list-empty") {
                    EmptyState(
                        Icons.FormatListBulletedW500Rounded,
                        stringResource(Res.string.lists__no_games),
                        hint = null,
                        modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding)
                    )
                }
            }
            listItems(list, state, viewModel, reorderState) { game -> navigationState.backStack.add(game.detailNavKey) }
            commentsSection(roots, commentsState, commentsViewModel, list.userId) {
                navigationState.backStack.add(RootNavTree.UserProfile(it))
            }
        }
        AnimatedVisibility(
            commentsState.composing,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) { CommentComposer(commentsState, commentsViewModel) }
    }
    AnimatedVisibility(
        !commentsState.composing,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding(),
    ) {
        ListFab(
            isOwner = state.isOwner,
            onAddGames = {
                val pick = GamePick(addGamesRequest(list.id.orEmpty()), multiple = true)
                navigationState.backStack.add(RootNavTree.GameList(null, null, null, null, null, null, pick))
            },
            onComment = commentsViewModel::startNew,
        )
    }
}

/** A wavy bar across the top while a change to the whole list is sent. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BusyBar(busy: Boolean) = AnimatedVisibility(busy, enter = expandVertically(), exit = shrinkVertically()) {
    LinearWavyProgressIndicator(Modifier.fillMaxWidth())
}

/**
 * The owner's floating menu, adding games or a comment, or a single button to comment for anyone
 * else. The menu button turns from a plus into a cross as the menu opens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListFab(isOwner: Boolean, onAddGames: () -> Unit, onComment: () -> Unit) {
    if (!isOwner) {
        ExtendedFloatingActionButton(
            onClick = onComment,
            icon = { Icon(Icons.ChatBubbleW500Rounded, contentDescription = null) },
            text = { Text(stringResource(Res.string.lists__add_comment)) },
            modifier = Modifier.padding(Dimens.ScreenPadding),
        )
        return
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            val label = stringResource(Res.string.lists__actions)
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { expanded = it },
                modifier = Modifier.semantics { contentDescription = label },
            ) {
                val progress = { checkedProgress }
                Icon(
                    if (checkedProgress > HalfOpen) Icons.CloseW500Rounded else Icons.AddW500Rounded,
                    contentDescription = null,
                    modifier = with(ToggleFloatingActionButtonDefaults) { Modifier.animateIcon(progress) },
                )
            }
        },
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onAddGames()
            },
            text = { Text(stringResource(Res.string.lists__add_games)) },
            icon = { Icon(Icons.PlaylistAddW500Rounded, contentDescription = null) },
        )
        FloatingActionButtonMenuItem(
            onClick = {
                expanded = false
                onComment()
            },
            text = { Text(stringResource(Res.string.lists__add_comment)) },
            icon = { Icon(Icons.ChatBubbleW500Rounded, contentDescription = null) },
        )
    }
}

/** Who made the list, with the like on the other side, and the description below. */
@Composable
private fun ListHeader(
    list: UserList,
    state: UserListDetailViewModel.UiState,
    onLike: () -> Unit,
    onUserClick: () -> Unit,
) = Column(
    modifier = Modifier.fillMaxWidth().padding(Dimens.ScreenPadding),
    verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            modifier = Modifier.weight(1f).clickable(onClick = onUserClick)
        ) {
            UserAvatar(list.user, 24.dp)
            Text(list.user?.displayName.orEmpty(), style = MaterialTheme.typography.labelLarge)
            if (list.isPrivate) Icon(Icons.LockW500Rounded, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        LikeButton(state.like.liked, state.like.count, loading = state.liking, onClick = onLike)
    }
    list.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
}

/** The [GamePick] request under which the game list hands back the games to add to [listId]. */
private fun addGamesRequest(listId: String) = "list-$listId"

/** The game rows, which the owner reorders by dragging. */
private fun LazyListScope.listItems(
    list: UserList,
    state: UserListDetailViewModel.UiState,
    viewModel: UserListDetailViewModel,
    reorderState: ReorderableLazyListState,
    onGameClick: (Game) -> Unit,
) = itemsIndexed(state.items, key = { _, item -> ItemKeyPrefix + item.id }) { index, item ->
    ReorderableItem(reorderState, key = ItemKeyPrefix + item.id, enabled = state.isOwner) { dragging ->
        ListItemRow(
            item = item,
            rank = (index + 1).takeIf { list.isRanked },
            game = state.games[item.gameId],
            shapes = ListItemDefaults.expressiveSegmentedShapes(index, state.items.size),
            owner = if (state.isOwner) {
                OwnerControls(
                    handle = Modifier.draggableHandle(onDragStopped = { viewModel.saveOrder() }),
                    dragging = dragging,
                    moves = listOfNotNull(
                        (Res.string.lists__move_up to { viewModel.move(index, -1) }).takeIf { index > 0 },
                        (Res.string.lists__move_down to { viewModel.move(index, 1) })
                            .takeIf { index < state.items.lastIndex },
                    ),
                    saving = item.id in state.savingItems,
                    onRemove = { viewModel.remove(item) },
                    onEditNote = { viewModel.editNote(item, it) },
                )
            } else {
                null
            },
            onClick = { state.games[item.gameId]?.let(onGameClick) },
        )
    }
}

/** The id of the item behind a lazy [key]; null for the header and the comments. */
private fun itemId(key: Any): String? =
    (key as? String)?.takeIf { it.startsWith(ItemKeyPrefix) }?.removePrefix(ItemKeyPrefix)

/**
 * The bar's only action: editing for the owner, who deletes from the edit dialog, with an indicator
 * while either is sent ([busy]); reporting for anyone else.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListAction(
    list: UserList,
    isOwner: Boolean,
    busy: Boolean,
    onEdit: (UserListForm) -> Unit,
    onDelete: () -> Unit,
) {
    var editOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    if (isOwner) {
        IconButton(onClick = { editOpen = true }, enabled = !busy, shapes = IconButtonDefaults.shapes()) {
            AnimatedContent(busy, transitionSpec = { fadeIn() togetherWith fadeOut() }) {
                if (it) {
                    LoadingIndicator(Modifier.size(24.dp))
                } else {
                    Icon(Icons.EditW500Rounded, contentDescription = stringResource(Res.string.lists__edit))
                }
            }
        }
    } else {
        IconButton(onClick = { reportOpen = true }, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.FlagW500Rounded, contentDescription = stringResource(Res.string.profile__report))
        }
    }
    if (editOpen) {
        UserListFormDialog(initial = list, onSave = onEdit, onDelete = onDelete, onDismiss = { editOpen = false })
    }
    if (reportOpen) list.id?.let { ReportDialog(InteractionTarget.LIST, it, onDismiss = { reportOpen = false }) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ListLoading() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    ContainedLoadingIndicator()
}
