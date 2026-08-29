package it.maicol07.gamerlogue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import kotlinx.serialization.serializer
import it.maicol07.gamerlogue.auth.AuthHandler
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.ui.components.layout.AppScaffold
import it.maicol07.gamerlogue.ui.components.layout.GlobalExceptionBottomSheet
import it.maicol07.gamerlogue.ui.navigation.AppNavDisplay
import it.maicol07.gamerlogue.ui.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.koinConfiguration

@KoinApplication
private object KoinApp

@Composable
fun App(authCallbackUri: String? = null) {
    // Closed polymorphism: AppNavKey is sealed, so no SerializersModule registration is needed.
    val backStack = rememberSerializable(serializer = serializer<NavBackStack<AppNavKey>>()) {
        NavBackStack(NavKeys.Discover)
    }
    val showBottomBar = backStack.last().showBottomBar
    NavigationBarContrastEnforced(!showBottomBar)

    KoinApplication(koinConfiguration<KoinApp>()) {
        CompositionLocalProvider(LocalNavBackStack provides backStack) {
            AuthHandler(authCallbackUri)

            AppTheme {
                AppScaffold(currentNavKey = backStack.last()) {
                    Column(
                        modifier = Modifier.padding(it),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppNavDisplay(backStack)

                        val reporter = koinInject<ExceptionReporter>()
                        val sheetOpen by reporter.sheetOpen.collectAsState()
                        if (sheetOpen) {
                            GlobalExceptionBottomSheet()
                        }
                    }
                }
            }
        }
    }
}
