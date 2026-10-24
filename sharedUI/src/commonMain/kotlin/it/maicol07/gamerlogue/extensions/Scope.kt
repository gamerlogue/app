package it.maicol07.gamerlogue.extensions

import it.maicol07.spraypaintkt.CollectionProxy
import it.maicol07.spraypaintkt.JsonApiCollectionResponse
import it.maicol07.spraypaintkt.JsonApiException
import it.maicol07.spraypaintkt.Resource
import it.maicol07.spraypaintkt.Scope
import it.maicol07.spraypaintkt.interfaces.HttpClientResponse
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Page-based pagination shared by every backend collection: one page per request, pages start
 * at 1 and the size is fixed by the server (30).
 */
private const val MaxPages = 500

// The backend reads the page number from a plain `page` parameter. spraypaintkt's `page()` sends
// `page[number]`, which the backend silently ignores, re-serving the first page.
private const val PageParam = "page"

/** The total number of resources matching the query, from the `meta.totalItems` every list response carries. */
val CollectionProxy<*>.totalItems: Int?
    get() = (meta["totalItems"] as? JsonPrimitive)?.intOrNull

/** The mean rating (0–100) the server adds to the reviews of a game (`meta.average_rating`). */
val CollectionProxy<*>.averageRating: Double?
    get() = (meta["average_rating"] as? JsonPrimitive)?.doubleOrNull

/** This scope restricted to page [number] (1-based). */
fun <R : Resource> Scope<R>.pageNumber(number: Int): Scope<R> = extraParam(PageParam, number.toString())

/**
 * Walk every page of this scope, invoking [onPage] with each page's *new* resources as it arrives.
 * [Scope.all] returns only one page. Stops when a page brings no new ids (covers both the end and a
 * backend that ignores the page number and re-serves the same page) or once `meta.totalItems` is
 * reached; [MaxPages] is a final guard.
 */
suspend fun <R : Resource> Scope<R>.forEachPage(onPage: suspend (List<R>) -> Unit) {
    val seen = mutableSetOf<String?>()
    var total: Int? = null
    var page = 1
    repeat(MaxPages) {
        val result = pageNumber(page).all()
        total = total ?: result.totalItems
        val fresh = result.data.filter { seen.add(it.id) }
        if (fresh.isEmpty()) return
        onPage(fresh)
        if (total != null && seen.size >= total) return
        page++
    }
}

/** Collect every page of this scope into one list (see [forEachPage]). */
suspend fun <R : Resource> Scope<R>.allPages(): List<R> {
    val out = mutableListOf<R>()
    forEachPage { out += it }
    return out
}

/** One page of a backend collection, with the total the server reported. */
data class ResourcePage<R>(val items: List<R>, val total: Int?) {
    /** True when [loaded] items, counting this page's, cover the whole collection. */
    fun isLast(loaded: Int): Boolean = items.isEmpty() || (total != null && loaded >= total)
}

/** Page [number] of this scope. */
suspend fun <R : Resource> Scope<R>.fetchPage(number: Int): ResourcePage<R> =
    pageNumber(number).all().let { ResourcePage(it.data, it.totalItems) }

/**
 * The resources of a JSON:API collection fetched outside a [Scope] (custom endpoints such as
 * `users/{id}/followers`), built with [factory] like a scope would.
 */
fun <R : Resource> JsonApiCollectionResponse.toPage(factory: () -> R): ResourcePage<R> = ResourcePage(
    items = data.map { factory().apply { fromJsonApi(it, included) } },
    total = (meta["totalItems"] as? JsonPrimitive)?.intOrNull,
)

/** The application error `code` of a JSON:API failure (e.g. `profile_private`), if the body has one. */
val Throwable.jsonApiErrorCode: String?
    get() = (this as? JsonApiException)?.let { runCatching { it.errors.firstOrNull()?.code }.getOrNull() }

/** A 2xx response, the only ones the backend answers a successful write with. */
val HttpClientResponse.isSuccessful: Boolean
    get() = statusCode in SuccessStatusRange

private val SuccessStatusRange = 200..299

/**
 * A copy of this resource's id, attributes and relationships in a new instance built by [factory].
 * Resources are mutable and compare by identity, so a state holding one that was changed in place
 * (by `save()`, say) would not recompose: put the copy in the state instead.
 */
fun <R : Resource> R.detachedCopy(factory: () -> R): R = factory().also { copy ->
    copy.id = id
    copy.isPersisted = isPersisted
    copy.attributes.putAll(attributes)
    copy.relationships.putAll(relationships)
    copy.attributes.clearChanges()
    copy.relationships.clearChanges()
}
