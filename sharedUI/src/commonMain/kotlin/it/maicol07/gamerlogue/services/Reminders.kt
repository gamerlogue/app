package it.maicol07.gamerlogue.services

import at.released.igdbclient.model.Event
import com.russhwolf.settings.ObservableSettings
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.reminders__event_started
import gamerlogue.sharedui.generated.resources.reminders__event_starts
import it.maicol07.gamerlogue.core.LocalNotifier
import it.maicol07.gamerlogue.core.NotificationChannel
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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

/** The game list scoped to the event, as for an [Event]'s `gamesNavKey`. */
val FollowedEvent.gamesNavKey: RootNavTree.GameList
    get() = RootNavTree.GameList(
        section = null,
        eventId = id.toInt(),
        eventName = name,
        preset = null,
        libraryStatus = null,
        libraryUserId = null,
        pick = null,
    )

/** When the reminders run: told by [Reminders.watch], each run calls [Reminders.check]. */
interface ReminderScheduler {
    /** Plans a [Reminders.check] once a day, and one at each of [eventChecks] (epoch seconds). */
    fun schedule(eventChecks: Collection<Long>)
}

/**
 * The reminders the app gives on its own, without the backend: the IGDB events the user follows on this device,
 * [EVENT_LEAD] before they start, and the [releases] of the user's games, once a day.
 */
@Single
class Reminders(
    private val settings: ObservableSettings,
    private val releases: ReleaseReminders,
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

    fun unfollow(id: Long) = saveEvents(followedEvents.value - id)

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
        return releases.check()
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
                target = event.gamesNavKey,
            )
        }
        saveEvents(followedEvents.value - due.map { it.id }.toSet())
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

        private const val EVENTS_KEY = "reminders.events"
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
