package it.maicol07.gamerlogue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import it.maicol07.gamerlogue.auth.AuthHandler
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.ui.components.layout.AppScaffold
import it.maicol07.gamerlogue.ui.components.layout.GlobalExceptionBottomSheet
import it.maicol07.gamerlogue.ui.navigation.AppNavDisplay
import it.maicol07.gamerlogue.ui.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.plugin.module.dsl.koinConfiguration

@KoinApplication
private object KoinApp

@OptIn(KoinExperimentalAPI::class)
@Composable
fun App(authCallbackUri: String? = null) {
    val backStack = rememberNavBackStack(NavKeys.savedStateConfiguration, NavKeys.Discover)
    val showBottomBar = (backStack.last() as? NavKeys.NavKeyWithMeta)?.showBottomBar ?: true
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
