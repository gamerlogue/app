package it.maicol07.gamerlogue.data

import it.maicol07.spraypaintkt_annotation.Attr
import it.maicol07.spraypaintkt_annotation.Relation
import it.maicol07.spraypaintkt_annotation.ResourceSchema

/**
 * A custom game list. The wire type is `List`, but the schema is named `UserList`: a generated
 * `List` class in this package would shadow `kotlin.collections.List` for every schema here.
 */
@ResourceSchema(endpoint = "lists", resourceType = "List")
interface UserListSchema {
    @Attr val name: String
    @Attr val description: String?
    @Attr val visibility: String
        get() = UserSchema.VISIBILITY_PUBLIC
    @Attr val isRanked: Boolean
        get() = false
    @Attr val userId: String?
    @Attr val itemsCount: Int
        get() = 0
    @Attr val likesCount: Int
        get() = 0
    @Attr val commentsCount: Int
        get() = 0
    @Attr val likedByMe: Boolean
        get() = false
    @Attr val createdAt: String?
    @Attr val updatedAt: String?
    @Relation val user: UserSchema?

    val isPrivate: Boolean
        get() = visibility == UserSchema.VISIBILITY_PRIVATE

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_DESCRIPTION_LENGTH = 5000
    }
}

/** A game in a [UserListSchema]; it does not have to be in the owner's library. */
@ResourceSchema(endpoint = "list_items", resourceType = "ListItem")
interface UserListItemSchema {
    @Attr val listId: Int
    @Attr val gameId: Int

    /** Zero-based; left unset on creation, the server puts the game last. */
    @Attr val position: Int
    @Attr val note: String?
    @Attr val createdAt: String?
    @Attr val updatedAt: String?

    companion object {
        const val MAX_NOTE_LENGTH = 2000
    }
}
