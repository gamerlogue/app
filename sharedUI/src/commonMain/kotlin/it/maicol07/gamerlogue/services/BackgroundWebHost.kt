package it.maicol07.gamerlogue.services

import it.maicol07.gamerlogue.ui.components.SyncPhase
import it.maicol07.gamerlogue.ui.components.WebSession
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/** The store session in the WebView cookies is gone: only the user can sign in again, in the app. */
class LoginRequiredException(service: ExternalService) : IllegalStateException("$service needs a new sign-in")

/** Hosts the WebView of the syncs nobody watches (see [SyncCoordinator]). */
interface BackgroundWebHost {
    /** Runs [block] on a fresh session in a WebView nobody sees, released afterwards. */
    suspend fun <T> withSession(block: suspend (WebSession) -> T): T
}

/**
 * A session nobody watches: a missing sign-in fails fast with [LoginRequiredException] instead of waiting for the
 * user, and a push that asks for confirmation sends nothing.
 */
internal class UnattendedSession(private val session: WebSession) : WebSession by session {
    override suspend fun awaitLogin(connector: ServiceConnector) {
        // One WebView serves every service of a run, so it may still be on the previous one's page: open this one's
        // sign-in page, which a stored session moves past by itself.
        // ponytail: a headless session loads it once more in its own awaitLogin; a second or so per service.
        session.read(DataSource.Web(WebStep(connector.loginUrl, SyncScripts.wrap("")), parse = {}), SyncPhase.LOGGED_IN)
        withTimeoutOrNull(LOGIN_WAIT) { session.awaitLogin(connector) } ?: throw LoginRequiredException(connector.service)
    }

    override suspend fun confirmPush(games: List<LibrarySync.OutgoingGame>): List<LibrarySync.OutgoingGame> = emptyList()

    private companion object {
        /** A stored session moves past the login page within seconds (see HeadlessWebSession.awaitLogin). */
        val LOGIN_WAIT = 30.seconds
    }
}
