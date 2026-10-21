package it.maicol07.gamerlogue.data

import it.maicol07.spraypaintkt_annotation.Attr
import it.maicol07.spraypaintkt_annotation.Relation
import it.maicol07.spraypaintkt_annotation.ResourceSchema

/**
 * Something a user did, written by the server. [payload] carries what the feed shows (game ids,
 * statuses, list name…), so rendering needs no further request; see [ActivityVerb]. The subject
 * (`include=subject`) is polymorphic, so it is read raw through `Activity.subjectId`.
 */
@ResourceSchema(endpoint = "activities", resourceType = "Activity")
interface ActivitySchema {
    @Attr val verb: String
    @Attr val subjectType: String?
    @Attr val payload: Map<String, Any?>
        get() = emptyMap()
    @Attr val createdAt: String?
    @Attr val updatedAt: String?
    @Relation val user: UserSchema?
}

/** The activity verbs the client renders; an unknown one is skipped. */
enum class ActivityVerb(val apiName: String) {
    LIBRARY_ADDED("library_added"),
    COMPLETED("completed"),
    STATUS_CHANGED("status_changed"),
    RATED("rated"),
    REVIEWED("reviewed"),
    LIST_CREATED("list_created"),
    LIST_ITEM_ADDED("list_item_added"),
    FOLLOWED("followed"),
    LIBRARY_IMPORTED("library_imported"),
    BADGE_EARNED("badge_earned");

    companion object {
        fun of(apiName: String): ActivityVerb? = entries.find { it.apiName == apiName }
    }
}

/** An in-app notification; [data] holds `actor`, `subject_type`, `subject_id` and the type's own keys. */
@ResourceSchema(endpoint = "notifications", resourceType = "Notification")
interface AppNotificationSchema {
    // Renamed: `type` is Resource's JSON:API type.
    @Attr("type")
    val notificationType: String?
    @Attr val data: Map<String, Any?>
        get() = emptyMap()
    @Attr val isRead: Boolean
        get() = false
    @Attr val readAt: String?
    @Attr val createdAt: String?
}

/** A report for moderation; one per author and subject. */
@ResourceSchema(endpoint = "reports", resourceType = "Report")
interface ReportSchema {
    @Attr val reportableType: String
    @Attr val reportableId: String
    @Attr val reason: String
    @Attr val note: String?

    companion object {
        const val MAX_NOTE_LENGTH = 1000
    }
}

/** Why something is reported, with its backend name. */
enum class ReportReason(val apiName: String) {
    SPAM("spam"),
    HARASSMENT("harassment"),
    HATE_SPEECH("hate_speech"),
    SPOILER("spoiler"),
    INAPPROPRIATE("inappropriate"),
    IMPERSONATION("impersonation"),
    OTHER("other"),
}

/** The current user following another one; only the caller's own are listed. */
@ResourceSchema(endpoint = "follows", resourceType = "Follow")
interface FollowSchema {
    @Attr val followableId: String
    @Attr val userId: String?
    @Attr val createdAt: String?
}

/** The current user blocking another one; only the caller's own are listed. */
@ResourceSchema(endpoint = "blocks", resourceType = "Block")
interface BlockSchema {
    @Attr val blockedId: String
    @Attr val userId: String?
    @Attr val createdAt: String?
    @Relation val blocked: UserSchema?
}

/**
 * The current user's reaction to a review, a list or a comment: one per subject. Reviews and lists
 * only take [LIKE]; comments take it or any single emoji.
 */
@ResourceSchema(endpoint = "reactions", resourceType = "Reaction")
interface ReactionSchema {
    @Attr val reactableType: String
    @Attr val reactableId: Int

    // Renamed: `type` is Resource's JSON:API type.
    @Attr("type")
    val reactionType: String
    @Attr val userId: String?

    companion object {
        const val LIKE = "like"
    }
}
