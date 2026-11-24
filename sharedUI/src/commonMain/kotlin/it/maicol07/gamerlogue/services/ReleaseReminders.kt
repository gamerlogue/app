package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.error.IgdbException
import at.released.igdbclient.getGames
import at.released.igdbclient.model.Game
import at.released.igdbclient.model.GameCategoryEnum
import co.touchlab.kermit.Logger
import com.russhwolf.settings.ObservableSettings
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.reminders__dlc
import gamerlogue.sharedui.generated.resources.reminders__release
import gamerlogue.sharedui.generated.resources.reminders__release_delayed
import gamerlogue.sharedui.generated.resources.reminders__release_earlier
import gamerlogue.sharedui.generated.resources.reminders__release_soon
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.LocalNotifier
import it.maicol07.gamerlogue.core.NotificationChannel
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.currentUserEntries
import it.maicol07.gamerlogue.extensions.igdb.releaseDayLabel
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.spraypaintkt.JsonApiException
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Single
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/** What the daily check tells about the backlog: [dates] are its release days, to compare with on the next check. */
internal data class ReleaseNews(
    val released: List<Game>,
    val soon: List<Game>,
    /** Each with the release day it had before (epoch seconds). */
    val moved: List<Pair<Game, Long>>,
    val dates: Map<Int, Long>,
)

/**
 * What of the [backlog] games (with their `first_release_date`) is news for the days [from] to [to]: out on one of
 * them, out [SOON_DAYS] after one of them, or moved by [MOVE_THRESHOLD_DAYS] days or more from the [known] day to a
 * day still to come. A game already told as out or soon isn't told as moved too.
 */
internal fun releaseNews(backlog: List<Game>, known: Map<Int, Long>, from: LocalDate, to: LocalDate): ReleaseNews {
    val dated = backlog.mapNotNull { game -> game.first_release_date?.getEpochSecond()?.let { game to it } }
    fun dayOf(date: LocalDate) = date.atStartOfDayIn(TimeZone.UTC).epochSeconds
    fun within(at: Long, first: LocalDate, last: LocalDate) =
        at >= dayOf(first) && at < dayOf(last.plus(DatePeriod(days = 1)))
    val soonShift = DatePeriod(days = SOON_DAYS)
    val released = dated.filter { (_, at) -> within(at, from, to) }.map { it.first }
    val soon = dated.filter { (_, at) -> within(at, from.plus(soonShift), to.plus(soonShift)) }.map { it.first }
    val told = (released + soon).map { it.id }.toSet()
    val moved = dated.mapNotNull { (game, at) ->
        val before = known[game.id.toInt()] ?: return@mapNotNull null
        val far = abs(at - before) >= MOVE_THRESHOLD_DAYS.days.inWholeSeconds
        (game to before).takeIf { far && at >= dayOf(to) && game.id !in told }
    }
    return ReleaseNews(released, soon, moved, dated.associate { (game, at) -> game.id.toInt() to at })
}

/** Out a week ahead: the heads-up. */
internal const val SOON_DAYS = 7

/** A smaller move is IGDB settling the day (often a time zone), not news. */
internal const val MOVE_THRESHOLD_DAYS = 7

/**
 * The daily check of the signed-in user's games: the backlog ones out, out in a week, or with a new release day, and
 * the DLCs and expansions of the library's games that came out. It covers the days since the last check, at most
 * [MAX_CATCH_UP_DAYS] back when checks were missed.
 */
@Single
class ReleaseReminders(
    private val settings: ObservableSettings,
    private val igdb: IgdbClient,
    private val auth: AuthTokenProvider,
    private val notifier: LocalNotifier,
) {
    /** False when the backend or IGDB didn't answer, so the caller tries again later. */
    suspend fun check(): Boolean {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val last = settings.getStringOrNull(DAY_KEY)?.let(LocalDate::parse)
        if (!auth.session.value.isAuthenticated || last == today) return true
        val from = last?.takeIf { it.daysUntil(today) <= MAX_CATCH_UP_DAYS }?.plus(DatePeriod(days = 1)) ?: today
        val fetched = fetchOrNull(from, today)
        if (fetched != null) {
            val known = settings.getStringOrNull(DATES_KEY)?.let { json.decodeFromString<Map<Int, Long>>(it) }.orEmpty()
            val news = releaseNews(fetched.backlog, known, from, today)
            tell(news, fetched.dlcs)
            settings.putString(DATES_KEY, json.encodeToString(news.dates))
            settings.putString(DAY_KEY, today.toString())
        }
        return fetched != null
    }

    private suspend fun tell(news: ReleaseNews, dlcs: List<Game>) {
        val out = getString(Res.string.reminders__release)
        news.released.forEach { notify(NotificationChannel.RELEASES, "release", it, out) }
        news.soon.forEach { game ->
            val day = releaseDayLabel(game.first_release_date?.getEpochSecond() ?: return@forEach)
            notify(NotificationChannel.RELEASES, "soon", game, getString(Res.string.reminders__release_soon, day))
        }
        news.moved.forEach { (game, before) ->
            val at = game.first_release_date?.getEpochSecond() ?: return@forEach
            val text = if (at > before) Res.string.reminders__release_delayed else Res.string.reminders__release_earlier
            notify(NotificationChannel.RELEASES, "moved", game, getString(text, releaseDayLabel(at)))
        }
        dlcs.forEach { dlc ->
            val text = getString(Res.string.reminders__dlc, dlc.parent_game?.name.orEmpty())
            notify(NotificationChannel.DLC, "dlc", dlc, text)
        }
    }

    private suspend fun notify(channel: NotificationChannel, kind: String, game: Game, text: String) = notifier.notify(
        channel,
        tag = "reminder.$kind.${game.id}",
        title = game.name,
        text = text,
        target = RootNavTree.GameDetail(game.id.toInt(), game.cover?.image_id, game.name),
    )

    private class Fetched(val backlog: List<Game>, val dlcs: List<Game>)

    private suspend fun fetchOrNull(from: LocalDate, to: LocalDate): Fetched? = try {
        fetch(from, to)
    } catch (error: IgdbException) {
        failed(error)
    } catch (error: JsonApiException) {
        failed(error)
    } catch (error: IOException) {
        failed(error)
    }

    private fun failed(error: Exception): Nothing? {
        Logger.w(error) { "Release reminders not checked" }
        return null
    }

    /** The backlog games with their release day, and the DLCs of the library's games (but the abandoned) out [from] [to]. */
    private suspend fun fetch(from: LocalDate, to: LocalDate): Fetched {
        val entries = allPages { LibraryEntry.currentUserEntries() }
        val backlog = entries.filter { it.status == GameLibraryStatus.BACKLOG }.map { it.gameId }.distinct()
        val library = entries.filter { it.status != GameLibraryStatus.ABANDONED }.map { it.gameId }.distinct()
        val start = from.atStartOfDayIn(TimeZone.UTC).epochSeconds
        val end = to.plus(DatePeriod(days = 1)).atStartOfDayIn(TimeZone.UTC).epochSeconds
        val backlogGames = backlog.chunked(IGDB_MAX_LIMIT).flatMap { chunk ->
            igdb.getGames {
                fields(Game.field.name, Game.field.cover.image_id, Game.field.first_release_date)
                where { Game.field.id inAny chunk.map(Int::toString) }
                limit(chunk.size)
            }.games
        }
        val dlcs = library.chunked(IGDB_MAX_LIMIT).flatMap { chunk ->
            igdb.getGames {
                fields(Game.field.name, Game.field.cover.image_id, Game.field.parent_game.name)
                where {
                    Game.field.parent_game inAny chunk.map(Int::toString)
                    Game.field.game_type inAny DLC_TYPES.map { it.value.toString() }
                    Game.field.first_release_date greaterThanOrEqual start
                    Game.field.first_release_date lessThan end
                }
                limit(IGDB_MAX_LIMIT)
            }.games
        }
        return Fetched(backlogGames, dlcs)
    }

    private companion object {
        const val MAX_CATCH_UP_DAYS = 7
        const val IGDB_MAX_LIMIT = 500
        const val DAY_KEY = "reminders.releases.day"
        const val DATES_KEY = "reminders.releases.dates"
        val DLC_TYPES = listOf(
            GameCategoryEnum.DLC_ADDON,
            GameCategoryEnum.EXPANSION,
            GameCategoryEnum.STANDALONE_EXPANSION,
            GameCategoryEnum.EPISODE,
            GameCategoryEnum.SEASON,
        )
        val json = Json { ignoreUnknownKeys = true }
    }
}
