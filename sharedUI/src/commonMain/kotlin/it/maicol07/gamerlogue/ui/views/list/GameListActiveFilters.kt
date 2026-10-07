package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.GameCategoryEnum
import at.released.igdbclient.model.GameMode
import at.released.igdbclient.model.GameStatusEnum
import at.released.igdbclient.model.Genre
import at.released.igdbclient.model.PlayerPerspective
import at.released.igdbclient.model.Theme
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.gamelist__critics_rating
import gamerlogue.sharedui.generated.resources.gamelist__filter_time_to_beat
import gamerlogue.sharedui.generated.resources.gamelist__hours_range
import gamerlogue.sharedui.generated.resources.gamelist__hours_range_open
import gamerlogue.sharedui.generated.resources.gamelist__my_rating
import gamerlogue.sharedui.generated.resources.gamelist__release_year
import gamerlogue.sharedui.generated.resources.gamelist__remove_filter
import gamerlogue.sharedui.generated.resources.gamelist__reset
import gamerlogue.sharedui.generated.resources.gamelist__user_rating
import gamerlogue.sharedui.generated.resources.library__end_date
import gamerlogue.sharedui.generated.resources.library__played_time
import gamerlogue.sharedui.generated.resources.library__start_date
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.RefreshW500Rounded
import it.maicol07.gamerlogue.extensions.igdb.localizedName
import it.maicol07.gamerlogue.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

private val ChipGap = 8.dp

/** One removable chip: its stable key, what it shows and what removing it does to the filter. */
private data class ActiveFilterChip(
    val key: String,
    val label: String,
    val remove: GameListFilterState.() -> GameListFilterState,
)

/**
 * A horizontally scrolling row with one chip per active filter, so what narrows the list stays
 * visible without opening the sheet. Tapping a chip removes that filter alone; with more than one
 * chip, a leading reset chip clears them all.
 */
@Composable
fun ActiveFilterChips(
    filterState: GameListFilterState,
    knownOptions: Map<FilterSearchTarget, Map<Int, NamedSearchResult>>,
    presetPlatform: GameListPreset?,
    onFilterChange: (GameListFilterState) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chips = sortChips(filterState) + libraryChips(filterState) + rangeChips(filterState) +
        fixedOptionChips(filterState, presetPlatform) + searchedOptionChips(filterState, knownOptions)
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(ChipGap)
    ) {
        if (chips.size > 1) {
            item(key = "reset") {
                AssistChip(
                    onClick = onReset,
                    label = { Text(stringResource(Res.string.gamelist__reset)) },
                    leadingIcon = {
                        Icon(
                            Icons.RefreshW500Rounded,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
        items(chips, key = ActiveFilterChip::key) { chip ->
            InputChip(
                selected = true,
                onClick = { onFilterChange(chip.remove(filterState)) },
                label = { Text(chip.label) },
                trailingIcon = {
                    Icon(
                        Icons.CloseW500Rounded,
                        contentDescription = stringResource(Res.string.gamelist__remove_filter, chip.label),
                        modifier = Modifier.size(InputChipDefaults.IconSize)
                    )
                },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun sortChips(filter: GameListFilterState): List<ActiveFilterChip> {
    if (filter.sortField == DefaultFilterState.sortField && filter.sortDirection == DefaultFilterState.sortDirection) {
        return emptyList()
    }
    val arrow = if (filter.sortDirection == SortDirection.DESC) "↓" else "↑"
    return listOf(
        ActiveFilterChip("sort", "${stringResource(filter.sortField.label)} $arrow") {
            copy(sortField = DefaultFilterState.sortField, sortDirection = DefaultFilterState.sortDirection)
        }
    )
}

/** The library scope's sort and entry filters; all at their defaults outside that scope. */
@Composable
private fun libraryChips(filter: GameListFilterState): List<ActiveFilterChip> = with(filter.library) {
    val default = LibraryFilterState()
    fun chip(key: String, label: String, reset: LibraryFilterState.() -> LibraryFilterState) =
        ActiveFilterChip("library:$key", label) { copy(library = library.reset()) }

    val arrow = if (sortDirection == SortDirection.DESC) "↓" else "↑"
    listOfNotNull(
        chip("sort", "${stringResource(sortField.label)} $arrow") {
            copy(sortField = default.sortField, sortDirection = default.sortDirection)
        }.takeIf { sortField != default.sortField || sortDirection != default.sortDirection },
        completionStatus?.let { chip("completion", stringResource(it.displayName)) { copy(completionStatus = null) } },
        owned?.let { chip("owned", stringResource(it.ownedLabel)) { copy(owned = null) } },
        chip("rating", "${stringResource(Res.string.gamelist__my_rating)} ${minRating.toInt()}–${maxRating.toInt()}") {
            copy(minRating = 0f, maxRating = MaxEntryRating)
        }.takeIf { hasRatingFilter },
        playedTimeLabel()?.let { hours ->
            chip("playedTime", "${stringResource(Res.string.library__played_time)} $hours") {
                copy(minPlayedTime = null, maxPlayedTime = null)
            }
        },
        startDate?.let { span ->
            chip("startDate", "${stringResource(Res.string.library__start_date)} ${span.label()}") {
                copy(startDate = null)
            }
        },
        endDate?.let { span ->
            chip("endDate", "${stringResource(Res.string.library__end_date)} ${span.label()}") { copy(endDate = null) }
        },
    )
}

@Composable
private fun rangeChips(filter: GameListFilterState): List<ActiveFilterChip> = with(filter) {
    val userRating = stringResource(Res.string.gamelist__user_rating) +
        " ${minUserRating.toInt()}–${maxUserRating.toInt()}"
    val criticsRating = stringResource(Res.string.gamelist__critics_rating) +
        " ${minCriticsRating.toInt()}–${maxCriticsRating.toInt()}"
    val releaseYear = "${stringResource(Res.string.gamelist__release_year)} $minReleaseYear–$maxReleaseYear"
    val timeToBeat = "${stringResource(Res.string.gamelist__filter_time_to_beat)} ${hoursLabel()}"
    listOfNotNull(
        ActiveFilterChip("userRating", userRating) { copy(minUserRating = 0f, maxUserRating = MaxRating) }
            .takeIf { minUserRating > 0f || maxUserRating < MaxRating },
        ActiveFilterChip("criticsRating", criticsRating) { copy(minCriticsRating = 0f, maxCriticsRating = MaxRating) }
            .takeIf { minCriticsRating > 0f || maxCriticsRating < MaxRating },
        ActiveFilterChip("releaseYear", releaseYear) {
            copy(minReleaseYear = MinReleaseYear, maxReleaseYear = MaxReleaseYear)
        }.takeIf { minReleaseYear > MinReleaseYear || maxReleaseYear < MaxReleaseYear },
        ActiveFilterChip("timeToBeat", timeToBeat) { copy(minHoursToBeat = 0f, maxHoursToBeat = MaxHoursToBeat) }
            .takeIf { hasTimeToBeatFilter },
        ActiveFilterChip("releaseStatus", stringResource(releaseStatus.label)) {
            copy(releaseStatus = ReleaseStatusFilter.ALL)
        }.takeIf { releaseStatus != ReleaseStatusFilter.ALL },
    )
}

@Composable
private fun GameListFilterState.hoursLabel(): String = if (maxHoursToBeat >= MaxHoursToBeat) {
    stringResource(Res.string.gamelist__hours_range_open, minHoursToBeat.toInt())
} else {
    stringResource(Res.string.gamelist__hours_range, minHoursToBeat.toInt(), maxHoursToBeat.toInt())
}

/**
 * One chip per option picked from the sheet's fixed option sets; enums are stored by ordinal.
 * [presetPlatform] names a platform the list was opened on that is not among the popular ones.
 */
@Composable
private fun fixedOptionChips(
    filter: GameListFilterState,
    presetPlatform: GameListPreset?
): List<ActiveFilterChip> = with(filter) {
    platformIds.map { id ->
        val name = PopularPlatforms.firstOrNull { it.id == id }?.name
            ?: presetPlatform?.takeIf { it.id == id }?.name
            ?: id.toString()
        ActiveFilterChip("platform:$id", name) { copy(platformIds = platformIds - id) }
    } + playerPerspectiveIds.map { id ->
        ActiveFilterChip("perspective:$id", PlayerPerspective(id.toLong()).localizedName) {
            copy(playerPerspectiveIds = playerPerspectiveIds - id)
        }
    } + categoryIds.map { id ->
        ActiveFilterChip("category:$id", GameCategoryEnum.entries[id].localizedName) {
            copy(categoryIds = categoryIds - id)
        }
    } + statusIds.map { id ->
        ActiveFilterChip("status:$id", GameStatusEnum.entries[id].localizedName) { copy(statusIds = statusIds - id) }
    } + genreIds.map { id ->
        ActiveFilterChip("genre:$id", Genre(id.toLong()).localizedName) { copy(genreIds = genreIds - id) }
    } + themeIds.map { id ->
        ActiveFilterChip("theme:$id", Theme(id.toLong()).localizedName) { copy(themeIds = themeIds - id) }
    } + gameModeIds.map { id ->
        ActiveFilterChip("mode:$id", GameMode(id.toLong()).localizedName) { copy(gameModeIds = gameModeIds - id) }
    }
}

/**
 * One chip per option picked from a searchable section. Names come from [knownOptions], which holds
 * every option the sheet ever showed; the id is only a fallback for a selection it somehow lacks.
 */
private fun searchedOptionChips(
    filter: GameListFilterState,
    knownOptions: Map<FilterSearchTarget, Map<Int, NamedSearchResult>>,
): List<ActiveFilterChip> = with(filter) {
    fun nameOf(target: FilterSearchTarget, id: Int) = knownOptions[target]?.get(id)?.name ?: id.toString()

    companyIds.map { id ->
        // Removing a company drops its roles too, as deselecting it in the sheet does.
        ActiveFilterChip("company:$id", nameOf(FilterSearchTarget.COMPANY, id)) {
            copy(companyIds = companyIds - id, companyRoles = companyRoles - id)
        }
    } + franchiseIds.map { id ->
        ActiveFilterChip("franchise:$id", nameOf(FilterSearchTarget.FRANCHISE, id)) {
            copy(franchiseIds = franchiseIds - id)
        }
    } + gameEngineIds.map { id ->
        ActiveFilterChip("engine:$id", nameOf(FilterSearchTarget.ENGINE, id)) {
            copy(gameEngineIds = gameEngineIds - id)
        }
    } + keywordIds.map { id ->
        ActiveFilterChip("keyword:$id", nameOf(FilterSearchTarget.KEYWORD, id)) { copy(keywordIds = keywordIds - id) }
    }
}
