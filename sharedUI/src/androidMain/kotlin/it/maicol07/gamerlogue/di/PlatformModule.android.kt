package it.maicol07.gamerlogue.di

import android.content.Context
import io.ktor.client.HttpClient
import it.maicol07.gamerlogue.auth.AndroidAuthTokenProvider
import it.maicol07.gamerlogue.auth.AndroidAuthenticationHandler
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.services.AndroidBackgroundWebHost
import it.maicol07.gamerlogue.services.BackgroundWebHost
import it.maicol07.gamerlogue.services.FileSyncStorage
import it.maicol07.gamerlogue.services.SyncScheduler
import it.maicol07.gamerlogue.services.SyncStorage
import it.maicol07.gamerlogue.services.WorkManagerSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

@Module
@Configuration
actual object PlatformModule {
    @Single
    actual fun provideAuthTokenProvider(scope: Scope): AuthTokenProvider = AndroidAuthTokenProvider(scope.get())

    // The application Context is enough: the Custom Tab is launched with FLAG_ACTIVITY_NEW_TASK.
    @Single
    actual fun provideAuthenticationHandler(scope: Scope): AuthenticationHandler = AndroidAuthenticationHandler(
        context = scope.get(),
        authProvider = scope.get(),
        authClient = scope.get<HttpClient>(named("AuthHttpClient")),
        serverUrl = { scope.get<AppPreferences>().serverUrl.value },
    )

    @Single
    actual fun provideBackgroundWebHost(scope: Scope): BackgroundWebHost = AndroidBackgroundWebHost(scope.get())

    @Single
    actual fun provideSyncScheduler(scope: Scope): SyncScheduler = WorkManagerSyncScheduler(scope.get())

    @Single
    actual fun provideSyncStorage(scope: Scope): SyncStorage = FileSyncStorage(
        fileSystem = FileSystem.SYSTEM,
        directory = scope.get<Context>().filesDir.toOkioPath() / "sync",
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    )
}
