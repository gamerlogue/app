package it.maicol07.gamerlogue.ui.views.list

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameListPresetTest {
    @Test
    fun fixedListPresetSetsItsFilterOnly() {
        val filter = GameListPreset(GameListPresetType.GENRE, 12, "RPG").applyTo(GameListFilterState())

        assertEquals(setOf(12), filter.genreIds)
        assertEquals(GameListFilterState().copy(genreIds = setOf(12)), filter)
    }

    @Test
    fun companyPresetCarriesItsRole() {
        val filter = GameListPreset(GameListPresetType.PUBLISHER, 7, "Nintendo").applyTo(GameListFilterState())

        assertEquals(setOf(7), filter.companyIds)
        assertEquals(mapOf(7 to setOf(CompanyRole.PUBLISHER)), filter.companyRoles)
    }

    @Test
    fun searchablePresetIsNamedInInitialState() {
        val state = GameListViewModel.UiState.from(GameListPreset(GameListPresetType.ENGINE, 3, "Unreal Engine"))

        assertTrue(state.filterState.hasActiveFilters)
        assertEquals("Unreal Engine", state.knownOptions[FilterSearchTarget.ENGINE]?.get(3)?.name)
    }
}
