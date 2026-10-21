package it.maicol07.gamerlogue.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ReactionsTest : StringSpec({
    val like = ReactionSchema.LIKE

    "a first reaction adds its type" {
        CommentReactions(emptyList(), mine = null).withMine("🔥") shouldBe
            CommentReactions(listOf(ReactionCount("🔥", 1)), mine = "🔥")
    }

    "changing reaction moves the count and keeps the most used first" {
        val before = CommentReactions(listOf(ReactionCount(like, 2), ReactionCount("🔥", 2)), mine = like)

        before.withMine("🔥") shouldBe
            CommentReactions(listOf(ReactionCount("🔥", 3), ReactionCount(like, 1)), mine = "🔥")
    }

    "taking a reaction back drops a type left empty" {
        CommentReactions(listOf(ReactionCount("😂", 1)), mine = "😂").withMine(null) shouldBe
            CommentReactions(emptyList(), mine = null)
    }

    "the same reaction again changes nothing" {
        val reactions = CommentReactions(listOf(ReactionCount(like, 1)), mine = like)

        reactions.withMine(like) shouldBe reactions
    }
})
