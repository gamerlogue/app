package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameCategoryEnum
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.game__editions_title
import gamerlogue.sharedui.generated.resources.game__variant_pick_hint
import gamerlogue.sharedui.generated.resources.game_category__main_game
import gamerlogue.sharedui.generated.resources.game_category__unknown
import it.maicol07.gamerlogue.extensions.igdb.gameCategoryStringResource
import it.maicol07.gamerlogue.services.GameMatcher
import it.maicol07.gamerlogue.ui.components.HintCard
import it.maicol07.gamerlogue.ui.views.library.components.EditionCover
import it.maicol07.gamerlogue.ui.views.settings.components.SettingsGroupHeader
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * What a tap on a game does when one game is picked with its editions, DLCs and expansions in mind: the
 * [GameVariantSheet] opens right away, loading them, and asks which one is meant; a game with none is handed to
 * [onPicked] as soon as the lookup says so.
 */
@Composable
internal fun rememberVariantPick(onPicked: (gameId: Int) -> Unit): (Game) -> Unit {
    val matcher = koinInject<GameMatcher>()
    val scope = rememberCoroutineScope()
    // The tapped game and its variants, null while they load.
    var shown by remember { mutableStateOf<Pair<Game, List<Game>?>?>(null) }
    var lookup by remember { mutableStateOf<Job?>(null) }
    shown?.let { (game, variants) ->
        GameVariantSheet(
            game = game,
            variants = variants,
            onPick = {
                shown = null
                onPicked(it.id.toInt())
            },
            onDismissRequest = {
                lookup?.cancel()
                shown = null
            },
        )
    }
    return { game ->
        // The open sheet covers the list, so a second tap can't start another lookup.
        shown = game to null
        lookup = scope.launch {
            val variants = matcher.variantsOf(game.id.toInt())
            if (variants.isEmpty()) {
                shown = null
                onPicked(game.id.toInt())
            } else {
                shown = game to variants
            }
        }
    }
}

/** Which of [game], its editions or its DLCs/expansions ([variants], null while loading) a pick means. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameVariantSheet(
    game: Game,
    variants: List<Game>?,
    onPick: (Game) -> Unit,
    onDismissRequest: () -> Unit,
) = ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = rememberBottomSheetState(
        SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    ),
) {
    if (variants == null) {
        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
        return@ModalBottomSheet
    }
    val (editions, others) = variants.partition { it.version_parent != null }
    // DLCs, expansions, expanded games, remasters…: each under its own type, in IGDB's type order.
    val othersByType = others
        .groupBy { variant -> GameCategoryEnum.entries.firstOrNull { it.value.toLong() == variant.game_type?.id } }
        .entries.sortedBy { it.key?.ordinal ?: Int.MAX_VALUE }
        .map { (type, games) ->
            (type?.let { gameCategoryStringResource(it.name) } ?: Res.string.game_category__unknown) to
                games.map { it to it.name.withoutPrefix(game.name) }
        }
    LazyColumn(contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            Text(
                game.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
        }
        item {
            HintCard(
                stringResource(Res.string.game__variant_pick_hint),
                Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
            )
        }
        listOf(
            Res.string.game_category__main_game to listOf(game to game.name),
            Res.string.game__editions_title to editions.map { it to it.version_title.ifEmpty { it.name } },
        ).plus(othersByType).filter { it.second.isNotEmpty() }.forEach { (title, games) ->
            item(key = title.key) {
                Column {
                    SettingsGroupHeader(stringResource(title))
                    // Plain horizontalScroll, like the add-to-library editions: a handful of covers.
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        games.forEach { (option, label) ->
                            EditionCover(option, label, selected = false, onClick = { onPick(option) })
                        }
                    }
                }
            }
        }
    }
}

/** A DLC's own part of its name, which usually repeats its game's ("Game: DLC" → "DLC"). */
private fun String.withoutPrefix(gameName: String): String =
    removePrefix(gameName).trimStart(' ', ':', '-', '–', '—').ifEmpty { this }
