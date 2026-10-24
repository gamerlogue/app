package it.maicol07.gamerlogue.data

import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.review_aspect__audio
import gamerlogue.sharedui.generated.resources.review_aspect__cast
import gamerlogue.sharedui.generated.resources.review_aspect__gameplay
import gamerlogue.sharedui.generated.resources.review_aspect__longevity
import gamerlogue.sharedui.generated.resources.review_aspect__story
import gamerlogue.sharedui.generated.resources.review_aspect__technical
import org.jetbrains.compose.resources.StringResource

/** A predefined part of a detailed review; [key] is what `rating_details` stores. */
enum class ReviewAspect(val label: StringResource) {
    GAMEPLAY(Res.string.review_aspect__gameplay),
    STORY(Res.string.review_aspect__story),
    CAST(Res.string.review_aspect__cast),
    TECHNICAL(Res.string.review_aspect__technical),
    AUDIO(Res.string.review_aspect__audio),
    LONGEVITY(Res.string.review_aspect__longevity);

    val key: String = name.lowercase()

    companion object {
        fun of(key: String): ReviewAspect? = entries.find { it.key == key }
    }
}

/** One part of a detailed review: [key] is a [ReviewAspect.key] or a title the user wrote. */
data class ReviewSection(val key: String, val rating: Double?, val review: String)
