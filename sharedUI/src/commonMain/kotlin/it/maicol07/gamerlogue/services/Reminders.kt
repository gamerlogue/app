package it.maicol07.gamerlogue.services

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.dsl.field.field
import at.released.igdbclient.error.IgdbException
import at.released.igdbclient.getGames
import at.released.igdbclient.model.Event
import at.released.igdbclient.model.Game
import co.touchlab.kermit.Logger
import com.russhwolf.settings.ObservableSettings
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.reminders__event_started
import gamerlogue.sharedui.generated.resources.reminders__event_starts
import gamerlogue.sharedui.generated.resources.reminders__release
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.LocalNotifier
import it.maicol07.gamerlogue.core.NotificationChannel
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.currentUserEntries
import it.maicol07.gamerlogue.extensions.where
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.views.library.GameLibraryStatus
import it.maicol07.spraypaintkt.JsonApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** An IGDB event the user wants a reminder of, as it was when followed: the reminder needs no request. */
@Serializable
data class FollowedEvent(val id: Long, val name: String, val startsAt: Long)

/** When the reminders run: told by [Reminders.watch], each run calls [Reminders.check]. */
interface ReminderScheduler {
    /** Plans a [Reminders.check] once a day, and one at each of [eventChecks] (epoch seconds). */
    fun schedule(eventChecks: Collection<Long>)
}

/**
 * The reminders the app gives on its own, without the backend: the games of the backlog that come out, once a day
 * for the signed-in user, and the IGDB events the user follows on this device, [EVENT_LEAD] before they start.
 */
@Single
class Reminders(
    private val settings: ObservableSettings,
    private val igdb: IgdbClient,
    private val auth: AuthTokenProvider,
    private val notifier: LocalNotifier,
) {
    val isSupported: Boolean get() = notifier.isSupported

    // ponytail: the start is the one IGDB had when followed; following again picks up a moved one.
    val followedEvents: StateFlow<Map<Long, FollowedEvent>>
        field = MutableStateFlow(
            settings.getStringOrNull(EVENTS_KEY)?.let { json.decodeFromString<List<FollowedEvent>>(it) }.orEmpty()
                .associateBy { it.id },
        )

    fun setFollowed(event: Event, followed: Boolean) {
        val startsAt = event.start_time?.getEpochSecond()
        val events = followedEvents.value.toMutableMap()
        if (followed && startsAt != null) {
            events[event.id] = FollowedEvent(event.id, event.name, startsAt)
        } else {
            events.remove(event.id)
        }
        saveEvents(events)
    }

    /** Keeps the [scheduler] on the followed events, for as long as it's collected. */
    suspend fun watch(scheduler: ReminderScheduler) {
        if (!isSupported) return
        followedEvents.collect { events ->
            scheduler.schedule(events.values.map { it.startsAt - EVENT_LEAD.inWholeSeconds })
        }
    }

    /**
     * Notifies what is due. False when the releases couldn't be checked (offline, a server down), so the caller tries
     * again later; the events need no request.
     */
    suspend fun check(): Boolean {
        checkEvents()
        return checkReleases()
    }

    /** Due events are told, and forgotten: one that already started a while ago (the app wasn't running) silently. */
    private suspend fun checkEvents() {
        val now = Clock.System.now().epochSeconds
        val due = followedEvents.value.values.filter { it.startsAt - EVENT_LEAD.inWholeSeconds <= now + EARLY_SLACK }
        if (due.isEmpty()) return
        for (event in due.filter { now < it.startsAt + LATE_LIMIT.inWholeSeconds }) {
            val local = Instant.fromEpochSeconds(event.startsAt).toLocalDateTime(TimeZone.currentSystemDefault())
            val time = LocalTime(local.hour, local.minute).toString()
            val text = if (now < event.startsAt) Res.string.reminders__event_starts else Res.string.reminders__event_started
            notifier.notify(
                NotificationChannel.EVENTS,
                tag = "reminder.event.${event.id}",
                title = event.name,
                text = getString(text, time),
                target = RootNavTree.GameList(
                    section = null,
                    eventId = event.id.toInt(),
                    eventName = event.name,
                    preset = null,
                    libraryStatus = null,
                    libraryUserId = null,
                    pick = null,
                ),
            )
        }
        saveEvents(followedEvents.value - due.map { it.id }.toSet())
    }

    /**
     * The backlog games out since the last day checked (at most [MAX_CATCH_UP_DAYS] back, when the checks were
     * missed), once a day. Each game gets its own notification: few come out on the same day.
     */
    private suspend fun checkReleases(): Boolean {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val last = settings.getStringOrNull(RELEASES_DAY_KEY)?.let(LocalDate::parse)
        val due = auth.session.value.isAuthenticated && last != today
        val from = last?.takeIf { it.daysUntil(today) <= MAX_CATCH_UP_DAYS }?.plus(DatePeriod(days = 1)) ?: today
        val games = if (due) backlogReleasesOrNull(from, today) else emptyList()
        if (!games.isNullOrEmpty()) {
            val text = getString(Res.string.reminders__release)
            for (game in games) {
                notifier.notify(
                    NotificationChannel.RELEASES,
                    tag = "reminder.release.${game.id}",
                    title = game.name,
                    text = text,
                    target = RootNavTree.GameDetail(game.id.toInt(), game.cover?.image_id, game.name),
                )
            }
        }
        if (due && games != null) settings.putString(RELEASES_DAY_KEY, today.toString())
        return games != null
    }

    /** [backlogReleases], or null when the backend or IGDB didn't answer. */
    private suspend fun backlogReleasesOrNull(from: LocalDate, to: LocalDate): List<Game>? = try {
        backlogReleases(from, to)
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

    /** The backlog games first released from [from] to [to]; IGDB stores release days as UTC midnights. */
    private suspend fun backlogReleases(from: LocalDate, to: LocalDate): List<Game> {
        val ids = LibraryEntry.currentUserEntries(GameLibraryStatus.BACKLOG).allPages().map { it.gameId }.distinct()
        val start = from.atStartOfDayIn(TimeZone.UTC).epochSeconds
        val end = to.plus(DatePeriod(days = 1)).atStartOfDayIn(TimeZone.UTC).epochSeconds
        return ids.chunked(IGDB_MAX_LIMIT).flatMap { chunk ->
            igdb.getGames {
                fields(Game.field.name, Game.field.cover.image_id)
                where {
                    Game.field.id inAny chunk.map(Int::toString)
                    Game.field.first_release_date greaterThanOrEqual start
                    Game.field.first_release_date lessThan end
                }
                limit(chunk.size)
            }.games
        }
    }

    private fun saveEvents(events: Map<Long, FollowedEvent>) {
        followedEvents.value = events
        settings.putString(EVENTS_KEY, json.encodeToString(events.values.toList()))
    }

    companion object {
        /** How long before an event starts its reminder comes. */
        val EVENT_LEAD = 15.minutes

        /** Past its start by this much, an event isn't worth a reminder any more. */
        private val LATE_LIMIT = 1.hours

        /** A planned check may run a little early: an event due within it is told then, or never. */
        private val EARLY_SLACK = 5.minutes.inWholeSeconds

        private const val MAX_CATCH_UP_DAYS = 7
        private const val IGDB_MAX_LIMIT = 500
        private const val EVENTS_KEY = "reminders.events"
        private const val RELEASES_DAY_KEY = "reminders.releases.day"
        private val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * Runs the reminders in this process, so only while the app runs: the desktop, kept in the tray. Checks at each
 * event's time and every hour, which covers the daily releases and a retry after a failure.
 */
class InProcessReminderScheduler(private val reminders: Reminders) : ReminderScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun schedule(eventChecks: Collection<Long>) {
        job?.cancel()
        job = scope.launch {
            while (true) {
                reminders.check()
                val now = Clock.System.now().epochSeconds
                val next = eventChecks.filter { it > now }.minOrNull()?.coerceAtMost(now + HOUR) ?: (now + HOUR)
                delay((next - now).seconds)
            }
        }
    }

    private companion object {
        val HOUR = 1.hours.inWholeSeconds
    }
}
