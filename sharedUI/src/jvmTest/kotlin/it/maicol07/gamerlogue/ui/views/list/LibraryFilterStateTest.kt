package it.maicol07.gamerlogue.ui.views.list

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.LibraryEntrySchema.CompletionStatus
import it.maicol07.gamerlogue.extensions.currentUserEntries
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import kotlinx.datetime.LocalDate
import it.maicol07.spraypaintkt.SortDirection as ApiSortDirection

class LibraryFilterStateTest : StringSpec({
    "the default filter only sorts by last update" {
        val scope = LibraryEntry.currentUserEntries(GameLibraryStatus.PLAYING).applyFilter(LibraryFilterState())

        scope.filter shouldContainExactly mapOf("status" to "PLAYING")
        scope.sort shouldContainExactly mapOf("updated_at" to ApiSortDirection.DESC)
        scope.params.keys.filter { it.startsWith("filter[") } shouldBe emptyList()
    }

    "every filter maps to the backend's parameters" {
        val filter = LibraryFilterState(
            sortField = LibrarySortField.START_DATE,
            sortDirection = SortDirection.ASC,
            completionStatus = CompletionStatus.FULL_100,
            owned = false,
            minRating = 6f,
            minPlayedTime = 10,
            maxPlayedTime = 40,
            startDate = DateSpan(LocalDate(2025, 1, 1), LocalDate(2025, 12, 31)),
            endDate = DateSpan(null, LocalDate(2026, 1, 20)),
        )

        val scope = LibraryEntry.currentUserEntries(GameLibraryStatus.COMPLETED).applyFilter(filter)

        scope.sort shouldContainExactly mapOf("start_date" to ApiSortDirection.ASC)
        scope.filter shouldContainExactly mapOf(
            "status" to "COMPLETED",
            "completion_status" to "FULL_100",
            "owned" to "false",
        )
        scope.params.filterKeys { it.startsWith("filter[") } shouldContainExactly mapOf(
            "filter[rating][gte]" to "6",
            "filter[played_time][gte]" to "10",
            "filter[played_time][lte]" to "40",
            "filter[start_date][gte]" to "2025-01-01",
            "filter[start_date][lte]" to "2025-12-31",
            "filter[end_date][lte]" to "2026-01-20",
        )
        // A rating bound left at the end of the slider is not sent: it would drop unrated entries.
        scope.params shouldNotContainKey "filter[rating][lte]"
    }
})
