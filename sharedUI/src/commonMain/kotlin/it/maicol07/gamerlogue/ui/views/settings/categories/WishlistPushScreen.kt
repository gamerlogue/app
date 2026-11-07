package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.IgdbImageSize
import at.released.igdbclient.util.igdbImageUrl
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__import_deselect_all
import gamerlogue.sharedui.generated.resources.settings__import_no_match
import gamerlogue.sharedui.generated.resources.settings__import_select_all
import gamerlogue.sharedui.generated.resources.settings__import_selected
import gamerlogue.sharedui.generated.resources.settings__open_store
import gamerlogue.sharedui.generated.resources.settings__wishlist_already_present
import gamerlogue.sharedui.generated.resources.settings__wishlist_choose_platform
import gamerlogue.sharedui.generated.resources.settings__wishlist_group_already_present
import gamerlogue.sharedui.generated.resources.settings__wishlist_group_choose_platform
import gamerlogue.sharedui.generated.resources.settings__wishlist_group_no_match
import gamerlogue.sharedui.generated.resources.settings__wishlist_group_to_add
import gamerlogue.sharedui.generated.resources.settings__wishlist_no_platform
import gamerlogue.sharedui.generated.resources.settings__wishlist_platform_for_all
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_confirm
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_off_platform
import gamerlogue.sharedui.generated.resources.settings__wishlist_push_skip
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.ui.components.RemoteImage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Outgoing-direction preview: pick which backlog games to add to the store wishlist, in collapsible sections. */
// Platform choices, sections and selection share the same state.
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun WishlistPushScreen(
    games: List<LibrarySync.OutgoingGame>,
    requiresPlatform: Boolean,
    matchesByName: Boolean,
    onConfirm: (List<LibrarySync.OutgoingGame>) -> Unit,
    onSkip: () -> Unit,
) {
    // A by-name push needs a publisher match when IGDB has no store URL.

    val platforms = remember(games) {
        mutableStateMapOf<String, Int>().apply {
            games.forEach { game ->
                game.defaultPlatformId?.let { put(game.uid, it) }
            }
        }
    }
    val resolved = if (requiresPlatform) {
        games.map { it.copy(
            platformIds = listOfNotNull(platforms[it.uid])
        ) }
    } else {
        games
    }
    val selected = remember(games) {
        mutableStateMapOf<String, Boolean>().apply {
            resolved.forEach { put(it.uid, it.isPushable(matchesByName, requiresPlatform)) }
        }
    }
    // Sections are taken once, on open: moving a game when its platform is picked would move it under the finger.
    val sectionOf = remember(games) { resolved.associate { it.uid to it.pushSection(matchesByName, requiresPlatform) } }
    val bySection = resolved.groupBy { sectionOf.getValue(it.uid) }
    val expanded = remember(games) { mutableStateMapOf<PushSection, Boolean>() }
    val pushable = resolved.filter { it.isPushable(matchesByName, requiresPlatform) }
    val selectedCount = pushable.count { selected[it.uid] == true }
    val allSelected = pushable.isNotEmpty() && selectedCount == pushable.size
    val uriHandler = LocalUriHandler.current

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                pluralStringResource(
                    Res.plurals.settings__import_selected,
                    selectedCount,
                    selectedCount,
                    pushable.size
                ),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { pushable.forEach { selected[it.uid] = !allSelected } },
                enabled = pushable.isNotEmpty(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(
                    stringResource(
                        if (allSelected) {
                            Res.string.settings__import_deselect_all
                        } else {
                            Res.string.settings__import_select_all
                        }
                    )
                )
            }
        }
        BulkPlatformButton(
            bySection[PushSection.CHOOSE_PLATFORM].orEmpty().filter { it.platformIds.isEmpty() },
        ) { id, games ->
            games.forEach {
                platforms[it.uid] = id
                selected[it.uid] = true
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            PushSection.entries.forEach { section ->
                val rows = bySection[section].orEmpty()
                val open = expanded[section] ?: section.startsExpanded
                collapsibleSection(
                    key = "section-${section.name}",
                    rows = rows,
                    rowKey = { it.gameId },
                    expanded = open,
                    header = { shapes, modifier ->
                        SectionHeader(
                            title = stringResource(section.title, rows.size),
                            icon = null,
                            supporting = null,
                            expanded = open,
                            shapes = shapes,
                            onToggle = { expanded[section] = !open },
                            modifier = modifier,
                        )
                    },
                ) { game, position, count ->
                    PushRow(
                        game,
                        position,
                        count,
                        selected[game.uid] == true,
                        matchesByName,
                        uriHandler::openURL,
                        onSelectPlatform = if (requiresPlatform && !game.alreadyOnWishlist) {
                            { id ->
                                platforms[game.uid] = id
                                selected[game.uid] = true
                            }
                        } else {
                            null
                        },
                        onCheckedChange = { selected[game.uid] = it },
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val height = ButtonDefaults.MediumContainerHeight
            val leading = ButtonGroupDefaults.connectedLeadingButtonShapes()
            val trailing = ButtonGroupDefaults.connectedTrailingButtonShapes()
            FilledTonalButton(
                onClick = onSkip,
                shapes = ButtonDefaults.shapes(leading.shape, leading.pressedShape),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier.weight(1f).heightIn(min = height),
            ) {
                Text(
                    stringResource(Res.string.settings__wishlist_push_skip),
                    style = ButtonDefaults.textStyleFor(height)
                )
            }
            Button(
                onClick = { onConfirm(pushable.filter { selected[it.uid] == true }) },
                enabled = selectedCount > 0,
                shapes = ButtonDefaults.shapes(trailing.shape, trailing.pressedShape),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
                modifier = Modifier.weight(1f).heightIn(min = height),
            ) {
                Text(
                    stringResource(Res.string.settings__wishlist_push_confirm),
                    style = ButtonDefaults.textStyleFor(height)
                )
            }
        }
    }
}

/**
 * Sets one platform on many [unresolved] games at once: the menu lists their platforms, most common first, and
 * [onPick] gets the picked platform with the games that release on it. Hidden when there is nothing to pick.
 */
@Composable
private fun BulkPlatformButton(
    unresolved: List<LibrarySync.OutgoingGame>,
    onPick: (platformId: Int, games: List<LibrarySync.OutgoingGame>) -> Unit,
) {
    val options = unresolved
        .flatMap { game -> game.platformNames.filterValues { it.isNotEmpty() }.entries }
        .groupBy({ it.key }, { it.value.first() })
        .entries.sortedByDescending { it.value.size }
    if (options.isEmpty()) return
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
        FilledTonalButton(
            onClick = { menu = true },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(Res.string.settings__wishlist_platform_for_all)) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            options.forEach { (id, names) ->
                DropdownMenuItem(
                    text = { Text(names.first()) },
                    trailingIcon = { Text(names.size.toString()) },
                    onClick = {
                        onPick(id, unresolved.filter { it.platformNames[id].orEmpty().isNotEmpty() })
                        menu = false
                    },
                )
            }
        }
    }
}

/** The preview's sections, in display order; the ones with nothing to do start collapsed. */
private enum class PushSection(val title: StringResource, val startsExpanded: Boolean) {
    TO_ADD(Res.string.settings__wishlist_group_to_add, true),
    CHOOSE_PLATFORM(Res.string.settings__wishlist_group_choose_platform, true),
    NO_MATCH(Res.string.settings__wishlist_group_no_match, true),
    ALREADY_PRESENT(Res.string.settings__wishlist_group_already_present, false),
    OFF_PLATFORM(Res.string.settings__wishlist_push_off_platform, false),
}

private fun LibrarySync.OutgoingGame.pushSection(matchesByName: Boolean, requiresPlatform: Boolean) = when {
    !onPlatform -> PushSection.OFF_PLATFORM
    alreadyOnWishlist -> PushSection.ALREADY_PRESENT
    !hasPushTarget(matchesByName) -> PushSection.NO_MATCH
    requiresPlatform && platformIds.isEmpty() -> PushSection.CHOOSE_PLATFORM
    else -> PushSection.TO_ADD
}

@Composable
private fun PushRow(
    game: LibrarySync.OutgoingGame,
    index: Int,
    count: Int,
    checked: Boolean,
    matchesByName: Boolean,
    onOpenStore: (String) -> Unit,
    onSelectPlatform: ((Int) -> Unit)?,
    onCheckedChange: (Boolean) -> Unit,
) {
    var platformMenu by remember(game.uid) { mutableStateOf(false) }
    val platformOptions = game.platformNames.filterValues { it.isNotEmpty() }
    SegmentedListItem(
        selected = checked,
        enabled = game.isPushable(matchesByName, requiresPlatform = false),
        onClick = {
            if (onSelectPlatform != null && game.platformIds.isEmpty()) {
                platformMenu = true
            } else {
                onCheckedChange(!checked)
            }
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = ListItemDefaults.expressiveSegmentedColors(),
        leadingContent = {
            val cover = game.coverImageId
            if (cover != null) {
                val coverModifier = Modifier.size(width = 40.dp, height = 53.dp).clip(MaterialTheme.shapes.medium)
                RemoteImage(
                    url = igdbImageUrl(cover, IgdbImageSize.COVER_SMALL),
                    contentDescription = game.name,
                    modifier = coverModifier,
                    loadingModifier = coverModifier,
                )
            }
        },
        trailingContent = {
            if (onSelectPlatform != null) {
                Box {
                    TextButton(
                        onClick = { platformMenu = true },
                        enabled = platformOptions.isNotEmpty(),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text(
                            game.platformIds.firstOrNull()?.let { platformOptions[it]?.lastOrNull() }
                                ?: stringResource(Res.string.settings__wishlist_choose_platform)
                        )
                    }
                    DropdownMenu(expanded = platformMenu, onDismissRequest = { platformMenu = false }) {
                        platformOptions.forEach { (id, names) ->
                            DropdownMenuItem(
                                text = { Text(names.first()) },
                                onClick = {
                                    onSelectPlatform(id);
                                    platformMenu = false
                                },
                            )
                        }
                    }
                }
            } else {
                game.storeUrl?.let { url ->
                    IconButton(onClick = { onOpenStore(url) }, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            Icons.OpenInNewW500Rounded,
                            contentDescription = stringResource(Res.string.settings__open_store)
                        )
                    }
                }
            }
        },
        supportingContent = {
            val subtitle = when {
                game.alreadyOnWishlist -> stringResource(Res.string.settings__wishlist_already_present)
                game.onPlatform && !game.hasPushTarget(matchesByName) -> stringResource(
                    Res.string.settings__import_no_match
                )
                onSelectPlatform != null && platformOptions.isEmpty() -> stringResource(
                    Res.string.settings__wishlist_no_platform
                )
                onSelectPlatform != null && game.platformIds.isEmpty() -> stringResource(
                    Res.string.settings__wishlist_choose_platform
                )

                else -> null
            }
            subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
    ) { Text(game.name) }
}

/** A store page, or — for connectors that push by searching the store's title — a confirmed publisher match. */
private fun LibrarySync.OutgoingGame.hasPushTarget(matchesByName: Boolean) =
    storeUrl != null || (matchesByName && matchesPublisher)

private fun LibrarySync.OutgoingGame.isPushable(matchesByName: Boolean, requiresPlatform: Boolean) =
    hasPushTarget(matchesByName) && onPlatform && !alreadyOnWishlist && (!requiresPlatform || platformIds.isNotEmpty())
