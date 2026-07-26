package it.maicol07.gamerlogue.ui.views.game.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import gamerlogue.sharedui.generated.resources.game__age_ratings_title
import gamerlogue.sharedui.generated.resources.game__alternative_names_title
import gamerlogue.sharedui.generated.resources.game__description_title
import gamerlogue.sharedui.generated.resources.game__bundles_title
import gamerlogue.sharedui.generated.resources.game__collections_carousel_title
import gamerlogue.sharedui.generated.resources.game__dlcs_expansions_title
import gamerlogue.sharedui.generated.resources.game__expanded_games_title
import gamerlogue.sharedui.generated.resources.game__game_modes_title
import gamerlogue.sharedui.generated.resources.game__genres_title
import gamerlogue.sharedui.generated.resources.game__keywords_title
import gamerlogue.sharedui.generated.resources.game__multiplayer_title
import gamerlogue.sharedui.generated.resources.game__no_description
import gamerlogue.sharedui.generated.resources.game__parent_games_title
import gamerlogue.sharedui.generated.resources.game__player_perspectives_title
import gamerlogue.sharedui.generated.resources.game__ports_title
import gamerlogue.sharedui.generated.resources.game__ratings_igdb_critics
import gamerlogue.sharedui.generated.resources.game__ratings_igdb_user
import gamerlogue.sharedui.generated.resources.game__ratings_title
import gamerlogue.sharedui.generated.resources.game__remakes_remasters_title
import gamerlogue.sharedui.generated.resources.game__similar_games_title
import gamerlogue.sharedui.generated.resources.game__standalone_expansions_title
import gamerlogue.sharedui.generated.resources.game__storyline_title
import gamerlogue.sharedui.generated.resources.game__themes_title
import gamerlogue.sharedui.generated.resources.game__websites_title
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Book4W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CategoryW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DevicesW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ExploreW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.InfoW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.Inventory2W500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.JoystickW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LanguageW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.LayersW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PersonW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RefreshW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarShineW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.WebTrafficW500Rounded
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
import it.maicol07.gamerlogue.extensions.igdb.displayTitle
import it.maicol07.gamerlogue.extensions.igdb.formattedCoverUrl
import it.maicol07.gamerlogue.extensions.igdb.icon
import it.maicol07.gamerlogue.extensions.igdb.localizedName
import it.maicol07.gamerlogue.ui.components.ConnectedButtonGroup
import it.maicol07.gamerlogue.ui.components.RemoteImage
import it.maicol07.gamerlogue.ui.components.game.GameCoverCard
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.components.imageviewer.FullscreenImageViewer
import it.maicol07.gamerlogue.ui.theme.Dimens
import net.sergeych.sprintf.sprintf
import org.jetbrains.compose.resources.stringResource

const val Ratio169 = 16f / 9f

/** Renders the scrollable content of the Game detail screen for a loaded [game]. */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class
)
internal fun LazyListScope.gameDetailContent(
    game: Game,
    onGameClick: ((Game) -> Unit)? = null
) {
    item { GameHeader(game) }
    item { GameRatings(game) }
    item { GameAgeRatings(game) }
    item { GameGenresAndThemes(game) }
    item { GameMultiplayerDetails(game) }
    item { GameMedia(game) }
    item { GameDescription(game) }
    item { GameKeywords(game) }
    item { GameDetailsList(game, onGameClick = onGameClick) }
    item { GameWebsites(game) }
    item { GameRelatedCarousels(game, onGameClick = onGameClick) }
    item { Spacer(Modifier.height(12.dp)) }
}

private const val RatingScale = 10

@Composable
private fun GameRatings(game: Game) {
    val ratings = remember(game) {
        listOfNotNull(
            (Res.string.game__ratings_igdb_user to game.rating to game.rating_count).takeIf { game.rating > 0.0 },
            (Res.string.game__ratings_igdb_critics to game.aggregated_rating to game.aggregated_rating_count).takeIf { game.aggregated_rating > 0.0 }
        )
    }
    if (ratings.isEmpty()) return

    Column(
        Modifier.padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        Text(stringResource(Res.string.game__ratings_title), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
            for ((pair, count) in ratings) {
                val (labelRes, value) = pair
                RatingTile(stringResource(labelRes), value, count, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RatingTile(label: String, value: Double, count: Int, modifier: Modifier) = Card(modifier) {
    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Icon(
                Icons.StarShineW500Rounded,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text(
                text = "%.1f".sprintf(value / RatingScale),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "/$RatingScale",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (count > 0) {
            Text(
                text = "($count)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun GameAgeRatings(game: Game) {
    if (game.age_ratings.isEmpty()) return

    val validRatings = remember(game) {
        game.age_ratings.mapNotNull { ageRating ->
            val title = ageRating.displayTitle()
            val logoUrl = ageRating.formattedCoverUrl()
            if (title != null || logoUrl != null) {
                ageRating to (title ?: "")
            } else null
        }.distinctBy { it.second.ifEmpty { it.first.id.toString() } }
    }
    if (validRatings.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
    ) {
        Text(
            text = stringResource(Res.string.game__age_ratings_title),
            style = MaterialTheme.typography.titleMedium
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for ((ageRating, title) in validRatings) {
                val logoUrl = ageRating.formattedCoverUrl()
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (!logoUrl.isNullOrBlank()) {
                            RemoteImage(
                                url = logoUrl,
                                contentDescription = title,
                                modifier = Modifier
                                    .height(32.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.InfoW500Rounded,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameGenresAndThemes(game: Game) {
    if (game.genres.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__genres_title)) {
            for (genre in game.genres) {
                AssistChip(
                    onClick = {},
                    leadingIcon = genre.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
                    label = { Text(genre.localizedName) }
                )
            }
        }
    }
    if (game.themes.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__themes_title)) {
            for (theme in game.themes) {
                AssistChip(
                    onClick = {},
                    leadingIcon = theme.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
                    label = { Text(theme.localizedName) }
                )
            }
        }
    }
    if (game.game_modes.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__game_modes_title)) {
            for (mode in game.game_modes) {
                AssistChip(
                    onClick = {},
                    leadingIcon = mode.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
                    label = { Text(mode.localizedName) }
                )
            }
        }
    }
    if (game.player_perspectives.isNotEmpty()) {
        ChipSection(stringResource(Res.string.game__player_perspectives_title)) {
            for (p in game.player_perspectives) {
                AssistChip(
                    onClick = {},
                    leadingIcon = p.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
                    label = { Text(p.localizedName) }
                )
            }
        }
    }
}

@Composable
private fun GameMultiplayerDetails(game: Game) {
    if (game.multiplayer_modes.isEmpty()) return

    val details = remember(game) {
        buildList {
            for (mode in game.multiplayer_modes) {
                if (mode.onlinecoop == true) {
                    val count = mode.onlinecoopmax?.takeIf { it > 0 }
                    add("Co-Op Online" + if (count != null) " (fino a $count giocatori)" else "")
                }
                if (mode.offlinecoop == true) {
                    val count = mode.offlinecoopmax?.takeIf { it > 0 }
                    add("Co-Op Locale" + if (count != null) " (fino a $count giocatori)" else "")
                }
                if (mode.campaigncoop == true) {
                    add("Co-Op Campagna")
                }
                if (mode.splitscreen == true) {
                    add("Schermo Condiviso (Split Screen)")
                }
                if (mode.lancoop == true) {
                    add("Co-Op LAN")
                }
                if (mode.dropin == true) {
                    add("Co-Op Drop-in/Drop-out")
                }
                if (mode.onlinemax != null && mode.onlinemax > 1) {
                    add("Multiplayer Online (fino a ${mode.onlinemax} giocatori)")
                }
                if (mode.offlinemax != null && mode.offlinemax > 1) {
                    add("Multiplayer Locale (fino a ${mode.offlinemax} giocatori)")
                }
            }
        }.distinct()
    }
    if (details.isEmpty()) return

    ChipSection(stringResource(Res.string.game__multiplayer_title)) {
        for (detail in details) {
            AssistChip(
                onClick = {},
                leadingIcon = { Icon(Icons.JoystickW500Rounded, contentDescription = null) },
                label = { Text(detail) }
            )
        }
    }
}

@Composable
private fun ChipSection(title: String, content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameMedia(game: Game) {
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

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.count() },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        preferredItemWidth = 200.dp,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding)
    ) { i ->
        val item = items[i]
        val itemModifier = Modifier
            .maskClip(MaterialTheme.shapes.large)
            .aspectRatio(Ratio169)

        when (item) {
            is Artwork -> item.Image(itemModifier.clickable {
                initialViewerIndex = i
                showViewer = true
            })
            is Screenshot -> item.Image(itemModifier.clickable {
                initialViewerIndex = i
                showViewer = true
            })
            is GameVideo -> {
                val videoId = item.video_id
                Box(
                    modifier = itemModifier.clickable {
                        if (!videoId.isNullOrBlank()) {
                            try {
                                uriHandler.openUri("https://www.youtube.com/watch?v=$videoId")
                            } catch (_: Exception) {}
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    RemoteImage(
                        url = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                        contentDescription = item.name ?: game.name,
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
            else -> {}
        }
    }

    if (showViewer) {
        FullscreenImageViewer(
            imagesCount = items.size,
            initialPage = initialViewerIndex,
            onDismissRequest = { showViewer = false },
            imageContent = { page, modifier ->
                when (val item = items[page]) {
                    is Artwork -> item.Image(modifier.aspectRatio(Ratio169))
                    is Screenshot -> item.Image(modifier.aspectRatio(Ratio169))
                    else -> {}
                }
            },
            thumbnailContent = { page, modifier ->
                when (val item = items[page]) {
                    is Artwork -> item.Image(modifier.aspectRatio(Ratio169))
                    is Screenshot -> item.Image(modifier.aspectRatio(Ratio169))
                    else -> {}
                }
            }
        )
    }
}

@Composable
private fun GameDescription(game: Game) {
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
private fun GameKeywords(game: Game) {
    if (game.keywords.isEmpty()) return

    val items = remember(game) {
        game.keywords.mapNotNull { kw -> kw.name.takeIf { !it.isNullOrBlank() } }.distinct()
    }
    if (items.isEmpty()) return

    ChipSection(stringResource(Res.string.game__keywords_title)) {
        for (kw in items.take(15)) {
            AssistChip(
                onClick = {},
                label = { Text("#$kw") }
            )
        }
    }
}

@Composable
private fun GameWebsites(game: Game) {
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
                try {
                    val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                    uriHandler.openUri(formattedUrl)
                } catch (_: Exception) {}
            },
            toggleButtonText = { website ->
                getWebsiteInfo(website.url ?: "").first
            },
            toggleButtonIcon = { website ->
                getWebsiteInfo(website.url ?: "").second
            },
            rowModifier = Modifier.fillMaxWidth()
        )
    }
}

private fun getWebsiteInfo(url: String): Pair<String, ImageVector?> {
    val lower = url.lowercase()
    return when {
        "steampowered.com" in lower || "steam.com" in lower -> "Steam" to SimpleIconsRoot.SteamSimpleIcons
        "gog.com" in lower -> "GOG" to SimpleIconsRoot.GogdotcomSimpleIcons
        "epicgames.com" in lower -> "Epic Games" to SimpleIconsRoot.EpicgamesSimpleIcons
        "playstation.com" in lower -> "PlayStation" to SimpleIconsRoot.PlaystationSimpleIcons
        "xbox.com" in lower || "microsoft.com" in lower -> "Xbox" to SvglIconsRoot.XboxSvgl
        "nintendo.com" in lower -> "Nintendo" to Icons.JoystickW500Rounded
        "facebook.com" in lower || "fb.com" in lower -> "Facebook" to SimpleIconsRoot.FacebookSimpleIcons
        "fandom.com" in lower || "wikia.com" in lower || "wikia.org" in lower -> "Fandom" to SimpleIconsRoot.FandomSimpleIcons
        "instagram.com" in lower -> "Instagram" to SimpleIconsRoot.InstagramSimpleIcons
        "x.com" in lower || "twitter.com" in lower -> "X" to SimpleIconsRoot.XSimpleIcons
        "twitch.tv" in lower || "twitch.com" in lower -> "Twitch" to SimpleIconsRoot.TwitchSimpleIcons
        "wikipedia.org" in lower -> "Wikipedia" to SimpleIconsRoot.WikipediaSimpleIcons
        "reddit.com" in lower -> "Reddit" to SimpleIconsRoot.RedditSimpleIcons
        "discord.gg" in lower || "discord.com" in lower -> "Discord" to SimpleIconsRoot.DiscordSimpleIcons
        "youtube.com" in lower || "youtu.be" in lower -> "YouTube" to SimpleIconsRoot.YoutubeSimpleIcons
        "apple.com" in lower -> "App Store" to SimpleIconsRoot.AppleSimpleIcons
        "play.google.com" in lower -> "Google Play" to SimpleIconsRoot.AndroidSimpleIcons
        else -> {
            val cleanDomain = extractCleanDomain(url)
            cleanDomain to Icons.LanguageW500Rounded
        }
    }
}

private fun extractCleanDomain(url: String): String {
    return try {
        val host = url.substringAfter("://").substringBefore("/")
        val domain = host.removePrefix("www.").removePrefix("m.")
        if (domain.isNotBlank()) domain.replaceFirstChar { it.uppercase() } else "Website"
    } catch (_: Exception) {
        "Website"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameRelatedCarousels(
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
        Triple(Res.string.game__parent_games_title, Icons.JoystickW500Rounded, if (parentGames.size > 1) parentGames else emptyList()),
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
        if (gamesList.isNotEmpty()) {
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

                HorizontalMultiBrowseCarousel(
                    state = rememberCarouselState { gamesList.size },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    preferredItemWidth = 120.dp,
                    itemSpacing = 12.dp,
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding)
                ) { index ->
                    val relatedGame = gamesList[index]
                    val metadata = ReleaseDate(date = relatedGame.first_release_date).displayDate()
                    GameCoverCard(
                        game = relatedGame,
                        metadata = metadata,
                        showTitle = true,
                        modifier = Modifier.maskClip(MaterialTheme.shapes.large),
                        onClick = { onGameClick?.invoke(it) }
                    )
                }
            }
        }
    }
}
