package it.maicol07.gamerlogue.ui.views.comments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import com.github.michaelbull.result.get
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.activity__unknown_game
import gamerlogue.sharedui.generated.resources.profile__report
import gamerlogue.sharedui.generated.resources.review__not_available
import gamerlogue.sharedui.generated.resources.review__title
import io.github.fopwoc.nav3ksp.annotation.Branch
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FlagW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RateReviewW500Rounded
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.ApiErrorCodes
import it.maicol07.gamerlogue.data.IgdbLookup
import it.maicol07.gamerlogue.data.InteractionTarget
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.LikeState
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.extensions.displayTimestamp
import it.maicol07.gamerlogue.extensions.fetchPage
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.extensions.userReviews
import it.maicol07.gamerlogue.ui.components.EmptyState
import it.maicol07.gamerlogue.ui.components.currentRatingScale
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.components.layout.LoadMoreEffect
import it.maicol07.gamerlogue.ui.components.layout.ScreenScaffold
import it.maicol07.gamerlogue.ui.components.social.LikeButton
import it.maicol07.gamerlogue.ui.components.social.ReportDialog
import it.maicol07.gamerlogue.ui.components.social.SpoilerText
import it.maicol07.gamerlogue.ui.components.user.UserAvatar
import it.maicol07.gamerlogue.ui.navigation.DetailPaneMetadata
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.RootTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.auth.AuthenticatedContent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

/** A guard on the search for another user's review. */
private const val MAX_REVIEW_PAGES = 20

private val CoverWidth = 80.dp
private val CoverHeight = 107.dp

/** One review with its likes; [userId] owns it, null for the signed-in user's own. */
@KoinViewModel
class ReviewDetailViewModel(
    @InjectedParam private val entryId: String,
    @InjectedParam private val userId: String?,
) : StateViewModel<ReviewDetailViewModel.UiState>(UiState()) {
    data class UiState(
        val entry: LibraryEntry? = null,
        val game: Game? = null,
        val loading: Boolean = true,
        val like: LikeState = LikeState(liked = false, count = 0),
        val liking: Boolean = false,
    )

    private val lookup by inject<IgdbLookup>()
    private val api by inject<ProfileApi>()
    private val authProvider by inject<AuthTokenProvider>()

    val currentUserId: String? get() = authProvider.session.value.userId

    /** Who wrote the review: [userId], or the signed-in user for their own. */
    val ownerId: String? get() = userId ?: currentUserId

    fun load() = viewModelScope.launch {
        val entry = safeRequest(ApiErrorCodes.HIDDEN_CONTENT) {
            if (userId == null || userId == currentUserId) ownEntry() else othersReview(userId)
        }.get()?.takeIf { it.hasReview }
        val game = entry?.let { safeRequest { lookup.games(listOf(it.gameId)) }.get()?.get(it.gameId) }
        update {
            copy(
                entry = entry,
                game = game,
                loading = false,
                like = LikeState(entry?.likedByMe == true, entry?.likesCount ?: 0)
            )
        }
    }

    private suspend fun ownEntry(): LibraryEntry = LibraryEntry.includes("user").find(entryId).data

    /**
     * Another user's review. Entries can only be read by id by their owner, so it is looked up among
     * the user's reviews, newest first: one opened from a feed or a notification is usually recent.
     */
    private suspend fun othersReview(owner: String): LibraryEntry? {
        var page = 0
        var loaded = 0
        var found: LibraryEntry? = null
        var last = false
        while (found == null && !last && page < MAX_REVIEW_PAGES) {
            page++
            val reviews = LibraryEntry.userReviews(owner).includes("user").fetchPage(page)
            found = reviews.items.find { it.id == entryId }
            loaded += reviews.items.size
            last = reviews.isLast(loaded)
        }
        return found
    }

    fun toggleLike() = viewModelScope.launch {
        update { copy(liking = true) }
        val like = safeRequest { api.toggleLike(InteractionTarget.REVIEW, entryId, state.like) }.get()
        update { copy(liking = false, like = like ?: this.like) }
    }
}

@Branch(RootTree::class, metadata = DetailPaneMetadata::class)
@Composable
fun ReviewDetailView(entryId: String, userId: String?) = ScreenScaffold(title = Res.string.review__title) {
    AuthenticatedContent {
        val navigationState = LocalNavigationState.current
        val viewModel = koinViewModel<ReviewDetailViewModel> { parametersOf(entryId, userId) }
        val commentsViewModel = koinViewModel<CommentsViewModel> { parametersOf(InteractionTarget.REVIEW, entryId) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val roots by commentsViewModel.roots.state.collectAsStateWithLifecycle()
        val commentsState by commentsViewModel.uiState.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { viewModel.load() }
        val entry = uiState.entry
        when {
            uiState.loading -> ReviewLoading()
            entry == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(Icons.RateReviewW500Rounded, stringResource(Res.string.review__not_available), hint = null)
            }
            else -> Column(Modifier.fillMaxSize()) {
                val listState = rememberLazyListState()
                LaunchedEffect(Unit) { commentsViewModel.roots.ensureLoaded() }
                LoadMoreEffect(listState, commentsViewModel.roots::loadMore)
                LazyColumn(state = listState, modifier = Modifier.weight(1f)) {
                    item(key = "review") {
                        ReviewContent(
                            entry = entry,
                            game = uiState.game,
                            like = uiState.like,
                            liking = uiState.liking,
                            isMine = viewModel.ownerId == viewModel.currentUserId,
                            onLike = viewModel::toggleLike,
                            onGameClick = { uiState.game?.let { navigationState.backStack.add(it.detailNavKey) } },
                            onUserClick = {
                                viewModel.ownerId?.let {
                                    navigationState.backStack.add(
                                        RootNavTree.UserProfile(it)
                                    )
                                }
                            },
                        )
                    }
                    commentsSection(roots, commentsState, commentsViewModel, viewModel.ownerId) {
                        navigationState.backStack.add(RootNavTree.UserProfile(it))
                    }
                }
                CommentComposer(commentsState, commentsViewModel)
            }
        }
    }
}

@Composable
private fun ReviewContent(
    entry: LibraryEntry,
    game: Game?,
    like: LikeState,
    liking: Boolean,
    isMine: Boolean,
    onLike: () -> Unit,
    onGameClick: () -> Unit,
    onUserClick: () -> Unit,
) {
    var reportOpen by remember { mutableStateOf(false) }
    val scale = currentRatingScale()
    Column(
        Modifier.fillMaxWidth().padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
            game?.CoverImage(
                Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onGameClick),
                sizeModifier = Modifier.width(CoverWidth).height(CoverHeight)
            )
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
                Text(
                    game?.name ?: stringResource(Res.string.activity__unknown_game),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier = Modifier.clickable(onClick = onGameClick)
                )
                entry.rating?.let { Text("★ ${scale.format(it)}", style = MaterialTheme.typography.titleMedium) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
                    modifier = Modifier.clickable(onClick = onUserClick)
                ) {
                    UserAvatar(entry.user, 24.dp)
                    Text(entry.user?.displayName.orEmpty(), style = MaterialTheme.typography.labelLarge)
                    displayTimestamp(entry.reviewUpdatedAt)?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        SpoilerText(entry.review.orEmpty(), entry.reviewSpoiler, MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LikeButton(like.liked, like.count, loading = liking, onClick = onLike)
            if (!isMine) {
                IconButton(onClick = { reportOpen = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.FlagW500Rounded, contentDescription = stringResource(Res.string.profile__report))
                }
            }
        }
    }
    if (reportOpen) entry.id?.let { ReportDialog(InteractionTarget.REVIEW, it, onDismiss = { reportOpen = false }) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReviewLoading() = Box(
    Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
) { ContainedLoadingIndicator() }
