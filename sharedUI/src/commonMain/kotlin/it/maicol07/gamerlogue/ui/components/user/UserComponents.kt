package it.maicol07.gamerlogue.ui.components.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.maicol07.gamerlogue.data.UserSchema
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.ui.components.RemoteImage

private const val AvatarInitialScale = 0.4f

/** The user's provider picture in a circle, or their initial when they have none. */
@Composable
fun UserAvatar(user: UserSchema?, size: Dp, modifier: Modifier = Modifier) {
    // Only a web address is a picture: the provider can send a bare placeholder name (e.g. "gravatar").
    val picture = user?.picture?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    val name = user?.displayName.orEmpty()
    if (picture != null) {
        RemoteImage(
            url = picture,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
            loadingModifier = Modifier.size(size),
            // A picture the provider no longer serves falls back to the initial instead of a hole.
            failure = { AvatarInitial(name, size, Modifier) },
        )
    } else {
        AvatarInitial(name, size, modifier)
    }
}

/** The user's initial on a color picked by [name], for who has no picture. */
@Composable
private fun AvatarInitial(name: String, size: Dp, modifier: Modifier) {
    val (container, content) = placeholderColors(name)
    Box(
        modifier = modifier.size(size).background(container, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            color = content,
            fontSize = (size.value * AvatarInitialScale).sp
        )
    }
}

/**
 * A container color and its content color for a picture placeholder, picked by [seed] (a
 * user name) so a user always gets the same one and different users tell apart.
 */
@Composable
private fun placeholderColors(seed: String?): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    val options = listOf(
        colors.primaryContainer to colors.onPrimaryContainer,
        colors.secondaryContainer to colors.onSecondaryContainer,
        colors.tertiaryContainer to colors.onTertiaryContainer,
    )
    return options[placeholderIndex(seed, options.size)]
}

/** A stable index in `0 until count` for [seed]; 0 without one. */
fun placeholderIndex(seed: String?, count: Int): Int = (seed?.hashCode() ?: 0).mod(count)

/** A user in a segmented list: avatar, nickname and full name, with optional [trailing] actions. */
@Composable
fun UserRow(
    user: UserSchema,
    shapes: ListItemShapes,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) = SegmentedListItem(
    onClick = onClick,
    shapes = shapes,
    colors = ListItemDefaults.expressiveSegmentedColors(),
    modifier = modifier,
    leadingContent = { UserAvatar(user, 40.dp) },
    supportingContent = user.name?.takeIf { user.nickname != null && it.isNotBlank() }?.let { name ->
        {
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    },
    trailingContent = trailing,
) {
    Text(user.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
