package it.maicol07.gamerlogue.data

/** One reaction type on a comment (`like` or an emoji) with how many users chose it. */
data class ReactionCount(val type: String, val count: Int)

/** A comment's reactions as shown: the summary and the current user's own type, if any. */
data class CommentReactions(val summary: List<ReactionCount>, val mine: String?)

/** [CommentSchema.reactionsSummary] parsed: the backend sends `[{type, count}]`, most used first. */
val CommentSchema.reactions: CommentReactions
    get() = CommentReactions(
        summary = reactionsSummary.mapNotNull { entry ->
            (entry["type"] as? String)?.let { ReactionCount(it, (entry["count"] as? Number)?.toInt() ?: 0) }
        },
        mine = myReaction,
    )

/**
 * These reactions after the current user's moved to [type] (null takes it back): the old type loses
 * one, the new one gains one, empty types disappear and the most used stay first.
 */
fun CommentReactions.withMine(type: String?): CommentReactions {
    if (type == mine) return this
    val counts = summary.associate { it.type to it.count }.toMutableMap()
    mine?.let { counts[it] = (counts[it] ?: 1) - 1 }
    type?.let { counts[it] = (counts[it] ?: 0) + 1 }
    val updated = counts.filterValues { it > 0 }.map { (reaction, count) -> ReactionCount(reaction, count) }
    return CommentReactions(updated.sortedByDescending { it.count }, type)
}

/** A like as shown: whether the current user likes it and the count, overriding the loaded resource. */
data class LikeState(val liked: Boolean, val count: Int)
