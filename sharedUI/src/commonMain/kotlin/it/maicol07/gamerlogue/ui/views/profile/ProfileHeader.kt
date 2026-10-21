package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.profile__follow
import gamerlogue.sharedui.generated.resources.profile__followers
import gamerlogue.sharedui.generated.resources.profile__following
import gamerlogue.sharedui.generated.resources.profile__follows_you
import gamerlogue.sharedui.generated.resources.profile__private
import gamerlogue.sharedui.generated.resources.profile__unfollow
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LockW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonAddW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonRemoveW500Rounded
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.ui.components.game.GameBannerImage
import it.maicol07.gamerlogue.ui.components.game.bottomScrim
import it.maicol07.gamerlogue.ui.components.user.UserAvatar
import it.maicol07.gamerlogue.ui.components.user.placeholderIndex
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.social.ConnectionsKind
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val BannerHeight = 120.dp
private val AvatarSize = 88.dp
private val AvatarBorder = 4.dp
private val NameLineGap = 2.dp
private val ConnectedGap = 2.dp
private val OuterCorner = 28.dp
private val InnerCorner = 8.dp
private val CountPressedCorner = 16.dp
private const val BannerShapeAlpha = 0.22f
private const val BannerShapeRotation = 15f

/** Banner and avatar, names with the follow action, bio, the follow counts and the platforms. */
@Composable
internal fun ProfileHeader(
    user: User,
    state: ProfileViewModel.UiState,
    onToggleFollow: () -> Unit,
    onConnections: (ConnectionsKind) -> Unit,
) = Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
    ProfileBanner(user)
    Row(
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NameLineGap)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
            ) {
                Text(user.displayName, style = MaterialTheme.typography.headlineSmallEmphasized)
                if (user.isPrivate) {
                    Icon(
                        Icons.LockW500Rounded,
                        contentDescription = stringResource(Res.string.profile__private),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            SecondaryLine(user, followsYou = !state.isSelf && user.followsMe)
        }
        // A private profile cannot be followed: there would be nothing to see.
        if (!state.isSelf && !state.contentHidden) FollowButton(state, onToggleFollow)
    }
    user.bio?.takeIf { it.isNotBlank() && !state.contentHidden }?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
        )
    }
    if (!state.contentHidden) FollowCounts(state.followersCount, user.followingCount, onConnections)
    if (state.platforms.isNotEmpty()) {
        PlatformChips(state.platforms, Modifier.padding(horizontal = Dimens.ScreenPadding))
    }
}

/** The real name (when a nickname is shown above it) and the "follows you" pill, on one line. */
@Composable
private fun SecondaryLine(user: User, followsYou: Boolean) {
    val name = user.name?.takeIf { user.nickname != null && it.isNotBlank() }
    if (name == null && !followsYou) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        name?.let {
            Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (followsYou) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                shape = CircleShape,
            ) {
                Text(
                    stringResource(Res.string.profile__follows_you),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = Dimens.ItemGap, vertical = NameLineGap)
                )
            }
        }
    }
}

/**
 * Followers and followed users as two connected tonal buttons, each opening its list: the outer corners
 * fully round, the inner ones tight, and every corner rounding further while pressed.
 */
@Composable
private fun FollowCounts(followers: Int, following: Int, onConnections: (ConnectionsKind) -> Unit) = Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemGap / 2),
    horizontalArrangement = Arrangement.spacedBy(ConnectedGap),
) {
    val pressed = RoundedCornerShape(CountPressedCorner)
    CountButton(
        followers,
        Res.string.profile__followers,
        ButtonDefaults.shapes(RoundedCornerShape(OuterCorner, InnerCorner, InnerCorner, OuterCorner), pressed),
    ) { onConnections(ConnectionsKind.FOLLOWERS) }
    CountButton(
        following,
        Res.string.profile__following,
        ButtonDefaults.shapes(RoundedCornerShape(InnerCorner, OuterCorner, OuterCorner, InnerCorner), pressed),
    ) { onConnections(ConnectionsKind.FOLLOWING) }
}

@Composable
private fun RowScope.CountButton(count: Int, label: StringResource, shapes: ButtonShapes, onClick: () -> Unit) =
    FilledTonalButton(
        onClick = onClick,
        shapes = shapes,
        contentPadding = PaddingValues(vertical = Dimens.SectionGap),
        modifier = Modifier.weight(1f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(count.toString(), style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        }
    }

/** The rounded banner with the avatar hanging over its bottom edge. */
@Composable
private fun ProfileBanner(user: User) = Box(
    Modifier.fillMaxWidth().height(BannerHeight + AvatarSize / 2).padding(horizontal = Dimens.ScreenPadding)
) {
    val bannerModifier = Modifier.fillMaxWidth().height(BannerHeight).clip(MaterialTheme.shapes.extraLarge)
    val bannerImageId = user.bannerImageId
    if (bannerImageId != null) {
        GameBannerImage(
            bannerImageId,
            modifier = bannerModifier.bottomScrim(),
            loadingModifier = bannerModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        BannerPlaceholder(user.displayName, bannerModifier)
    }
    UserAvatar(
        user,
        AvatarSize,
        Modifier
            .align(Alignment.BottomStart)
            .offset(x = Dimens.ItemGap)
            .border(AvatarBorder, MaterialTheme.colorScheme.surface, CircleShape)
    )
}

/** A banner for who has none: a tonal gradient with a few expressive shapes, always the same for a user. */
@Composable
private fun BannerPlaceholder(seed: String?, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val variant = placeholderIndex(seed, BannerVariants.size)
    val (large, medium, small) = BannerVariants[variant]
    val (from, to, accent) = listOf(
        Triple(colors.primaryContainer, colors.tertiaryContainer, colors.primary),
        Triple(colors.secondaryContainer, colors.primaryContainer, colors.secondary),
        Triple(colors.tertiaryContainer, colors.secondaryContainer, colors.tertiary),
    )[variant % BannerPalettes]
    Box(modifier.background(Brush.linearGradient(listOf(from, to)))) {
        BannerShape(large, 132.dp, accent, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-36).dp))
        BannerShape(medium, 72.dp, accent, Modifier.align(Alignment.BottomCenter).offset(x = 24.dp, y = 28.dp))
        BannerShape(small, 36.dp, accent, Modifier.align(Alignment.TopCenter).offset(x = (-48).dp, y = 20.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BannerShape(shape: RoundedPolygon, size: Dp, color: Color, modifier: Modifier) = Box(
    modifier
        .size(size)
        .rotate(BannerShapeRotation)
        .background(color.copy(alpha = BannerShapeAlpha), shape.toShape())
)

private const val BannerPalettes = 3

/** The large, medium and small shape of each placeholder banner. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val BannerVariants: List<Triple<RoundedPolygon, RoundedPolygon, RoundedPolygon>> = listOf(
    Triple(MaterialShapes.Cookie9Sided, MaterialShapes.Pill, MaterialShapes.Sunny),
    Triple(MaterialShapes.Flower, MaterialShapes.Gem, MaterialShapes.Clover4Leaf),
    Triple(MaterialShapes.SoftBurst, MaterialShapes.Arch, MaterialShapes.Puffy),
    Triple(MaterialShapes.Clover8Leaf, MaterialShapes.Cookie4Sided, MaterialShapes.Burst),
)

@Composable
private fun FollowButton(state: ProfileViewModel.UiState, onToggleFollow: () -> Unit) = if (state.following) {
    OutlinedButton(onClick = onToggleFollow, enabled = !state.followPending, shapes = ButtonDefaults.shapes()) {
        ActionLabel(Icons.PersonRemoveW500Rounded, Res.string.profile__unfollow, state.followPending)
    }
} else {
    Button(onClick = onToggleFollow, enabled = !state.followPending, shapes = ButtonDefaults.shapes()) {
        ActionLabel(Icons.PersonAddW500Rounded, Res.string.profile__follow, state.followPending)
    }
}

/** Leading icon plus label, the content of the follow actions; an indicator replaces the icon while [loading]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionLabel(icon: ImageVector, label: StringResource, loading: Boolean) {
    AnimatedContent(loading, transitionSpec = { fadeIn() togetherWith fadeOut() }) { busy ->
        if (busy) {
            LoadingIndicator(Modifier.size(ButtonDefaults.IconSize), color = LocalContentColor.current)
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        }
    }
    Text(stringResource(label), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
}
