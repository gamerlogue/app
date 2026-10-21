package it.maicol07.gamerlogue.ui.views.comments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.emoji__group_activities
import gamerlogue.sharedui.generated.resources.emoji__group_animals_nature
import gamerlogue.sharedui.generated.resources.emoji__group_animated
import gamerlogue.sharedui.generated.resources.emoji__group_flags
import gamerlogue.sharedui.generated.resources.emoji__group_food_drink
import gamerlogue.sharedui.generated.resources.emoji__group_objects
import gamerlogue.sharedui.generated.resources.emoji__group_people_body
import gamerlogue.sharedui.generated.resources.emoji__group_smileys_emotion
import gamerlogue.sharedui.generated.resources.emoji__group_symbols
import gamerlogue.sharedui.generated.resources.emoji__group_travel_places
import gamerlogue.sharedui.generated.resources.reactions__pause
import gamerlogue.sharedui.generated.resources.reactions__play
import gamerlogue.sharedui.generated.resources.reactions__search
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PauseCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SearchW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.StarShineW500Rounded
import it.maicol07.gamerlogue.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.kodein.emoji.Emoji
import org.kodein.emoji.Toned1Emoji
import org.kodein.emoji.Toned2Emoji
import org.kodein.emoji.allGroups
import org.kodein.emoji.allOf
import org.kodein.emoji.compose.EmojiService
import org.kodein.emoji.compose.NotoAnimatedEmoji
import org.kodein.emoji.compose.NotoImageEmoji
import org.kodein.emoji.findEmoji

private val EmojiCellSize = 48.dp
private val AnimatedEmojiSize = 32.dp
private val ReactionEmojiSize = 18.dp
private val GridHeight = 320.dp
private val TabEmojiSize = 24.dp

/** The emoji by group, without the skin-toned variants or those Noto lacks, and the animated ones among them. */
private class EmojiCatalog(val groups: List<Pair<String, List<Emoji>>>, val animated: List<Emoji>) {
    val all: List<Emoji> get() = groups.flatMap { it.second }
}

// ponytail: kept for the process, since Emoji.kt warns the groups are heavy to build; never invalidated.
private var cachedCatalog: EmojiCatalog? = null

private suspend fun loadCatalog(): EmojiCatalog = withContext(Dispatchers.Default) {
    val groups = Emoji.allGroups().map { group ->
        // Only what Noto draws: its image components refuse the others.
        group to Emoji.allOf(group).filter { it !is Toned1Emoji && it !is Toned2Emoji && it.details.hasNotoImage }
    }
    EmojiCatalog(groups, groups.flatMap { it.second }.filter { it.details.hasNotoAnimation })
}.also { cachedCatalog = it }

/**
 * Picks one emoji: the animated ones first, as their own tab, then every group; typing searches them
 * all by name. The animated emoji play in the grid, the others show the still Noto image.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EmojiPickerSheet(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val catalog by produceState(cachedCatalog) { value = cachedCatalog ?: loadCatalog() }
    val query = rememberTextFieldState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var playing by rememberSaveable { mutableStateOf(true) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
            TextField(
                state = query,
                placeholder = { Text(stringResource(Res.string.reactions__search)) },
                leadingIcon = { Icon(Icons.SearchW500Rounded, contentDescription = null) },
                trailingIcon = {
                    // Only the animated tab moves: elsewhere there is nothing to pause.
                    if (query.text.isEmpty() && tab == 0) PlayPauseButton(playing) { playing = it }
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.ScreenPadding),
            )
            val loaded = catalog
            if (loaded == null) {
                Box(Modifier.fillMaxWidth().height(GridHeight), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
                return@Column
            }
            val text = query.text.toString().trim()
            if (text.isEmpty()) GroupTabs(loaded, tab) { tab = it }
            val shown = when {
                text.isNotEmpty() -> loaded.all.filter { it.matches(text) }
                tab == 0 -> loaded.animated
                else -> loaded.groups[tab - 1].second
            }
            AnimatedContent(text.isEmpty() to tab, transitionSpec = { fadeIn() togetherWith fadeOut() }) {
                EmojiGrid(shown, animated = text.isEmpty() && tab == 0 && playing) {
                    onPick(it)
                    onDismiss()
                }
            }
        }
    }
}

/** Plays or pauses the animated emoji of the grid; paused, they show their still image. */
@Composable
private fun PlayPauseButton(playing: Boolean, onChange: (Boolean) -> Unit) = IconToggleButton(
    checked = playing,
    onCheckedChange = onChange,
    shapes = IconButtonDefaults.toggleableShapes(),
) {
    AnimatedContent(playing, transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() }) {
        if (it) {
            Icon(Icons.PauseCircleW500Rounded, contentDescription = stringResource(Res.string.reactions__pause))
        } else {
            Icon(Icons.PlayCircleW500Rounded, contentDescription = stringResource(Res.string.reactions__play))
        }
    }
}

/** The animated tab, then one tab per group, each shown by its first emoji. */
@Composable
private fun GroupTabs(catalog: EmojiCatalog, selected: Int, onSelect: (Int) -> Unit) =
    SecondaryScrollableTabRow(selectedTabIndex = selected, edgePadding = Dimens.ScreenPadding) {
        val animatedLabel = stringResource(Res.string.emoji__group_animated)
        Tab(
            selected = selected == 0,
            onClick = { onSelect(0) },
            icon = { Icon(Icons.StarShineW500Rounded, contentDescription = animatedLabel) },
        )
        catalog.groups.forEachIndexed { index, (group, emojis) ->
            val label = GroupLabels[group]?.let { stringResource(it) } ?: group
            Tab(
                selected = selected == index + 1,
                onClick = { onSelect(index + 1) },
                modifier = Modifier.semantics { contentDescription = label },
                icon = { emojis.firstOrNull()?.let { NotoImageEmoji(it, Modifier.size(TabEmojiSize)) } },
            )
        }
    }

@Composable
private fun EmojiGrid(emojis: List<Emoji>, animated: Boolean, onPick: (String) -> Unit) = LazyVerticalGrid(
    columns = GridCells.Adaptive(EmojiCellSize),
    modifier = Modifier.fillMaxWidth().height(GridHeight).padding(horizontal = Dimens.ItemGap),
) {
    items(emojis, key = { it.details.string }) { emoji ->
        Box(
            Modifier
                .size(EmojiCellSize)
                .clip(CircleShape)
                .clickable { onPick(emoji.details.string) }
                .semantics { contentDescription = emoji.details.description },
            contentAlignment = Alignment.Center,
        ) {
            if (animated) {
                NotoAnimatedEmoji(emoji, Modifier.size(AnimatedEmojiSize))
            } else {
                NotoImageEmoji(emoji, Modifier.size(AnimatedEmojiSize))
            }
        }
    }
}

private fun Emoji.matches(text: String) =
    details.description.contains(text, ignoreCase = true) || details.aliases.any { it.contains(text, ignoreCase = true) }

/**
 * A reaction's emoji drawn by Noto: its animation, played once, or its still image when Noto has none;
 * the plain character only for a string Emoji.kt does not know.
 */
@Composable
internal fun ReactionEmoji(type: String) {
    val service = EmojiService.get()
    // The server may keep or drop the emoji presentation selector (U+FE0F): either spelling is found.
    val emoji = remember(service, type) {
        service?.finder?.let { finder ->
            (finder.findEmoji(type).firstOrNull() ?: finder.findEmoji(type.replace(VariationSelector, "")).firstOrNull())
                ?.emoji
        }
    }
    when {
        emoji == null || !emoji.details.hasNotoImage -> Text(type)
        // Defers to the still Noto image for the emoji Noto did not animate.
        else -> NotoAnimatedEmoji(emoji, Modifier.size(ReactionEmojiSize), iterations = 1)
    }
}

private const val VariationSelector = "️"

private val GroupLabels: Map<String, StringResource> = mapOf(
    "smileys_emotion" to Res.string.emoji__group_smileys_emotion,
    "people_body" to Res.string.emoji__group_people_body,
    "animals_nature" to Res.string.emoji__group_animals_nature,
    "food_drink" to Res.string.emoji__group_food_drink,
    "travel_places" to Res.string.emoji__group_travel_places,
    "activities" to Res.string.emoji__group_activities,
    "objects" to Res.string.emoji__group_objects,
    "symbols" to Res.string.emoji__group_symbols,
    "flags" to Res.string.emoji__group_flags,
)
