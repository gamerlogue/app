package it.maicol07.gamerlogue.di

import io.ktor.client.HttpClient
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.auth.WebAuthTokenProvider
import it.maicol07.gamerlogue.auth.WebAuthenticationHandler
import it.maicol07.gamerlogue.services.BackgroundWebHost
import it.maicol07.gamerlogue.services.InProcessSyncScheduler
import it.maicol07.gamerlogue.services.SyncScheduler
import it.maicol07.gamerlogue.services.SyncStorage
import it.maicol07.gamerlogue.services.WebSyncStorage
import it.maicol07.gamerlogue.ui.components.WebSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

@Module
@Configuration
actual object PlatformModule {
    @Single
    actual fun provideAuthTokenProvider(scope: Scope): AuthTokenProvider = WebAuthTokenProvider()

    @Single
    actual fun provideAuthenticationHandler(scope: Scope): AuthenticationHandler = WebAuthenticationHandler(
        authProvider = scope.get(),
        authClient = scope.get<HttpClient>(named("AuthHttpClient")),
        serverUrl = { scope.get<AppPreferences>().serverUrl.value },
    )

    // Store syncs need a WebView the browser can't host (see isServiceSyncSupported): nothing ever asks for one.
    @Single
    actual fun provideBackgroundWebHost(scope: Scope): BackgroundWebHost = object : BackgroundWebHost {
        override suspend fun <T> withSession(block: suspend (WebSession) -> T): T =
            throw UnsupportedOperationException("Store syncs don't run in the browser")
    }

    @Single
    actual fun provideSyncScheduler(scope: Scope): SyncScheduler = InProcessSyncScheduler(scope.get(), scope.get())

    @Single
    actual fun provideSyncStorage(scope: Scope): SyncStorage = WebSyncStorage(CoroutineScope(SupervisorJob()))
}
