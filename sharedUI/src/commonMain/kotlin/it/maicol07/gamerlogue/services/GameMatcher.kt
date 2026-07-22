package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.getExternalGames
import at.released.igdbclient.getGames
import at.released.igdbclient.getWebsites
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.UnpackedMultiQueryResult
import at.released.igdbclient.multiquery
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import kotlinx.coroutines.delay
import org.koin.core.annotation.Single

/**
 * Maps games coming from an external store to IGDB games, reusing the app's [IgdbClient].
 *
 * Primary match is IGDB's `external_games` endpoint (store id → IGDB game). When a store id has no
 * mapping (common for PSN/Xbox), callers fall back to [searchByName]. Alternatives for the editable
 * import preview are fetched on demand via [searchByName] rather than eagerly, to keep IGDB calls down.
 */
@Single()
class GameMatcher(
    private val igdb: IgdbClient,
    private val exceptionReporter: ExceptionReporter,
) {

    /**
     * IGDB match for a store [ref]: the best [game] (or null) plus the full [candidates] list so the
     * import preview can let the user pick a different one when several titles match. [confident] is
     * true only for an exact store-id (URL) match; a name-fallback guess is not, so the UI requires
     * the user to confirm/choose it before it can be selected.
     */
    data class Match(
        val ref: ExternalGameRef,
        val game: Game?,
        val candidates: List<Game>,
        val confident: Boolean,
    )

    private val externalFields = arrayOf("url", "uid", "game.id", "game.name", "game.cover.image_id")

    /**
     * Match [refs] of [service] to IGDB games. Tries the `external_games` store-id mapping first; for
     * any ref it can't map that way (the `category` field is deprecated in IGDB, so this often returns
     * nothing) it falls back to a fuzzy name search — hence reading store game names matters.
     */
    /** Collecting overload (no streaming). */
    suspend fun match(connector: ServiceConnector, refs: List<ExternalGameRef>): List<Match> {
        val all = ArrayList<Match>(refs.size)
        match(connector, refs) { all.addAll(it) }
        return all
    }

    /**
     * Match [refs], delivering results to [onBatch] as they're resolved so the UI can stream them and
     * show progress: first the store-id matches, then one [onBatch] per name-search batch.
     */
    suspend fun match(
        connector: ServiceConnector,
        refs: List<ExternalGameRef>,
        onBatch: (List<Match>) -> Unit,
    ) {
        if (refs.isEmpty()) return
        val byUid = matchByStoreId(connector, refs)
        Logger.i(tag = TAG) { "store-id matched ${byUid.size}/${refs.size}" }

        // Store-id hits (and refs with no name to search) resolve immediately.
        val resolved = ArrayList<Match>()
        val unmatched = ArrayList<ExternalGameRef>()
        refs.forEach { ref ->
            val direct = byUid[ref.uid]
            when {
                direct != null -> resolved.add(Match(ref, direct, listOf(direct), confident = true))
                ref.name.isBlank() -> resolved.add(Match(ref, null, emptyList(), confident = false))
                else -> unmatched.add(ref)
            }
        }
        if (resolved.isNotEmpty()) onBatch(resolved)

        // Name fallback, batched via multiquery (≤10/request) + throttled to dodge IGDB's 429 limit.
        // These are guesses (confident = false) — the UI makes the user confirm one before selecting.
        unmatched.chunked(MULTIQUERY_CHUNK).forEachIndexed { batchIndex, batch ->
            if (batchIndex > 0) delay(THROTTLE_MS)
            val candidates = nameSearchBatch(batch)
            onBatch(batch.map { ref ->
                val c = candidates[ref.uid].orEmpty()
                Match(ref, c.firstOrNull(), c, confident = false)
            })
        }
    }

    /**
     * One batch of name matches. Tries IGDB fuzzy `search`; if that yields nothing for the whole batch
     * (e.g. the IGDB proxy doesn't support `search`), retries with a `where name ~` contains-match,
     * which the rest of the app already relies on.
     */
    private suspend fun nameSearchBatch(batch: List<ExternalGameRef>): Map<String, List<Game>> {
        val viaSearch = nameMultiquery(batch, useSearch = true)
        if (viaSearch.values.any { it.isNotEmpty() }) {
            Logger.i(tag = TAG) { "name batch (search): ${viaSearch.values.sumOf { it.size }} candidates / ${batch.size}" }
            return viaSearch
        }
        val viaWhere = nameMultiquery(batch, useSearch = false)
        Logger.i(tag = TAG) { "name batch (where~): ${viaWhere.values.sumOf { it.size }} candidates / ${batch.size}" }
        return viaWhere
    }

    private suspend fun nameMultiquery(batch: List<ExternalGameRef>, useSearch: Boolean): Map<String, List<Game>> {
        val result = igdbCall("name multiquery failed (search=$useSearch)") {
            igdb.multiquery {
                batch.forEachIndexed { i, ref ->
                    val q = ref.name.sanitizeForSearch()
                    query(IgdbEndpoint.GAME, "q$i") {
                        if (useSearch) search(q) else where("name ~ *\"$q\"*")
                        fields("id", "name", "cover.image_id")
                        limit(NAME_FALLBACK_LIMIT)
                    }
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        val responses = result as? List<UnpackedMultiQueryResult<Game>> ?: return emptyMap()
        val byUid = mutableMapOf<String, List<Game>>()
        responses.forEach { response ->
            val i = response.name.removePrefix("q").toIntOrNull() ?: return@forEach
            val ref = batch.getOrNull(i) ?: return@forEach
            byUid[ref.uid] = response.results.orEmpty()
        }
        return byUid
    }

    /**
     * Store-id → IGDB game map via `external_games`, matching on the modern `external_game_source` +
     * `url` (the `category`/`uid` pair is deprecated). Only services with a [ExternalService.storeUrl]
     * mapping participate; the rest fall through to the name search.
     */
    private suspend fun matchByStoreId(connector: ServiceConnector, refs: List<ExternalGameRef>): Map<String, Game> {
        val source = connector.externalGameSource
        if (connector.idMatchesUid) return matchByUid(source, refs)
        val urlToUid = refs.asSequence()
            .mapNotNull { ref -> connector.storeUrl(ref.uid)?.let { it to ref.uid } }
            .toMap()
        if (urlToUid.isEmpty()) return emptyMap()

        val byUid = mutableMapOf<String, Game>()
        urlToUid.keys.chunked(UID_CHUNK).forEach { chunk ->
            val quoted = chunk.joinToString(",") { "\"${it.escapeApicalypse()}\"" }
            val response = igdbCall("external_games lookup failed") {
                igdb.getExternalGames {
                    fields(*externalFields)
                    where("external_game_source = $source & url = ($quoted)")
                    limit(chunk.size)
                }
            }
            response?.externalgames?.forEach { ext ->
                val game = ext.game ?: return@forEach
                val url = ext.url ?: return@forEach
                val uid = connector.uidFromUrl(url) ?: urlToUid[url] ?: return@forEach
                byUid[uid] = game
            }
        }
        return byUid
    }

    /**
     * Store-id match on the numeric `uid` field (for stores whose IGDB `url` is slug-based and can't
     * be rebuilt from the store uid, e.g. GOG): the store's product id equals IGDB's `external_games.uid`.
     */
    private suspend fun matchByUid(source: Int, refs: List<ExternalGameRef>): Map<String, Game> {
        val byUid = mutableMapOf<String, Game>()
        refs.map { it.uid }.distinct().chunked(UID_CHUNK).forEach { chunk ->
            val quoted = chunk.joinToString(",") { "\"${it.escapeApicalypse()}\"" }
            val response = igdbCall("external_games uid lookup failed") {
                igdb.getExternalGames {
                    fields(*externalFields)
                    where("external_game_source = $source & uid = ($quoted)")
                    limit(chunk.size)
                }
            }
            response?.externalgames?.forEach { ext ->
                val game = ext.game ?: return@forEach
                val uid = ext.uid.ifBlank { return@forEach }
                byUid[uid] = game
            }
        }
        return byUid
    }

    /**
     * Reverse direction: the store [connector] page URL (IGDB `external_games.url`) for each of
     * [gameIds], used to push Gamerlogue backlog games onto the store wishlist. Games with no store
     * mapping for this service are simply absent.
     */
    suspend fun urlsForGames(connector: ServiceConnector, gameIds: List<Int>): Map<Int, String> {
        if (gameIds.isEmpty()) return emptyMap()
        val source = connector.externalGameSource
        val byGame = mutableMapOf<Int, String>()
        gameIds.distinct().chunked(UID_CHUNK).forEach { chunk ->
            val ids = chunk.joinToString(",")
            val response = igdbCall("external_games url lookup failed") {
                igdb.getExternalGames {
                    fields("url", "game.id")
                    where("external_game_source = $source & game = ($ids)")
                    limit(chunk.size)
                }
            }
            response?.externalgames?.forEach { ext ->
                val gid = ext.game?.id?.toInt() ?: return@forEach
                val url = ext.url ?: return@forEach
                if (gid !in byGame) byGame[gid] = url
            }
        }
        // Fallback: some games carry the store page only in `websites`, not `external_games`. Accept a
        // website URL when the connector recognises it as one of its store pages (uidFromUrl matches).
        val missing = gameIds.distinct().filter { it !in byGame }
        missing.chunked(WEBSITE_GAME_CHUNK).forEach { chunk ->
            val response = igdbCall("websites lookup failed") {
                igdb.getWebsites {
                    fields("url", "game.id")
                    where("game = (${chunk.joinToString(",")})")
                    limit(WEBSITE_LIMIT)
                }
            }
            response?.websites?.forEach { web ->
                val gid = web.game?.id?.toInt() ?: return@forEach
                val url = web.url ?: return@forEach
                if (gid !in byGame && connector.uidFromUrl(url) != null) byGame[gid] = url
            }
        }
        return byGame
    }

    /** Fetch IGDB games by id (name + cover + platform family + involved companies), e.g. to label
     *  Gamerlogue backlog games for a push preview, tell which release on the target store's platform,
     *  and confirm ([ServiceConnector.matchesPublisher]) a search-by-name push targets the right store. */
    suspend fun gamesByIds(ids: List<Int>): List<Game> {
        if (ids.isEmpty()) return emptyList()
        val games = mutableListOf<Game>()
        ids.distinct().chunked(UID_CHUNK).forEach { chunk ->
            igdbCall("games by-id lookup failed") {
                igdb.getGames {
                    fields(
                        "id", "name", "cover.image_id", "platforms.platform_family",
                        "involved_companies.company.name",
                    )
                    where("id = (${chunk.joinToString(",")})")
                    limit(chunk.size)
                }
            }?.games?.let { games.addAll(it) }
        }
        return games
    }

    /** IGDB fuzzy full-text search by name — the fallback match and the manual "change match". */
    suspend fun searchByName(query: String, limit: Int = SEARCH_LIMIT): List<Game> {
        val q = query.sanitizeForSearch()
        if (q.isEmpty()) return emptyList()
        return igdbCall("name search failed") {
            igdb.getGames {
                search(q)
                fields("id", "name", "cover.image_id")
                limit(limit)
            }
        }?.games.orEmpty()
    }

    /** Run an IGDB [request] through [safeRequest] (global error reporting), logging [warn] on failure
     *  and returning the response, or null when the call failed.
     */
    private suspend fun <T> igdbCall(warn: String, request: suspend () -> T): T? {
        val result = exceptionReporter.safeRequest(request)
        result.getError()?.let { Logger.w(tag = TAG, throwable = it) { warn } }
        return result.get()
    }

    private fun String.escapeApicalypse() = replace("\"", "")

    /** Drop trademark glyphs and quotes that hurt IGDB's fuzzy search; collapse whitespace. */
    private fun String.sanitizeForSearch() =
        replace(Regex("[™®©\"]"), " ").replace(Regex("\\s+"), " ").trim()

    companion object {
        private const val TAG = "GameMatcher"
        private const val UID_CHUNK = 100
        private const val SEARCH_LIMIT = 15
        private const val NAME_FALLBACK_LIMIT = 6

        // IGDB multiquery sub-query cap.
        private const val MULTIQUERY_CHUNK = 10
        private const val THROTTLE_MS = 350L

        // Websites: many rows per game, so fewer games per request against IGDB's 500-row page cap.
        private const val WEBSITE_GAME_CHUNK = 30
        private const val WEBSITE_LIMIT = 500
    }
}
