package it.maicol07.gamerlogue.data

import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__completion_100
import gamerlogue.sharedui.generated.resources.library__completion_main_plus_sides
import gamerlogue.sharedui.generated.resources.library__completion_main_story
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.spraypaintkt_annotation.Attr
import it.maicol07.spraypaintkt_annotation.Relation
import it.maicol07.spraypaintkt_annotation.ResourceSchema
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource

@ResourceSchema(endpoint = "library_entries", resourceType = "LibraryEntry")
interface LibraryEntrySchema {
    @Attr val gameId: Int
    @Attr val status: GameLibraryStatus
    @Attr val completionStatus: CompletionStatus?
    @Attr val owned: Boolean
    @Attr val editionsIds: List<Int>
    @Attr val platformsIds: List<Int>
    @Attr val startDate: String?
    @Attr val endDate: String?
    @Attr val playedTime: Int? // in hours
    @Attr val rating: Number? // 0-10
    @Attr val ratingDetails: Map<*, Number>? // 0-10
    @Attr val review: String?
    @Attr val createdAt: String?
    @Attr val updatedAt: String?
    @Relation val user: UserSchema?

    enum class CompletionStatus(val displayName: StringResource) {
        MAIN_STORY(Res.string.library__completion_main_story),
        MAIN_PLUS_SIDES(Res.string.library__completion_main_plus_sides),
        FULL_100(Res.string.library__completion_100)
    }

    val startLocalDate: LocalDate?
        get() = startDate?.let(::parseCalendarDate)
    val endLocalDate: LocalDate?
        get() = endDate?.let(::parseCalendarDate)
}

/**
 * The calendar day of a date attribute. The backend sends date columns with a time and offset
 * (`2026-01-01T00:00:00+01:00`): only the date part is meaningful, and converting the whole value
 * to UTC would land on the day before.
 */
private fun parseCalendarDate(value: String): LocalDate? = LocalDate.Formats.ISO.parseOrNull(value.substringBefore('T'))
