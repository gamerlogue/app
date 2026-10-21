package it.maicol07.gamerlogue.ui.views.comments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.reactions__add
import gamerlogue.sharedui.generated.resources.reactions__rejected
import gamerlogue.sharedui.generated.resources.social__like
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AddReactionW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.FavoriteW500Rounded
import it.maicol07.gamerlogue.data.Comment
import it.maicol07.gamerlogue.data.ReactionCount
import it.maicol07.gamerlogue.data.ReactionSchema
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

private val ReactingIndicatorSize = 24.dp

/**
 * A comment's reactions: one chip per type with its count (the like always shown), selected when it
 * is the current user's, then a button to react with any emoji. Chips come and go, and counts roll,
 * with the row resizing smoothly around them.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CommentReactionsRow(comment: Comment, state: CommentsViewModel.UiState, viewModel: CommentsViewModel) {
    val reactions = state.reactionsOf(comment)
    var pickerOpen by remember { mutableStateOf(false) }
    val chips = if (reactions.summary.any { it.type == ReactionSchema.LIKE }) {
        reactions.summary
    } else {
        listOf(ReactionCount(ReactionSchema.LIKE, 0)) + reactions.summary
    }
    FlowRow(
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { reaction ->
            val mine = reaction.type == reactions.mine
            FilterChip(
                selected = mine,
                onClick = { viewModel.react(comment, if (mine) null else reaction.type) },
                label = { ReactionLabel(reaction) },
            )
        }
        IconButton(onClick = { pickerOpen = true }, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.AddReactionW500Rounded, contentDescription = stringResource(Res.string.reactions__add))
        }
        AnimatedVisibility(comment.id in state.reacting) { LoadingIndicator(Modifier.size(ReactingIndicatorSize)) }
    }
    if (state.rejectedReaction == comment.id) {
        Text(
            stringResource(Res.string.reactions__rejected),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    if (pickerOpen) EmojiPickerSheet(onPick = { viewModel.react(comment, it) }, onDismiss = { pickerOpen = false })
}

@Composable
private fun ReactionLabel(reaction: ReactionCount) = Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp)
) {
    if (reaction.type == ReactionSchema.LIKE) {
        Icon(
            Icons.FavoriteW500Rounded,
            contentDescription = stringResource(Res.string.social__like),
            modifier = Modifier.size(16.dp)
        )
    } else {
        ReactionEmoji(reaction.type)
    }
    RollingCount(reaction.count)
}

/** A count that rolls up when it grows and down when it shrinks. */
@Composable
private fun RollingCount(count: Int) = AnimatedContent(
    count,
    transitionSpec = {
        val up = targetState > initialState
        (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith
            (slideOutVertically { if (up) -it else it } + fadeOut())
    },
) { Text(it.toString()) }
