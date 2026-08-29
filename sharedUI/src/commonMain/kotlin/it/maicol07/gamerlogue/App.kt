package it.maicol07.gamerlogue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import it.maicol07.gamerlogue.auth.AuthHandler
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
 * the tree. The current destination is read where it is used, inside the bottom bar.
 */
@Composable
fun App(
    authCallbackUri: String? = null,
    onAuthCallbackHandled: () -> Unit = {},
) {
    // Closed polymorphism: AppNavKey is sealed, so no SerializersModule registration is needed.
    val backStack = rememberSerializable(serializer = serializer<NavBackStack<AppNavKey>>()) {
        NavBackStack(NavKeys.Discover)
    }

    KoinApplication(koinConfiguration<KoinApp>()) {
        CompositionLocalProvider(LocalNavBackStack provides backStack) {
            AuthHandler(authCallbackUri, onAuthCallbackHandled)

            AppTheme {
                AppScaffold {
                    Box(Modifier.padding(it)) {
                        AppNavDisplay(backStack)

                        val reporter = koinInject<ExceptionReporter>()
                        val errorState by reporter.state.collectAsState()
                        if (errorState?.sheetOpen == true) {
                            GlobalExceptionBottomSheet()
                        }
                    }
                }
            }
        }
    }
}
