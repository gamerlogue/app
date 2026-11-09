package it.maicol07.gamerlogue.core

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.push.PushListener
import com.mmk.kmpnotifier.push.firebase.addPushListener
import com.mmk.kmpnotifier.push.firebase.firebasePushNotifier
import com.mmk.kmpnotifier.push.firebase.removePushListener
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.data.ProfileApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single

/**
 * Hands this device's FCM token to the backend while a user is signed in, and refreshes the unread count when a push
 * arrives. Only the Android app initializes KMPNotifier, so elsewhere this does nothing; without a Firebase config the
 * token is null and nothing is sent either.
 *
 * On sign-out the token is deleted rather than unregistered, since the bearer is already gone: the backend drops it
 * when FCM answers that it is no longer registered.
 */
@Single
class PushNotifications(
    private val api: ProfileApi,
    private val auth: AuthTokenProvider,
    private val unread: UnreadNotifications,
    private val reporter: ExceptionReporter,
) {
    suspend fun run() {
        if (!KMPNotifier.isInitialized) return
        val notifier = KMPNotifier.firebasePushNotifier
        val tokens = callbackFlow {
            val listener = object : PushListener {
                override fun onNewToken(token: String) {
                    trySend(token)
                }

                override fun onPushNotification(title: String?, body: String?) {
                    launch { unread.refresh() }
                }
            }
            KMPNotifier.addPushListener(listener)
            notifier.getToken()?.let { send(it) }
            awaitClose { KMPNotifier.removePushListener(listener) }
        }
        val signedIn = auth.session.map { it.userId != null }.distinctUntilChanged()
        var registered = false
        combine(signedIn, tokens, ::Pair).collectLatest { (signedIn, token) ->
            when {
                signedIn -> registered = reporter.safeRequest { api.registerPushToken(token) }.isOk
                registered -> {
                    notifier.deleteMyToken()
                    registered = false
                }
            }
        }
    }
}
