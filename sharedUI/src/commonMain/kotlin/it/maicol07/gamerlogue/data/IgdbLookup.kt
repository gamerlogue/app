package it.maicol07.gamerlogue.data

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.apicalypse.SortOrder
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.getGames
import at.released.igdbclient.getPlatforms
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.Platform
import it.maicol07.gamerlogue.extensions.sort
import it.maicol07.gamerlogue.extensions.where
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Single

/**
 * The IGDB games and platforms the profile screens show next to backend data, which only carries
 * their ids. Kept in memory for the session: feeds, reviews and lists repeat the same games, and
 * a game's name and cover don't change while the app runs.
 *
 * Failures are thrown; callers wrap the calls in `safeRequest`.
 */
@Single
class IgdbLookup(private val igdb: IgdbClient) {
    private val mutex = Mutex()
    private val games = mutableMapOf<Int, Game>()
    private var platforms: List<Platform>? = null

    /** The games among [ids] that IGDB knows, by id; one request per [IGDB_MAX_LIMIT] missing ids. */
    suspend fun games(ids: Collection<Int>): Map<Int, Game> = mutex.withLock {
        val missing = ids.filterNot(games::containsKey).distinct()
        for (chunk in missing.chunked(IGDB_MAX_LIMIT)) {
            val result = igdb.getGames {
                fields(
                    Game.field.name,
                    Game.field.cover.image_id,
                    Game.field.first_release_date,
                    Game.field.artworks.image_id,
                    Game.field.screenshots.image_id,
                )
                where { "id" inAny chunk.map(Int::toString) }
                // Without an explicit limit IGDB returns only 10.
                limit(chunk.size)
            }
            result.games.forEach { games[it.id.toInt()] = it }
        }
        ids.mapNotNull { id -> games[id]?.let { id to it } }.toMap()
    }

    /** Every IGDB platform, by name; fetched once. */
    suspend fun platforms(): List<Platform> = mutex.withLock {
        platforms ?: igdb.getPlatforms {
            fields(
                Platform.field.name,
                Platform.field.abbreviation,
                Platform.field.slug,
                Platform.field.platform_logo.image_id,
            )
            sort(Platform.field.name, SortOrder.ASC)
            limit(IGDB_MAX_LIMIT)
        }.platforms.also { platforms = it }
    }

    private companion object {
        /** IGDB's largest page. */
        const val IGDB_MAX_LIMIT = 500
    }
}
