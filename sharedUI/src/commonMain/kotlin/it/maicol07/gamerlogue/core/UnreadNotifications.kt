package it.maicol07.gamerlogue.core

import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.data.ProfileApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The signed-in user's unread notifications count, shared by the bell of every top bar. */
@Single
class UnreadNotifications(private val api: ProfileApi, private val reporter: ExceptionReporter) {
    val count: StateFlow<Int>
        field = MutableStateFlow(0)

    private var fetchedAt: Instant? = null

    /** Fetches the count, unless it was fetched moments ago: every screen's bar asks when it shows. */
    suspend fun refreshIfStale() {
        val last = fetchedAt
        if (last != null && Clock.System.now() - last < StaleAfter) return
        refresh()
    }

    suspend fun refresh() {
        fetchedAt = Clock.System.now()
        reporter.safeRequest { api.unreadNotificationsCount() }.get()?.let { count.value = it }
    }

    private companion object {
        val StaleAfter = 30.seconds
    }
}
