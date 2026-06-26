package it.maicol07.gamerlogue

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.github.michaelbull.result.unwrap
import io.ktor.http.decodeURLQueryComponent
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.rememberAuthenticationHandler
import it.maicol07.gamerlogue.data.User
import it.maicol07.gamerlogue.data.UserStore
import it.maicol07.gamerlogue.di.appModule
import it.maicol07.gamerlogue.di.httpModule
import it.maicol07.gamerlogue.di.platformModule
import it.maicol07.gamerlogue.ui.components.layout.AppScaffold
import it.maicol07.gamerlogue.ui.components.layout.GlobalExceptionBottomSheet
import it.maicol07.gamerlogue.ui.components.layout.LocalAppUiState
import it.maicol07.gamerlogue.ui.navigation.AppNavDisplay
import it.maicol07.gamerlogue.ui.theme.AppTheme
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.module

@OptIn(KoinExperimentalAPI::class)
@Composable
fun App(authCallbackUri: String? = null) {
    val backStack = rememberNavBackStack(NavKeys.savedStateConfiguration, NavKeys.Discover)

    KoinApplication(KoinConfiguration {
        modules(
            appModule,
            httpModule,
            platformModule,
            // Compose specific module to provide the NavBackStack
            module {
                single<NavBackStack> { backStack }
            }
        )
    }
    ) {
        AuthHandler(authCallbackUri)

        AppTheme {
            AppScaffold(currentNavKey = backStack.last()) {
                Column(
                    modifier = Modifier.padding(it),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppNavDisplay(backStack)

                    val appUiState = LocalAppUiState.current
                    val showExceptionBottomSheet by remember {
                        derivedStateOf {
                            appUiState.networkException != null && appUiState.showExceptionBottomSheet
                        }
                    }
                    if (showExceptionBottomSheet) {
                        GlobalExceptionBottomSheet()
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthHandler(authCallbackUri: String?) {
    val authProvider = koinInject<AuthTokenProvider>()
    val authHandler = rememberAuthenticationHandler()
    val userStore = remember { UserStore() }

    // Handle the login callback here, inside the Koin composition, so the token lands on the
    // same AuthTokenProvider singleton the Ktor client reads (the Activity has its own Koin-less scope).
    LaunchedEffect(authCallbackUri) {
        if (authCallbackUri != null) {
            // Backend double-URL-encodes the token, so Sanctum's "id|hash" arrives as "id%257Chash".
            // ponytail: fully decode (safe: token has no literal '%'); single decode if the backend stops double-encoding.
            authHandler.handleCallback(authCallbackUri) { raw ->
                var value = raw
                while (true) {
                    val decoded = value.decodeURLQueryComponent(plusIsSpace = true)
                    if (decoded == value) break
                    value = decoded
                }
                value
            }
        }
    }

    LaunchedEffect(Unit) {
        if (BuildConfig.APP_ENV === AppEnvironment.LOCAL) {
            Logger.setMinSeverity(Severity.Verbose)
            Logger.i("Running in LOCAL environment")
        }
        val savedUser = userStore.getUser()
        if (savedUser != null) {
            authProvider.currentUser = savedUser
        }
    }

    LaunchedEffect(authProvider.accessToken, authProvider.currentUserId) {
        Logger.d("AuthState changed: token=${authProvider.accessToken}, userId=${authProvider.currentUserId}")
        if (authProvider.accessToken != null) {
            if (authProvider.currentUser == null && authProvider.currentUserId != null) {
                val result = safeRequest { User.find(authProvider.currentUserId!!).data }
                if (result.isOk) {
                    val user = result.unwrap()
                    authProvider.currentUser = user
                    userStore.saveUser(user)
                }
            }
        } else {
            authProvider.currentUser = null
            authProvider.updateUserId(null)
            userStore.clear()
        }
    }
}
