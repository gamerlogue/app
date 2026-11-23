package it.maicol07.gamerlogue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import it.maicol07.gamerlogue.auth.AuthHandler
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.PushNotifications
import it.maicol07.gamerlogue.di.KoinApp
import it.maicol07.gamerlogue.services.ReminderScheduler
import it.maicol07.gamerlogue.services.Reminders
import it.maicol07.gamerlogue.services.SyncCoordinator
import it.maicol07.gamerlogue.services.SyncScheduler
import it.maicol07.gamerlogue.ui.components.layout.AppScaffold
import it.maicol07.gamerlogue.ui.components.layout.GlobalExceptionBottomSheet
import it.maicol07.gamerlogue.ui.navigation.AppNavDisplay
import it.maicol07.gamerlogue.ui.navigation.LocalNavigationState
import it.maicol07.gamerlogue.ui.navigation.rememberAppNavigationState
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.plugin.module.dsl.koinConfiguration

/**
 * This function deliberately reads nothing from the back stack. Doing so would recompose it on every
 * push and pop, and with it [KoinApplication] — whose configuration is rebuilt on each recomposition,
 * re-providing the Koin composition locals and invalidating every `koinInject` / `koinViewModel` in
 * the tree. The current destination is read where it is used, inside the app shell and nav host.
 */
@Composable
fun App(
    authCallbackUri: String? = null,
    onAuthCallbackHandled: () -> Unit = {},
    openDestination: NavKey? = null,
    onDestinationOpened: () -> Unit = {},
) {
    val navigationState = rememberAppNavigationState()

    KoinApplication(koinConfiguration<KoinApp>()) {
        CompositionLocalProvider(LocalNavigationState provides navigationState) {
            AuthHandler(authCallbackUri, onAuthCallbackHandled)
            LocalNetworkAccess(koinInject<AppPreferences>().serverUrl.collectAsStateWithLifecycle().value)
            PushNotificationsHost()
            BackgroundSyncHost()
            ReminderHost()
            // A tapped notification. The notifications list lives under the profile, where its bell is.
            LaunchedEffect(openDestination) {
                val destination = openDestination ?: return@LaunchedEffect
                if (destination == RootNavTree.Notifications) navigationState.selectRoot(RootNavTree.Profile)
                navigationState.backStack.add(destination)
                onDestinationOpened()
            }

            // Compose resources read the platform locale during composition, so an in-app language
            // change is only picked up by composing the tree again. Keyed here rather than higher up
            // so the Koin root survives; the cost is that navigation restarts at the root, which is
            // also what Android does on its own when AppCompat recreates the activity.
            val language by koinInject<AppPreferences>().language.collectAsStateWithLifecycle()
            key(language) {
                AppTheme {
                    AppScaffold {
                        Box(Modifier.padding(it)) {
                            AppNavDisplay(navigationState)

                            GlobalErrorHost()
                        }
                    }
                }
            }
        }
    }
}

// Its own scope, so a sign-in or sign-out recomposes only this.
@Composable
private fun PushNotificationsHost() {
    val session by koinInject<AuthTokenProvider>().session.collectAsStateWithLifecycle()
    // Asked once something would notify: a signed-in user's pushes and syncs, or an event followed signed out.
    val followsEvents by koinInject<Reminders>().followedEvents.collectAsStateWithLifecycle()
    NotificationPermission(session.isAuthenticated || followsEvents.isNotEmpty())
    val pushNotifications = koinInject<PushNotifications>()
    LaunchedEffect(pushNotifications) { pushNotifications.run() }
}

// Signed in: the background sync needs the library. It lasts as long as the app's composition, so on the desktop it
// goes on with the window hidden in the tray; on Android WorkManager runs the syncs once the activity is gone.
@Composable
private fun BackgroundSyncHost() {
    val session by koinInject<AuthTokenProvider>().session.collectAsStateWithLifecycle()
    if (!session.isAuthenticated) return
    val coordinator = koinInject<SyncCoordinator>()
    val scheduler = koinInject<SyncScheduler>()
    LaunchedEffect(coordinator, scheduler) { coordinator.watch(scheduler) }
}

// For as long as the app's composition lives, like the background sync; on Android WorkManager keeps the plan after.
@Composable
private fun ReminderHost() {
    val reminders = koinInject<Reminders>()
    val scheduler = koinInject<ReminderScheduler>()
    LaunchedEffect(reminders, scheduler) { reminders.watch(scheduler) }
}

@Composable
private fun GlobalErrorHost() {
    val reporter = koinInject<ExceptionReporter>()
    val errorState by reporter.state.collectAsStateWithLifecycle()
    errorState?.takeIf { it.sheetOpen }?.let { GlobalExceptionBottomSheet(it, reporter) }
}
