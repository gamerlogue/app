package it.maicol07.gamerlogue.data

import it.maicol07.spraypaintkt_annotation.Attr
import it.maicol07.spraypaintkt_annotation.Relation
import it.maicol07.spraypaintkt_annotation.ResourceSchema

/**
 * A comment on a review ([InteractionTarget.REVIEW], a library entry), a list or another comment: a
 * reply is a comment on a comment, at any depth. Deleting one deletes its whole subtree.
 */
@ResourceSchema(endpoint = "comments", resourceType = "Comment")
interface CommentSchema {
    // Renamed from the wire `comment`, which reads as the resource itself.
    @Attr("comment")
    val text: String?
    @Attr val spoiler: Boolean
        get() = false
    @Attr val commentableType: String
    @Attr val commentableId: Int
    @Attr val userId: String?
    @Attr val edited: Boolean
        get() = false
    @Attr val repliesCount: Int
        get() = 0

    /** Each reaction type (`like` or an emoji) with its count, the most used first. */
    @Attr val reactionsSummary: List<Map<String, Any?>>
        get() = emptyList()

    /** The current user's reaction type, if any. */
    @Attr val myReaction: String?
    @Attr val createdAt: String?
    @Attr val updatedAt: String?
    @Relation val user: UserSchema?

    companion object {
        const val MAX_TEXT_LENGTH = 2000
    }
}

/**
 * What a reaction, a comment or a report can point at, with its backend type name. Reactions and
 * comments go on [REVIEW], [LIST] and [COMMENT]; a [USER] can only be reported.
 */
enum class InteractionTarget(val apiType: String) {
    REVIEW("library_entry"),
    LIST("list"),
    COMMENT("comment"),
    USER("user"),
}
