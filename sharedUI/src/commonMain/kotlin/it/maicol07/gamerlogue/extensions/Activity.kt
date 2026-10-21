package it.maicol07.gamerlogue.extensions

import it.maicol07.gamerlogue.data.Activity
import it.maicol07.spraypaintkt.Resource

/** The IGDB games the activity is about: one for library verbs, every game added for `list_item_added`. */
val Activity.gameIds: List<Int>
    get() = (payload["game_ids"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
        ?: listOfNotNull((payload["game_id"] as? Number)?.toInt())

/** The first of [gameIds], the one the feed shows. */
val Activity.gameId: Int?
    get() = gameIds.firstOrNull()

/**
 * The id of the activity's subject (an entry, a list, a user, a badge). The relationship is
 * polymorphic, so it is read raw; it is only there when requested with `include=subject`.
 */
val Activity.subjectId: String?
    get() = (relationships["subject"] as? Resource)?.id

/** The id of who did it, from the included `user`. */
val Activity.actorId: String?
    get() = user?.let { (it as? Resource)?.id }

/** A payload value as text, e.g. a list name or a status. */
fun Activity.payloadText(key: String): String? = payload[key]?.toString()
