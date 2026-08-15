package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Artwork
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameVideo
import at.released.igdbclient.model.ReleaseDate
import at.released.igdbclient.model.Screenshot
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__bundles_title
import gamerlogue.sharedui.generated.resources.game__collections_carousel_title
import gamerlogue.sharedui.generated.resources.game__description_title
import gamerlogue.sharedui.generated.resources.game__dlcs_expansions_title
import gamerlogue.sharedui.generated.resources.game__expanded_games_title
import gamerlogue.sharedui.generated.resources.game__no_description
import gamerlogue.sharedui.generated.resources.game__parent_games_title
import gamerlogue.sharedui.generated.resources.game__ports_title
import gamerlogue.sharedui.generated.resources.game__remakes_remasters_title
import gamerlogue.sharedui.generated.resources.game__similar_games_title
import gamerlogue.sharedui.generated.resources.game__standalone_expansions_title
import gamerlogue.sharedui.generated.resources.game__storyline_title
import gamerlogue.sharedui.generated.resources.game__websites_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CategoryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DevicesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Inventory2W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LanguageW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LayersW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RefreshW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.Icons as SimpleIconsRoot
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.AndroidSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.AppleSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.DiscordSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.EpicgamesSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.FacebookSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.FandomSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.GogdotcomSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.InstagramSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.PlaystationSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.RedditSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.SteamSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.TwitchSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.WikipediaSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.XSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.`simple-icons`.icons.YoutubeSimpleIcons
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.Icons as SvglIconsRoot
import io.github.kingsword09.symbolcraft.symbols.icons.svgl.icons.XboxSvgl
import it.maicol07.gamerlogue.extensions.igdb.displayDate
import it.maicol07.gamerlogue.ui.components.ConnectedButtonGroup
import it.maicol07.gamerlogue.ui.components.GameCoverCarousel
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.game.GameCoverCard
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.components.imageviewer.FullscreenImageViewer
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

private val MediaItemWidth = 200.dp
private val RelatedItemWidth = 120.dp
private val RelatedCarouselHeight = 180.dp

/** Videos, artworks and screenshots in one carousel, with a fullscreen viewer for the images. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun GameMedia(game: Game) {
    val items = remember(game) {
        buildList {
            addAll(game.videos)
            addAll(game.artworks)
            addAll(game.screenshots)
        }
    }
    if (items.isEmpty()) return

    var showViewer by remember { mutableStateOf(false) }
    var initialViewerIndex by remember { mutableStateOf(0) }
    val uriHandler = LocalUriHandler.current

    GameCoverCarousel(
        itemCount = items.count(),
        preferredItemWidth = MediaItemWidth,
        modifier = Modifier.wrapContentHeight()
    ) { i ->
        val item = items[i]
        val itemModifier = Modifier
            .maskClip(MaterialTheme.shapes.large)
            .aspectRatio(Ratio169)

        val openViewer = {
            initialViewerIndex = i
            showViewer = true
        }
        when (item) {
            is Artwork -> item.Image(itemModifier.clickable(onClick = openViewer))
            is Screenshot -> item.Image(itemModifier.clickable(onClick = openViewer))
            is GameVideo -> VideoThumbnail(item, game, itemModifier) { videoId ->
                runCatching { uriHandler.openUri("https://www.youtube.com/watch?v=$videoId") }
            }
            else -> {}
        }
    }

    if (showViewer) {
        FullscreenImageViewer(
            imagesCount = items.size,
            initialPage = initialViewerIndex,
            onDismissRequest = { showViewer = false },
            imageContent = { page, modifier -> MediaImage(items[page], modifier) },
            thumbnailContent = { page, modifier -> MediaImage(items[page], modifier) }
        )
    }
}

@Composable
private fun MediaImage(item: Any, modifier: Modifier) {
    when (item) {
        is Artwork -> item.Image(modifier.aspectRatio(Ratio169))
        is Screenshot -> item.Image(modifier.aspectRatio(Ratio169))
        else -> {}
    }
}

@Composable
private fun VideoThumbnail(
    video: GameVideo,
    game: Game,
    modifier: Modifier,
    onPlay: (videoId: String) -> Unit,
) {
    val videoId = video.video_id
    Box(
        modifier = modifier.clickable { if (!videoId.isNullOrBlank()) onPlay(videoId) },
        contentAlignment = Alignment.Center
    ) {
        RemoteImage(
            url = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
            contentDescription = video.name ?: game.name,
            modifier = Modifier.fillMaxWidth().aspectRatio(Ratio169)
        )
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(
                Icons.PlayCircleW500Rounded,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
internal fun GameDescription(game: Game) {
    val summary = game.summary.takeIf { it.isNotBlank() }
    val storyline = game.storyline.takeIf { it.isNotBlank() }
    if (summary == null && storyline == null) {
        Column(
            Modifier.padding(horizontal = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(stringResource(Res.string.game__description_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(Res.string.game__no_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (summary != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.game__description_title), style = MaterialTheme.typography.titleMedium)
                Text(summary)
            }
        }
        if (storyline != null) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.game__storyline_title), style = MaterialTheme.typography.titleMedium)
                Text(storyline)
            }
        }
    }
}

@Composable
internal fun GameWebsites(game: Game) {
    if (game.websites.isEmpty()) return
    val uriHandler = LocalUriHandler.current

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
    ) {
        Text(
            text = stringResource(Res.string.game__websites_title),
            style = MaterialTheme.typography.titleMedium
        )

        val validWebsites = remember(game.websites) { game.websites.filter { !it.url.isNullOrBlank() } }
        if (validWebsites.isEmpty()) return@Column

        ConnectedButtonGroup(
            options = validWebsites,
            checked = { false },
            onCheckedChange = { website, _ ->
                val url = website.url ?: return@ConnectedButtonGroup
                val formattedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                runCatching { uriHandler.openUri(formattedUrl) }
            },
            toggleButtonText = { website -> websiteInfo(website.url ?: "").first },
            toggleButtonIcon = { website -> websiteInfo(website.url ?: "").second },
            rowModifier = Modifier.fillMaxWidth()
        )
    }
}

/** Label and icon for a store/social/media URL; falls back to the bare domain. */
@Suppress("CyclomaticComplexMethod")
private fun websiteInfo(url: String): Pair<String, ImageVector?> {
    val lower = url.lowercase()
    return when {
        "steampowered.com" in lower || "steam.com" in lower -> "Steam" to SimpleIconsRoot.SteamSimpleIcons
        "gog.com" in lower -> "GOG" to SimpleIconsRoot.GogdotcomSimpleIcons
        "epicgames.com" in lower -> "Epic Games" to SimpleIconsRoot.EpicgamesSimpleIcons
        "playstation.com" in lower -> "PlayStation" to SimpleIconsRoot.PlaystationSimpleIcons
        "xbox.com" in lower || "microsoft.com" in lower -> "Xbox" to SvglIconsRoot.XboxSvgl
        "nintendo.com" in lower -> "Nintendo" to Icons.JoystickW500Rounded
        "facebook.com" in lower || "fb.com" in lower -> "Facebook" to SimpleIconsRoot.FacebookSimpleIcons
        "fandom.com" in lower || "wikia.com" in lower || "wikia.org" in lower ->
            "Fandom" to SimpleIconsRoot.FandomSimpleIcons
        "instagram.com" in lower -> "Instagram" to SimpleIconsRoot.InstagramSimpleIcons
        "x.com" in lower || "twitter.com" in lower -> "X" to SimpleIconsRoot.XSimpleIcons
        "twitch.tv" in lower || "twitch.com" in lower -> "Twitch" to SimpleIconsRoot.TwitchSimpleIcons
        "wikipedia.org" in lower -> "Wikipedia" to SimpleIconsRoot.WikipediaSimpleIcons
        "reddit.com" in lower -> "Reddit" to SimpleIconsRoot.RedditSimpleIcons
        "discord.gg" in lower || "discord.com" in lower -> "Discord" to SimpleIconsRoot.DiscordSimpleIcons
        "youtube.com" in lower || "youtu.be" in lower -> "YouTube" to SimpleIconsRoot.YoutubeSimpleIcons
        "apple.com" in lower -> "App Store" to SimpleIconsRoot.AppleSimpleIcons
        "play.google.com" in lower -> "Google Play" to SimpleIconsRoot.AndroidSimpleIcons
        else -> cleanDomain(url) to Icons.LanguageW500Rounded
    }
}

private fun cleanDomain(url: String): String {
    val host = url.substringAfter("://").substringBefore("/")
    val domain = host.removePrefix("www.").removePrefix("m.")
    return if (domain.isNotBlank()) domain.replaceFirstChar { it.uppercase() } else "Website"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GameRelatedCarousels(
    game: Game,
    onGameClick: ((Game) -> Unit)?
) {
    val parentGames = remember(game) { listOfNotNull(game.parent_game, game.version_parent).distinctBy { it.id } }
    val dlcsAndExpansions = remember(game) { (game.dlcs + game.expansions).distinctBy { it.id } }
    val standaloneExpansions = remember(game) { game.standalone_expansions.distinctBy { it.id } }
    val expandedVersions = remember(game) { game.expanded_games.distinctBy { it.id } }
    val bundles = remember(game) { game.bundles.distinctBy { it.id } }
    val ports = remember(game) { game.ports.distinctBy { it.id } }
    val remakesAndRemasters = remember(game) { (game.remakes + game.remasters).distinctBy { it.id } }
    val similarGames = remember(game) { game.similar_games.distinctBy { it.id } }
    val collectionsGames = remember(game) { game.collections.flatMap { it.games }.distinctBy { it.id } }

    val sections = listOf(
        // A single parent game is already shown by the header, so it only earns a carousel when there are several.
        Triple(
            Res.string.game__parent_games_title,
            Icons.JoystickW500Rounded,
            if (parentGames.size > 1) parentGames else emptyList()
        ),
        Triple(Res.string.game__dlcs_expansions_title, Icons.Inventory2W500Rounded, dlcsAndExpansions),
        Triple(Res.string.game__standalone_expansions_title, Icons.LayersW500Rounded, standaloneExpansions),
        Triple(Res.string.game__expanded_games_title, Icons.CategoryW500Rounded, expandedVersions),
        Triple(Res.string.game__bundles_title, Icons.Inventory2W500Rounded, bundles),
        Triple(Res.string.game__ports_title, Icons.DevicesW500Rounded, ports),
        Triple(Res.string.game__remakes_remasters_title, Icons.RefreshW500Rounded, remakesAndRemasters),
        Triple(Res.string.game__similar_games_title, Icons.ExploreW500Rounded, similarGames),
        Triple(Res.string.game__collections_carousel_title, Icons.CategoryW500Rounded, collectionsGames)
    )

    for ((titleRes, icon, gamesList) in sections) {
        if (gamesList.isEmpty()) continue
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            GameCoverCarousel(
                itemCount = gamesList.size,
                preferredItemWidth = RelatedItemWidth,
                modifier = Modifier.height(RelatedCarouselHeight)
            ) { index ->
                val relatedGame = gamesList[index]
                val metadata = ReleaseDate(date = relatedGame.first_release_date).displayDate()
                GameCoverCard(
                    game = relatedGame,
                    metadata = listOfNotNull(metadata),
                    showTitle = true,
                    modifier = Modifier.maskClip(MaterialTheme.shapes.large),
                    onClick = { onGameClick?.invoke(it) }
                )
            }
        }
    }
}
