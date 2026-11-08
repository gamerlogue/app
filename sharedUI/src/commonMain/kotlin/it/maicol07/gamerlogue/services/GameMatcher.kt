package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.apicalypse.ApicalypseQueryBuilder
import at.released.igdbclient.getCovers
import at.released.igdbclient.getExternalGames
import at.released.igdbclient.getGames
import at.released.igdbclient.getWebsites
import at.released.igdbclient.model.Game
import at.released.igdbclient.multiquery
import co.touchlab.kermit.Logger
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.safeRequest
import it.maicol07.gamerlogue.extensions.igdb.BUNDLE_OR_ADDON_GAME_TYPES
import it.maicol07.gamerlogue.extensions.igdb.NON_LIBRARY_GAME_TYPES
import it.maicol07.gamerlogue.extensions.igdb.isBaseGame
import it.maicol07.gamerlogue.extensions.multiqueryResults
import it.maicol07.gamerlogue.services.connectors.SteamConnector
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.milliseconds

/**
 * Maps games coming from an external store to IGDB games, reusing the app's [IgdbClient].
 *
 * Primary match is IGDB's `external_games` endpoint (store id → IGDB game). When a store id has no
 * mapping (common for PSN/Xbox), matching falls back to a batched name search. The editable import
 * preview uses GameList for manual picks.
 */
// One cohesive matcher: the per-key lookups are private steps of [match], not a separate concern.
@Suppress("TooManyFunctions")
@Single
class GameMatcher(
    private val igdb: IgdbClient,
    private val exceptionReporter: ExceptionReporter,
    val memory: MatchMemory,
) {

    /**
     * IGDB match for a store [ref]: the best [game] (or null) plus the full [candidates] list so the
     * import preview can let the user pick a different one when several titles match. [confident] is
     * true for an exact identity match; a name-fallback guess is not, so the UI requires
     * the user to confirm/choose it before it can be selected.
     */
    data class Match(
        val ref: ExternalGameRef,
        val game: Game?,
        val candidates: List<Game>,
        val confident: Boolean,
    )

    /**
     * Match [refs] of [connector] to IGDB games, collecting every result (no streaming). Tries the
     * `external_games` store-id mapping first; for any ref it can't map that way (the `category` field
     * is deprecated in IGDB, so this often returns nothing) it falls back to a fuzzy name search —
     * hence reading store game names matters.
     */
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
        // The id the service's metadata carries, else the game the user picked for this uid before.
        val picks = memory.picks(connector.service)
        val idByUid = refs.mapNotNull { ref ->
            (ref.gameId?.takeIf { it > 0 } ?: picks[ref.uid])?.let { ref.uid to it }
        }.toMap()
        val games = gamesByIds(idByUid.values.toList()).associateBy { it.id.toInt() }
        val byGameId = idByUid.mapNotNull { (uid, id) -> games[id]?.let { uid to it } }.toMap()
        val untagged = refs.filter { it.uid !in byGameId }
        val byStoreId =
            byGameId + (connector.igdbKey?.let { matchByIgdbKey(it, untagged) } ?: matchByStoreId(connector, untagged))
        // Refs that name their game by IGDB slug (Grouvee's IGDB link) or Steam app id (HowLongToBeat's Steam
        // link) match exactly too.
        val rest = refs.filter { it.uid !in byStoreId }
        val byUid = (byStoreId +
            matchBySlug(rest.filter { it.igdbSlug != null }) +
            matchBySteamAppId(rest.filter { it.steamAppId != null }))
            .filterValues { it.isMatchable }
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

        // Name fallback, batched via multiquery (≤10/request). Batches overlap, their starts spaced to stay
        // under IGDB's 4 req/s; a 429 is retried by the HTTP client. These are guesses (confident = false) —
        // the UI makes the user confirm one before selecting.
        coroutineScope {
            unmatched.chunked(MULTIQUERY_CHUNK).forEachIndexed { batchIndex, batch ->
                launch {
                    delay((THROTTLE_MS * batchIndex).milliseconds)
                    val candidates = nameSearchBatch(batch)
                    onBatch(
                        batch.map { ref ->
                            val c = candidates[ref.uid].orEmpty().rankedBy(ref.name)
                            Match(ref, c.firstOrNull(), c, confident = false)
                        }
                    )
                }
            }
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
            Logger.i(tag = TAG) {
                "name batch (search): ${viaSearch.values.sumOf { it.size }} candidates / ${batch.size}"
            }
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
                        if (useSearch) search(q)
                        where(if (useSearch) NAME_MATCH_FILTER else "name ~ *\"$q\"* & $NAME_MATCH_FILTER")
                        fields("id", "name", "cover.image_id", "platforms.id", "platforms.platform_family", *identityFields())
                        limit(NAME_FALLBACK_LIMIT)
                    }
                }
            }
        }
        val responses = result.orEmpty()
        val byUid = mutableMapOf<String, List<Game>>()
        responses.forEach { response ->
            val i = response.name.removePrefix("q").toIntOrNull() ?: return@forEach
            val ref = batch.getOrNull(i) ?: return@forEach
            byUid[ref.uid] = responses.multiqueryResults<Game>(response.name).filter { it.isMatchable }
        }
        return byUid
    }

    /**
     * Store-id → IGDB game map via `external_games`, matching on the modern `external_game_source` +
     * `url` (the `category`/`uid` pair is deprecated). Only connectors with a [ServiceConnector.storeUrl]
     * mapping participate; the rest fall through to the name search.
     */
    private suspend fun matchByStoreId(connector: ServiceConnector, refs: List<ExternalGameRef>): Map<String, Game> {
        val source = connector.externalGameSource
        if (connector.idMatchesUid) return matchByUid(source, refs)
        val urlToUid = refs.asSequence()
            .mapNotNull { ref -> connector.storeUrl(ref.uid)?.let { it to ref.uid } }
            .toMap()

        val byUid = mutableMapOf<String, Game>()
        urlToUid.keys.chunked(UID_CHUNK).forEach { chunk ->
            val quoted = chunk.joinToString(",") { "\"${it.escapeApicalypse()}\"" }
            val response = igdbCall("external_games lookup failed") {
                igdb.getExternalGames {
                    externalGameFields()
                    where("external_game_source = $source & url = ($quoted)")
                    limit(IGDB_MAX_LIMIT)
                }
            }
            response?.externalgames?.forEach { ext ->
                val game = ext.game ?: return@forEach
                val url = ext.url.ifEmpty { return@forEach }
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
                    externalGameFields()
                    where("external_game_source = $source & uid = ($quoted)")
                    limit(IGDB_MAX_LIMIT)
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

    /** Exact match for services whose uid is an IGDB value itself ([ServiceConnector.igdbKey]). */
    /** Exact match on the Steam app id a ref carries ([ExternalGameRef.steamAppId]), by ref uid: the same
     *  `external_games` lookup a Steam import does, so the store-URL shape stays defined by [SteamConnector]. */
    private suspend fun matchBySteamAppId(refs: List<ExternalGameRef>): Map<String, Game> {
        val uidsByAppId = refs.groupBy({ it.steamAppId.orEmpty() }, { it.uid })
        val byAppId = matchByStoreId(SteamConnector(), uidsByAppId.keys.map { ExternalGameRef(it) })
        return byAppId.flatMap { (appId, game) -> uidsByAppId[appId].orEmpty().map { it to game } }.toMap()
    }

    /** Exact match on the IGDB slug a ref carries ([ExternalGameRef.igdbSlug]), by ref uid. */
    private suspend fun matchBySlug(refs: List<ExternalGameRef>): Map<String, Game> {
        val uidsBySlug = refs.groupBy({ it.igdbSlug.orEmpty() }, { it.uid })
        val byUid = mutableMapOf<String, Game>()
        uidsBySlug.keys.chunked(UID_CHUNK).forEach { chunk ->
            val quoted = chunk.joinToString(",") { "\"${it.escapeApicalypse()}\"" }
            igdbCall("games by-slug lookup failed") {
                igdb.getGames {
                    fields("id", "name", "slug", "cover.image_id", "platforms.id", "platforms.platform_family", *identityFields())
                    where("slug = ($quoted)")
                    limit(chunk.size)
                }
            }?.games?.forEach { game -> uidsBySlug[game.slug]?.forEach { byUid[it] = game } }
        }
        return byUid
    }

    private suspend fun matchByIgdbKey(key: IgdbKey, refs: List<ExternalGameRef>): Map<String, Game> = when (key) {
        IgdbKey.GAME_ID -> gamesByIds(refs.mapNotNull { it.uid.toIntOrNull() }).associateBy { it.id.toString() }
        IgdbKey.COVER_IMAGE_ID -> {
            val covers = refs.map { it.uid }.distinct().chunked(UID_CHUNK).flatMap { chunk ->
                val quoted = chunk.joinToString(",") { "\"${it.escapeApicalypse()}\"" }
                igdbCall("covers lookup failed") {
                    igdb.getCovers {
                        fields("image_id", "game")
                        where("image_id = ($quoted)")
                        limit(chunk.size)
                    }
                }?.covers.orEmpty()
            }
            val gameByCover = covers.mapNotNull { cover -> cover.game?.let { cover.image_id to it.id.toInt() } }.toMap()
            val games = gamesByIds(gameByCover.values.toList()).associateBy { it.id.toInt() }
            gameByCover.entries.mapNotNull { (cover, id) -> games[id]?.let { cover to it } }.toMap()
        }
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
                    limit(IGDB_MAX_LIMIT)
                }
            }
            response?.externalgames?.forEach { ext ->
                val gid = ext.game?.id?.toInt() ?: return@forEach
                val url = ext.url.ifEmpty { return@forEach }
                if (gid !in byGame) byGame[gid] = url
            }
        }
        // Fallback: some games carry the store page only in `websites`, not `external_games`. Accept a
        // website URL when the connector recognizes it as one of its store pages (uidFromUrl matches).
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
                val url = web.url.ifEmpty { return@forEach }
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
        // One row per id, so a request can take IGDB's whole page: the proxy allows few requests a minute.
        ids.distinct().chunked(IGDB_MAX_LIMIT).forEach { chunk ->
            igdbCall("games by-id lookup failed") {
                igdb.getGames {
                    fields(
                        "id",
                        "name",
                        "cover.image_id",
                        "platforms.platform_family",
                        "platforms.name",
                        "platforms.abbreviation",
                        "involved_companies.company.name",
                        *identityFields(),
                    )
                    where("id = (${chunk.joinToString(",")})")
                    limit(chunk.size)
                }
            }?.games?.let { games.addAll(it) }
        }
        return games
    }

    /** The version parent id of each of [ids] that is an edition; the other ids are absent. Library entries saved
     *  before editions were folded into their base game are keyed by the edition's id. */
    suspend fun editionParents(ids: Collection<Int>): Map<Int, Int> =
        ids.distinct().chunked(IGDB_MAX_LIMIT).flatMap { chunk ->
            igdbCall("version parents lookup failed") {
                igdb.getGames {
                    fields("id", "version_parent.id")
                    where("id = (${chunk.joinToString(",")}) & version_parent != null")
                    limit(chunk.size)
                }
            }?.games.orEmpty()
        }.mapNotNull { game -> game.version_parent?.let { game.id.toInt() to it.id.toInt() } }.toMap()

    /** The editions, DLCs and expansions of game [id] that a match may be instead of the game itself. */
    suspend fun variantsOf(id: Int): List<Game> = igdbCall("variants lookup failed") {
        igdb.getGames {
            fields(
                "id",
                "name",
                "version_title",
                "cover.image_id",
                "platforms.id",
                "platforms.platform_family",
                *identityFields(),
            )
            where("(version_parent = $id | parent_game = $id) & $LIBRARY_TYPES_FILTER")
            limit(IGDB_MAX_LIMIT)
        }
    }?.games.orEmpty()

    /** IGDB fuzzy full-text search restricted to games shown in GameList. */
    suspend fun searchByName(query: String, limit: Int = SEARCH_LIMIT): List<Game> {
        val q = query.sanitizeForSearch()
        if (q.isEmpty()) return emptyList()
        return igdbCall("name search failed") {
            igdb.getGames {
                search(q)
                fields("id", "name", "cover.image_id", "platforms.id", "platforms.platform_family", *identityFields())
                where(BASE_GAMES_FILTER)
                limit(limit)
            }
        }?.games.orEmpty().filter { it.isBaseGame }
    }

    /** Run an IGDB [request] through [safeRequest] (global error reporting), logging [warn] on failure
     *  and returning the response, or null when the call failed.
     */
    private suspend fun <T> igdbCall(warn: String, request: suspend () -> T): T? {
        val result = exceptionReporter.safeRequest(request)
        result.getError()?.let { Logger.w(tag = TAG, throwable = it) { warn } }
        return result.get()
    }

    companion object {
        private const val TAG = "GameMatcher"
        private const val UID_CHUNK = 100
        private const val SEARCH_LIMIT = 15

        // Wide enough that the closest title is likely among the candidates [rankedBy] reorders.
        private const val NAME_FALLBACK_LIMIT = 10

        // IGDB multiquery sub-query cap.
        private const val MULTIQUERY_CHUNK = 10
        private const val THROTTLE_MS = 350L

        // Websites: many rows per game, so fewer games per request against IGDB's 500-row page cap.
        private const val WEBSITE_GAME_CHUNK = 30
        private const val WEBSITE_LIMIT = 500

        // IGDB's page cap: external_games can hold several rows per game, so the key count is not enough.
        private const val IGDB_MAX_LIMIT = 500
    }
}

/** The `external_games` fields the store-id matchers read. */
private fun ApicalypseQueryBuilder.externalGameFields() {
    fields(
        "url",
        "uid",
        "game.id",
        "game.name",
        "game.cover.image_id",
        "game.platforms.id",
        "game.platforms.platform_family",
        *identityFields("game."),
    )
}

/**
 * The fields [isMatchable] and `baseGameId` read. They go into the same `fields(...)` call as the query's own:
 * igdbclient's `fields()` replaces the list, so a separate call would drop everything set before it.
 */
internal fun identityFields(prefix: String = ""): Array<String> = arrayOf(
    "${prefix}game_type",
    "${prefix}version_parent.id",
    "${prefix}version_parent.name",
    "${prefix}version_parent.game_type",
    "${prefix}version_parent.cover.image_id",
    "${prefix}version_parent.platforms.id",
    "${prefix}version_parent.platforms.platform_family",
)

/** Whether a match may be a library entry: an edition counts through its parent, [NON_LIBRARY_GAME_TYPES] never do. */
val Game.isMatchable: Boolean
    get() = NON_LIBRARY_GAME_TYPES.none { it.value.toLong() == (version_parent ?: this).game_type?.id }

private val BASE_GAMES_FILTER = "version_parent = null & game_type != " +
    BUNDLE_OR_ADDON_GAME_TYPES.joinToString(",", "(", ")") { it.value.toString() }

private val LIBRARY_TYPES_FILTER = "game_type != " +
    NON_LIBRARY_GAME_TYPES.joinToString(",", "(", ")") { it.value.toString() }

// DLCs are searched too: their names carry the game's, so the closest title still wins (see [rankedBy]).
private val NAME_MATCH_FILTER = "version_parent = null & $LIBRARY_TYPES_FILTER"

private fun String.escapeApicalypse() = replace("\"", "")

/**
 * Candidates ordered by how close their name is to the store's [name]: IGDB's search relevance puts
 * "The Clown's Forest 3: Haunting Apparitions" before "Forest 3". Sorting is stable, so IGDB's order breaks ties.
 */
// ponytail: plain edit distance on normalized titles; weigh tokens (subtitles, editions) if it misranks.
internal fun List<Game>.rankedBy(name: String): List<Game> {
    val target = name.normalizedTitle()
    return sortedBy { editDistance(it.name.normalizedTitle(), target) }
}

private fun String.normalizedTitle() = lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

/** Levenshtein distance between [a] and [b]. */
private fun editDistance(a: String, b: String): Int {
    var previous = IntArray(b.length + 1) { it }
    a.forEachIndexed { i, ca ->
        val current = IntArray(b.length + 1)
        current[0] = i + 1
        b.forEachIndexed { j, cb ->
            current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + if (ca == cb) 0 else 1)
        }
        previous = current
    }
    return previous[b.length]
}

/** Drop trademark glyphs and quotes that hurt IGDB's fuzzy search; collapse whitespace. */
private fun String.sanitizeForSearch() =
    replace(Regex("[™®©\"]"), " ").replace(Regex("\\s+"), " ").trim()
