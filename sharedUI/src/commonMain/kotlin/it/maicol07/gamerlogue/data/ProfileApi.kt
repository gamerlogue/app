package it.maicol07.gamerlogue.data

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import it.maicol07.gamerlogue.extensions.ResourcePage
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.fetchPage
import it.maicol07.gamerlogue.extensions.toPage
import it.maicol07.spraypaintkt.JsonApiCollectionResponse
import it.maicol07.spraypaintkt.JsonApiException
import it.maicol07.spraypaintkt.JsonApiSingleResponse
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

/**
 * The profile calls that are not a plain resource read or write: plain-JSON endpoints (stats, list
 * reordering, read-all, the account), and the operations that need a lookup first (unfollow and
 * unblock by user, reactions by subject) or a header the resources cannot send (library imports).
 *
 * Calls go through the same authenticated client as the resources, so the bearer refresh and the
 * web session apply; failures are thrown as [JsonApiException], like a resource call's, so callers
 * read the error code the same way.
 */
@Single
@Suppress("TooManyFunctions") // One function per endpoint: splitting the class would only hide the count.
class ProfileApi : KoinComponent {
    // Resolved per call, like AppJsonApiConfig's, so it never outlives the Koin instance it came from.
    private val client: HttpClient
        get() = get(named("JsonApiHttpClient"))

    private val json = Json { ignoreUnknownKeys = true }

    /** [userId]'s library statistics, limited to [year] when set. */
    suspend fun stats(userId: String, year: Int?): UserStats {
        val params = year?.let { mapOf("year" to it.toString()) }.orEmpty()
        return json.decodeFromString(getJson("users/$userId/stats", params))
    }

    suspend fun follow(userId: String) = Follow().apply { followableId = userId }.save()

    /** Always allowed, even once the profile turned private or a block came in between. */
    suspend fun unfollow(userId: String) {
        Follow.where("followable_id", userId).all().data.firstOrNull()?.destroy()
    }

    suspend fun followers(userId: String, page: Int): ResourcePage<User> = userPage("users/$userId/followers", page)

    suspend fun following(userId: String, page: Int): ResourcePage<User> = userPage("users/$userId/following", page)

    /** Blocking also ends the follows in both directions. */
    suspend fun block(userId: String) = Block().apply { blockedId = userId }.save()

    suspend fun unblock(userId: String) {
        blockOf(userId)?.destroy()
    }

    /**
     * The current user's block of [userId], if any. The blocks have no filter by user, so they are
     * walked: a user blocks few people.
     */
    suspend fun blockOf(userId: String): Block? = Block.scope().allPages().find { it.blockedId == userId }

    /** The current user's blocks, with the blocked users' public cards. */
    suspend fun blocks(page: Int): ResourcePage<Block> = Block.includes("blocked").fetchPage(page)

    /**
     * Sets the current user's reaction to [id] of [target] to [type] (`like` or, on a comment, an
     * emoji), or takes it back with null. A user has one reaction per subject, so an existing one is
     * changed rather than doubled.
     */
    suspend fun setReaction(target: InteractionTarget, id: String, type: String?) {
        val existing = Reaction.where(
            "reactable_type",
            target.apiType
        ).where("reactable_id", id).all().data.firstOrNull()
        when {
            type == null -> existing?.destroy()
            existing == null -> Reaction().apply {
                reactableType = target.apiType
                reactableId = id.toInt()
                reactionType = type
            }.save()
            existing.reactionType != type -> existing.apply { reactionType = type }.save()
        }
    }

    /** Likes or unlikes [id] of [target], the opposite of [current], and returns the state to show. */
    suspend fun toggleLike(target: InteractionTarget, id: String, current: LikeState): LikeState {
        setReaction(target, id, if (current.liked) null else ReactionSchema.LIKE)
        return LikeState(!current.liked, current.count + if (current.liked) -1 else 1)
    }

    /** Reorders a list: [itemIds] must be exactly all of its item ids, in the new order. */
    suspend fun reorderList(listId: String, itemIds: List<String>) =
        send(HttpMethod.Post, "lists/$listId/reorder", jsonBody = """{"ids":[${itemIds.joinToString(",")}]}""")

    suspend fun markNotificationRead(id: String) = AppNotification().apply {
        this.id = id
        isPersisted = true
        isRead = true
    }.save()

    suspend fun markAllNotificationsRead() = send(HttpMethod.Post, "notifications/read-all")

    /** The unread notifications, counted by the server as the `meta.totalItems` of their collection. */
    suspend fun unreadNotificationsCount(): Int = AppNotification.where("unread", "true").fetchPage(1).total ?: 0

    /** Permanently deletes the account and revokes every session; it does not touch the identity provider. */
    suspend fun deleteAccount() = send(HttpMethod.Delete, "account")

    /** Every piece of the current user's data, as the JSON file the server builds. */
    suspend fun exportAccount(): String = getJson("account/export", emptyMap())

    /**
     * Writes [entry] as part of the store sync [batch]: the `X-Library-Import` header makes the server
     * fold the sync into one `library_imported` activity instead of one per entry. [Resource.save]
     * cannot send headers, so this repeats its request on the same client.
     */
    suspend fun saveImported(entry: LibraryEntry, batch: String) {
        val response = client.request(entry.toUrl()) {
            method = if (entry.isPersisted) HttpMethod.Patch else HttpMethod.Post
            header(IMPORT_BATCH_HEADER, batch)
            contentType(JsonApiContentType)
            setBody(entry.toJsonApiString(onlyDirty = entry.isPersisted))
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw JsonApiException(response.status.value, body)
        if (body.isNotBlank()) entry.fromJsonApiResponse(JsonApiSingleResponse.fromJsonApiString(body))
        entry.attributes.clearChanges()
        entry.relationships.clearChanges()
    }

    private suspend fun userPage(path: String, page: Int): ResourcePage<User> =
        JsonApiCollectionResponse.fromJsonApiString(send(HttpMethod.Get, path, pageParam(page))).toPage(::User)

    private fun pageParam(page: Int) = mapOf("page" to page.toString())

    /**
     * A GET of a plain-JSON endpoint. It adds JSON to the accepted types: Ktor's `defaultRequest` still
     * appends the client's JSON:API one rather than letting a request replace it.
     */
    private suspend fun getJson(path: String, params: Map<String, String>) =
        send(HttpMethod.Get, path, params) { accept(ContentType.Application.Json) }

    private suspend fun send(
        method: HttpMethod,
        path: String,
        params: Map<String, String> = emptyMap(),
        jsonBody: String? = null,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): String {
        val response = client.request("${AppJsonApiConfig.baseUrl}/$path") {
            this.method = method
            configure()
            params.forEach { (key, value) -> parameter(key, value) }
            if (jsonBody != null) {
                // Plain JSON, not the JSON:API media type the client sends by default.
                contentType(ContentType.Application.Json)
                setBody(jsonBody)
            }
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw JsonApiException(response.status.value, body)
        return body
    }

    private companion object {
        const val IMPORT_BATCH_HEADER = "X-Library-Import"
        val JsonApiContentType = ContentType("application", "vnd.api+json")
    }
}
