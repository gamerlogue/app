package it.maicol07.gamerlogue.data

import it.maicol07.spraypaintkt_annotation.Attr
import it.maicol07.spraypaintkt_annotation.ResourceSchema

/**
 * A Gamerlogue user. Another user's private profile only carries id, nickname, name, picture and
 * visibility, and the backend may omit null attributes, so everything else is nullable or has a
 * default here: a non-null attribute without one throws when the key is absent.
 */
@ResourceSchema(resourceType = "User")
interface UserSchema {
    @Attr val nickname: String?
    @Attr val name: String?
    @Attr val firstName: String?
    @Attr val lastName: String?
    @Attr val picture: String?

    /** Only sent to the owner. */
    @Attr val email: String?
    @Attr val emailVerifiedAt: String?
    @Attr val bio: String?

    /** The IGDB game whose artwork ([bannerImageId]) is the profile banner; both null or both set. */
    @Attr val bannerGameId: Int?
    @Attr val bannerImageId: String?

    /** IGDB platforms the user owns. */
    @Attr val platformsIds: List<Int>
        get() = emptyList()

    @Attr val visibility: String
        get() = VISIBILITY_PUBLIC

    /** Where the identity provider lets the user change [picture]; null when it has no such page. */
    @Attr val pictureEditUrl: String?
    @Attr val followersCount: Int
        get() = 0
    @Attr val followingCount: Int
        get() = 0
    @Attr val isFollowedByMe: Boolean
        get() = false
    @Attr val followsMe: Boolean
        get() = false
    @Attr val createdAt: String?
    @Attr val updatedAt: String?

    val isPrivate: Boolean
        get() = visibility == VISIBILITY_PRIVATE

    /** The name to show: the nickname, falling back to the full name. */
    val displayName: String
        get() = nickname ?: name.orEmpty()

    companion object {
        const val VISIBILITY_PUBLIC = "public"
        const val VISIBILITY_PRIVATE = "private"
        const val MAX_BIO_LENGTH = 500
    }
}
