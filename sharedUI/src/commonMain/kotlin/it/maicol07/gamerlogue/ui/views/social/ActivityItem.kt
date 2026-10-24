package it.maicol07.gamerlogue.ui.views.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.activity__badge_earned
import gamerlogue.sharedui.generated.resources.activity__completed
import gamerlogue.sharedui.generated.resources.activity__followed
import gamerlogue.sharedui.generated.resources.activity__library_added
import gamerlogue.sharedui.generated.resources.activity__library_imported
import gamerlogue.sharedui.generated.resources.activity__list_created
import gamerlogue.sharedui.generated.resources.activity__list_item_added
import gamerlogue.sharedui.generated.resources.activity__list_items_added
import gamerlogue.sharedui.generated.resources.activity__rated
import gamerlogue.sharedui.generated.resources.activity__reviewed
import gamerlogue.sharedui.generated.resources.activity__status_changed
import gamerlogue.sharedui.generated.resources.activity__unknown_game
import gamerlogue.sharedui.generated.resources.badge__completed_1
import gamerlogue.sharedui.generated.resources.badge__completed_10
import gamerlogue.sharedui.generated.resources.badge__completed_100
import gamerlogue.sharedui.generated.resources.badge__completed_50
import gamerlogue.sharedui.generated.resources.badge__first_list
import gamerlogue.sharedui.generated.resources.badge__game_100_hours
import gamerlogue.sharedui.generated.resources.badge__library_100
import gamerlogue.sharedui.generated.resources.badge__platforms_5
import gamerlogue.sharedui.generated.resources.badge__reviews_1
import gamerlogue.sharedui.generated.resources.badge__reviews_10
import gamerlogue.sharedui.generated.resources.badge__reviews_50
import it.maicol07.gamerlogue.data.Activity
import it.maicol07.gamerlogue.data.ActivityVerb
import it.maicol07.gamerlogue.extensions.actorId
import it.maicol07.gamerlogue.extensions.displayTimestamp
import it.maicol07.gamerlogue.extensions.gameId
import it.maicol07.gamerlogue.extensions.gameIds
import it.maicol07.gamerlogue.extensions.igdb.detailNavKey
import it.maicol07.gamerlogue.extensions.payloadText
import it.maicol07.gamerlogue.extensions.subjectId
import it.maicol07.gamerlogue.ui.components.currentRatingScale
import it.maicol07.gamerlogue.ui.components.game.CoverImage
import it.maicol07.gamerlogue.ui.components.user.UserAvatar
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val ActivityCoverWidth = 40.dp
private val ActivityCoverHeight = 53.dp

/**
 * One activity: who did what, when, with the game's cover when there is one. Returns nothing for a
 * verb this version doesn't know, so a newer server can add verbs without breaking the feed.
 */
@Composable
fun ActivityItem(activity: Activity, game: Game?, onNavigate: (NavKey) -> Unit, modifier: Modifier = Modifier) {
    val verb = ActivityVerb.of(activity.verb) ?: return
    val user = activity.user
    val target = activity.target(verb, game)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (target != null) it.clickable { onNavigate(target) } else it }
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemGap),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(
            user,
            40.dp,
            Modifier.clip(CircleShape).let { m ->
                activity.actorId?.let { id -> m.clickable { onNavigate(RootNavTree.UserProfile(id)) } } ?: m
            }
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                activity.describe(verb, user?.displayName.orEmpty(), game),
                style = MaterialTheme.typography.bodyMedium
            )
            displayTimestamp(activity.createdAt)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        game?.CoverImage(
            Modifier.clip(MaterialTheme.shapes.small),
            sizeModifier = Modifier.width(ActivityCoverWidth).height(ActivityCoverHeight)
        )
    }
}

@Composable
private fun Activity.describe(verb: ActivityVerb, actor: String, game: Game?): String {
    val gameName = game?.name ?: stringResource(Res.string.activity__unknown_game)
    return when (verb) {
        ActivityVerb.LIBRARY_ADDED -> stringResource(Res.string.activity__library_added, actor, gameName)
        ActivityVerb.COMPLETED -> stringResource(Res.string.activity__completed, actor, gameName)
        ActivityVerb.STATUS_CHANGED -> {
            val status = payloadText("new")?.let { runCatching { GameLibraryStatus.valueOf(it) }.getOrNull() }
            val statusName = status?.let { stringResource(it.displayName) }.orEmpty()
            stringResource(Res.string.activity__status_changed, actor, gameName, statusName)
        }
        ActivityVerb.RATED -> stringResource(
            Res.string.activity__rated,
            actor,
            gameName,
            payloadText("rating")?.toDoubleOrNull()?.let(currentRatingScale()::format).orEmpty()
        )
        ActivityVerb.REVIEWED -> stringResource(Res.string.activity__reviewed, actor, gameName)
        ActivityVerb.LIST_CREATED -> stringResource(
            Res.string.activity__list_created,
            actor,
            payloadText("name").orEmpty()
        )
        ActivityVerb.LIST_ITEM_ADDED -> listItemsAdded(actor, gameName)
        ActivityVerb.FOLLOWED -> stringResource(Res.string.activity__followed, actor, payloadText("nickname").orEmpty())
        ActivityVerb.LIBRARY_IMPORTED -> stringResource(
            Res.string.activity__library_imported,
            actor,
            payloadText("count").orEmpty()
        )
        ActivityVerb.BADGE_EARNED ->
            stringResource(Res.string.activity__badge_earned, actor, badgeLabel(payloadText("badge").orEmpty()))
    }
}

/** One game added to a list, or how many when the grouping folded several. */
@Composable
private fun Activity.listItemsAdded(actor: String, gameName: String): String {
    val listName = payloadText("name").orEmpty()
    val count = gameIds.size
    return if (count > 1) {
        pluralStringResource(Res.plurals.activity__list_items_added, count, actor, count, listName)
    } else {
        stringResource(Res.string.activity__list_item_added, actor, gameName, listName)
    }
}

/** Where tapping the activity leads: the review, the list, the followed user or the game. */
private fun Activity.target(verb: ActivityVerb, game: Game?): NavKey? = when (verb) {
    ActivityVerb.REVIEWED -> reviewTarget()
    ActivityVerb.LIST_CREATED, ActivityVerb.LIST_ITEM_ADDED -> subjectId?.let(RootNavTree::UserListDetail)
    ActivityVerb.FOLLOWED -> subjectId?.let(RootNavTree::UserProfile)
    ActivityVerb.LIBRARY_IMPORTED, ActivityVerb.BADGE_EARNED -> actorId?.let(RootNavTree::UserProfile)
    else -> game?.detailNavKey ?: gameId?.let { RootNavTree.GameDetail(it, null, null) }
}

private fun Activity.reviewTarget(): NavKey? = subjectId?.let { id ->
    actorId?.let {
        RootNavTree.ReviewDetail(
            id,
            it
        )
    }
}

/** The badge's name; an unknown key (a badge newer than this app) shows as is. */
@Composable
private fun badgeLabel(key: String): String = BadgeLabels[key]?.let { stringResource(it) } ?: key

private val BadgeLabels: Map<String, StringResource> = mapOf(
    "completed_1" to Res.string.badge__completed_1,
    "completed_10" to Res.string.badge__completed_10,
    "completed_50" to Res.string.badge__completed_50,
    "completed_100" to Res.string.badge__completed_100,
    "reviews_1" to Res.string.badge__reviews_1,
    "reviews_10" to Res.string.badge__reviews_10,
    "reviews_50" to Res.string.badge__reviews_50,
    "game_100_hours" to Res.string.badge__game_100_hours,
    "platforms_5" to Res.string.badge__platforms_5,
    "first_list" to Res.string.badge__first_list,
    "library_100" to Res.string.badge__library_100,
)
