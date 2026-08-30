package it.maicol07.gamerlogue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.maicol07.gamerlogue.auth.AuthHandler
import it.maicol07.gamerlogue.auth.LocalAuthenticationHandler
import it.maicol07.gamerlogue.auth.rememberAuthenticationHandler
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.di.KoinApp
import it.maicol07.gamerlogue.ui.components.layout.AppScaffold
import it.maicol07.gamerlogue.ui.components.layout.GlobalExceptionBottomSheet
import it.maicol07.gamerlogue.ui.navigation.AppNavDisplay
import it.maicol07.gamerlogue.ui.theme.AppTheme
import kotlinx.serialization.serializer
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
) {
    // One saveable stack per top-level destination preserves nested navigation and screen state.
    val discoverBackStack = rememberSerializable(serializer = serializer<NavBackStack<AppNavKey>>()) {
        NavBackStack(NavKeys.Discover)
    }
    val libraryBackStack = rememberSerializable(serializer = serializer<NavBackStack<AppNavKey>>()) {
        NavBackStack(NavKeys.Library)
    }
    val calendarBackStack = rememberSerializable(serializer = serializer<NavBackStack<AppNavKey>>()) {
        NavBackStack(NavKeys.Calendar)
    }
    val profileBackStack = rememberSerializable(serializer = serializer<NavBackStack<AppNavKey>>()) {
        NavBackStack(NavKeys.Profile)
    }
    val selectedRootIndex = rememberSaveable { mutableIntStateOf(0) }
    val navigationState = remember(
        selectedRootIndex,
        discoverBackStack,
        libraryBackStack,
        calendarBackStack,
        profileBackStack,
    ) {
        AppNavigationState(
            selectedRootIndex,
            listOf(discoverBackStack, libraryBackStack, calendarBackStack, profileBackStack),
        )
    }

    KoinApplication(koinConfiguration<KoinApp>()) {
        val authHandler = rememberAuthenticationHandler()
        CompositionLocalProvider(
            LocalNavigationState provides navigationState,
            LocalAuthenticationHandler provides authHandler,
        ) {
            AuthHandler(authCallbackUri, onAuthCallbackHandled)

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

@Composable
private fun GlobalErrorHost() {
    val reporter = koinInject<ExceptionReporter>()
    val errorState by reporter.state.collectAsStateWithLifecycle()
    errorState?.takeIf { it.sheetOpen }?.let { GlobalExceptionBottomSheet(it, reporter) }
}
