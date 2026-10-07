package it.maicol07.gamerlogue.ui.views.list

import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.data.LibraryEntrySchema.CompletionStatus
import it.maicol07.spraypaintkt.Scope
import kotlinx.datetime.LocalDate
import it.maicol07.spraypaintkt.SortDirection as ApiSortDirection

/** Upper bound of a library entry's rating. */
const val MaxEntryRating = 10f

/** A library entry attribute the backend sorts by, under its snake_case [attribute] name. */
enum class LibrarySortField(val attribute: String) {
    UPDATED_AT("updated_at"),
    CREATED_AT("created_at"),
    RATING("rating"),
    PLAYED_TIME("played_time"),
    START_DATE("start_date"),
    END_DATE("end_date"),
}

/** A span of calendar days; a null side is open. */
data class DateSpan(val from: LocalDate?, val to: LocalDate?)

/**
 * The library entry filters and sort of the library scope, sent to the backend; the IGDB filters of
 * [GameListFilterState] still apply on top of the games it returns. Null means "any".
 */
data class LibraryFilterState(
    val sortField: LibrarySortField = LibrarySortField.UPDATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC,
    val completionStatus: CompletionStatus? = null,
    val owned: Boolean? = null,
    val minRating: Float = 0f,
    val maxRating: Float = MaxEntryRating,
    val minPlayedTime: Int? = null,
    val maxPlayedTime: Int? = null,
    val startDate: DateSpan? = null,
    val endDate: DateSpan? = null,
)

/** True when the rating span is narrower than the full 0..[MaxEntryRating] slider. */
val LibraryFilterState.hasRatingFilter: Boolean
    get() = minRating > 0f || maxRating < MaxEntryRating

/** [filter] as the backend's `filter[…]` and `sort` parameters. */
fun Scope<LibraryEntry>.applyFilter(filter: LibraryFilterState): Scope<LibraryEntry> = apply {
    order(
        filter.sortField.attribute,
        if (filter.sortDirection == SortDirection.DESC) ApiSortDirection.DESC else ApiSortDirection.ASC
    )
    filter.completionStatus?.let { where("completion_status", it.name) }
    filter.owned?.let { where("owned", it) }
    if (filter.minRating > 0f) range("rating", "gte", filter.minRating.toInt())
    if (filter.maxRating < MaxEntryRating) range("rating", "lte", filter.maxRating.toInt())
    filter.minPlayedTime?.let { range("played_time", "gte", it) }
    filter.maxPlayedTime?.let { range("played_time", "lte", it) }
    filter.startDate?.let { dateSpan("start_date", it) }
    filter.endDate?.let { dateSpan("end_date", it) }
}

/** `filter[attribute][operator]`: spraypaintkt's `where` only builds the operator-less form. */
private fun Scope<LibraryEntry>.range(attribute: String, operator: String, value: Any) {
    extraParam("filter[$attribute][$operator]", value.toString())
}

/** Dates go as `yyyy-MM-dd`: the backend compares them by day. */
private fun Scope<LibraryEntry>.dateSpan(attribute: String, span: DateSpan) {
    span.from?.let { range(attribute, "gte", it) }
    span.to?.let { range(attribute, "lte", it) }
}
