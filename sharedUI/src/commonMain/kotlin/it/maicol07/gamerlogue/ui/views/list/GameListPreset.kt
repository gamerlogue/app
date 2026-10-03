package it.maicol07.gamerlogue.ui.views.list

import kotlinx.serialization.Serializable

/** The game metadata a list can be opened pre-filtered on, each mapping to one filter of [GameListFilterState]. */
@Serializable
enum class GameListPresetType {
    PLATFORM,
    PLAYER_PERSPECTIVE,
    GENRE,
    THEME,
    GAME_MODE,
    DEVELOPER,
    PUBLISHER,
    FRANCHISE,
    ENGINE,
    KEYWORD,
}

/**
 * A single filter the game list opens with, e.g. "genre 12, Role-playing" when a genre is tapped
 * on a game page. Small and serializable on purpose: it travels in the navigation key, and [name]
 * labels the list and the filter chip without a lookup.
 */
@Serializable
data class GameListPreset(val type: GameListPresetType, val id: Int, val name: String)

/** [filter] with this preset applied on top. */
fun GameListPreset.applyTo(filter: GameListFilterState): GameListFilterState = when (type) {
    GameListPresetType.PLATFORM -> filter.copy(platformIds = filter.platformIds + id)
    GameListPresetType.PLAYER_PERSPECTIVE -> filter.copy(playerPerspectiveIds = filter.playerPerspectiveIds + id)
    GameListPresetType.GENRE -> filter.copy(genreIds = filter.genreIds + id)
    GameListPresetType.THEME -> filter.copy(themeIds = filter.themeIds + id)
    GameListPresetType.GAME_MODE -> filter.copy(gameModeIds = filter.gameModeIds + id)
    GameListPresetType.DEVELOPER -> filter.withCompany(id, CompanyRole.DEVELOPER)
    GameListPresetType.PUBLISHER -> filter.withCompany(id, CompanyRole.PUBLISHER)
    GameListPresetType.FRANCHISE -> filter.copy(franchiseIds = filter.franchiseIds + id)
    GameListPresetType.ENGINE -> filter.copy(gameEngineIds = filter.gameEngineIds + id)
    GameListPresetType.KEYWORD -> filter.copy(keywordIds = filter.keywordIds + id)
}

/** The searchable filter section the preset belongs to, whose chip needs [GameListPreset.name]; null for fixed lists. */
val GameListPreset.searchTarget: FilterSearchTarget?
    get() = when (type) {
        GameListPresetType.DEVELOPER, GameListPresetType.PUBLISHER -> FilterSearchTarget.COMPANY
        GameListPresetType.FRANCHISE -> FilterSearchTarget.FRANCHISE
        GameListPresetType.ENGINE -> FilterSearchTarget.ENGINE
        GameListPresetType.KEYWORD -> FilterSearchTarget.KEYWORD
        else -> null
    }

private fun GameListFilterState.withCompany(id: Int, role: CompanyRole) = copy(
    companyIds = companyIds + id,
    companyRoles = companyRoles + (id to (companyRoles[id].orEmpty() + role))
)
