package it.maicol07.gamerlogue.ui.views.list

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * [isActive] is what decides whether the list replays a Discover section's query or runs a filtered
 * one, and it is derived from the default instance — these cover that every kind of field
 * participates, so the derivation cannot silently stop covering one.
 */
@OptIn(ExperimentalTime::class)
class GameListFilterStateTest : StringSpec({
    "a pristine filter is not active" {
        GameListFilterState().isActive shouldBe false
        GameListFilterState().hasActiveFilters shouldBe false
    }

    "a search query activates the filter but not the filter button" {
        val state = GameListFilterState(searchQuery = "zelda")

        state.isActive shouldBe true
        // The query has its own visible affordance, so it must not light up the filter button.
        state.hasActiveFilters shouldBe false
    }

    "an id set activates the filter" {
        GameListFilterState(genreIds = setOf(12)).hasActiveFilters shouldBe true
    }

    "an enum choice activates the filter" {
        GameListFilterState(releaseStatus = ReleaseStatusFilter.UPCOMING).hasActiveFilters shouldBe true
        GameListFilterState(sortField = SortField.NAME).hasActiveFilters shouldBe true
        GameListFilterState(sortDirection = SortDirection.ASC).hasActiveFilters shouldBe true
    }

    "a narrowed range activates the filter" {
        GameListFilterState(minUserRating = 50f).hasActiveFilters shouldBe true
        GameListFilterState(maxCriticsRating = MaxRating - 1f).hasActiveFilters shouldBe true
        GameListFilterState(minReleaseYear = 2000).hasActiveFilters shouldBe true
        GameListFilterState(maxReleaseYear = MaxReleaseYear - 1).hasActiveFilters shouldBe true
    }

    "a per-company role activates the filter" {
        val state = GameListFilterState(
            companyIds = setOf(70),
            companyRoles = mapOf(70 to setOf(CompanyRole.DEVELOPER))
        )

        state.hasActiveFilters shouldBe true
    }

    "the time to beat filter is off at the full span" {
        GameListFilterState().hasTimeToBeatFilter shouldBe false
        GameListFilterState(minHoursToBeat = 5f).hasTimeToBeatFilter shouldBe true
        GameListFilterState(maxHoursToBeat = MaxHoursToBeat - 5f).hasTimeToBeatFilter shouldBe true
    }

    "an empty role map does not count as a filter" {
        // Selecting a company and clearing its roles again must not leave the filter looking active
        // while companiesClause() has nothing to add.
        GameListFilterState(companyRoles = emptyMap()).hasActiveFilters shouldBe false
    }

    "the release year range follows the clock" {
        MaxReleaseYear shouldBe Clock.System.now().toLocalDateTime(TimeZone.UTC).year + 1
    }
})
