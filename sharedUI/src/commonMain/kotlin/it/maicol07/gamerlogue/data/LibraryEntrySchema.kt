package it.maicol07.gamerlogue.data

import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__completion_100
import gamerlogue.sharedui.generated.resources.library__completion_main_plus_sides
import gamerlogue.sharedui.generated.resources.library__completion_main_story
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.spraypaintkt_annotation.Attr
import it.maicol07.spraypaintkt_annotation.Relation
import it.maicol07.spraypaintkt_annotation.ResourceSchema
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Instant

@ResourceSchema(endpoint = "library_entries")
interface LibraryEntrySchema {
    @Attr val gameId: Int
    @Attr val status: GameLibraryStatus
    @Attr val completionStatus: CompletionStatus?
    @Attr val owned: Boolean
    @Attr val editionId: Int?
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

    val startDateAsInstant: Instant?
        get() = startDate?.let { Instant.parseOrNull(it) }
    val endDateAsInstant: Instant?
        get() = endDate?.let { Instant.parseOrNull(it) }
}
