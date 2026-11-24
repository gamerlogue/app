package it.maicol07.gamerlogue.services

import androidx.navigation3.runtime.NavKey
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbResult
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.error.IgdbHttpErrorResponse
import com.russhwolf.settings.MapSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.LocalNotifier
import it.maicol07.gamerlogue.core.NotificationChannel
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private object NoIgdbCalls : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("No IGDB webhooks expected")
    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery,
    ): IgdbResult<T, IgdbHttpErrorResponse> = error("No IGDB calls expected")
}

/** Signed out: the release check stops before any request, leaving only the events. */
private class SignedOut : AuthTokenProvider() {
    init {
        restore()
    }

    override fun loadPersistedSession() = PersistedSession()
    override fun savePersistedSession(persisted: PersistedSession) = Unit
}

private class RecordingNotifier : LocalNotifier {
    val tags = mutableListOf<String>()
    override val isSupported = true
    override val openSystemSettings: (() -> Unit)? = null

    override suspend fun notify(channel: NotificationChannel, tag: String, title: String, text: String, target: NavKey?) {
        tags += tag
    }
}

class RemindersEventsTest : StringSpec({
    "an event within the lead is told and forgotten, a later one waits, a long-started one is dropped silently" {
        val now = Clock.System.now().epochSeconds
        val events = listOf(
            FollowedEvent(1, "Soon", now + 10.minutes.inWholeSeconds),
            FollowedEvent(2, "Later", now + 2.hours.inWholeSeconds),
            FollowedEvent(3, "Long gone", now - 2.hours.inWholeSeconds),
            FollowedEvent(4, "Just started", now - 10.minutes.inWholeSeconds),
        )
        val settings = MapSettings().apply { putString("reminders.events", Json.encodeToString(events)) }
        val notifier = RecordingNotifier()
        val reminders = Reminders(settings, ReleaseReminders(settings, NoIgdbCalls, SignedOut(), notifier), notifier)

        reminders.check() shouldBe true

        notifier.tags shouldBe listOf("reminder.event.1", "reminder.event.4")
        reminders.followedEvents.value.keys shouldBe setOf(2L)
    }
})
