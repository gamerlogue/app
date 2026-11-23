package it.maicol07.gamerlogue.di

import io.ktor.client.HttpClient
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.core.AppPreferences
import it.maicol07.gamerlogue.core.LocalNotifier
import it.maicol07.gamerlogue.auth.JvmAuthTokenProvider
import it.maicol07.gamerlogue.auth.JvmAuthenticationHandler
import it.maicol07.gamerlogue.services.BackgroundWebHost
import it.maicol07.gamerlogue.services.DesktopBackgroundWebHost
import it.maicol07.gamerlogue.services.DesktopNotifier
import it.maicol07.gamerlogue.services.InProcessReminderScheduler
import it.maicol07.gamerlogue.services.InProcessSyncScheduler
import it.maicol07.gamerlogue.services.FileSyncStorage
import it.maicol07.gamerlogue.services.ReminderScheduler
import it.maicol07.gamerlogue.services.SyncScheduler
import it.maicol07.gamerlogue.services.SyncStorage
import it.maicol07.gamerlogue.services.appDataDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope

@Module
@Configuration
actual object PlatformModule {
    @Single
    actual fun provideAuthTokenProvider(scope: Scope): AuthTokenProvider = JvmAuthTokenProvider()

    @Single
    actual fun provideAuthenticationHandler(scope: Scope): AuthenticationHandler = JvmAuthenticationHandler(
        authProvider = scope.get(),
        authClient = scope.get<HttpClient>(named("AuthHttpClient")),
        serverUrl = { scope.get<AppPreferences>().serverUrl.value },
    )

    @Single
    actual fun provideBackgroundWebHost(scope: Scope): BackgroundWebHost = DesktopBackgroundWebHost

    @Single
    actual fun provideSyncScheduler(scope: Scope): SyncScheduler =
        InProcessSyncScheduler(scope.get(), scope.get(), scope.get())

    @Single
    actual fun provideSyncStorage(scope: Scope): SyncStorage = FileSyncStorage(
        fileSystem = FileSystem.SYSTEM,
        directory = appDataDirectory() / "sync",
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    )

    @Single
    actual fun provideLocalNotifier(scope: Scope): LocalNotifier = DesktopNotifier

    @Single
    actual fun provideReminderScheduler(scope: Scope): ReminderScheduler = InProcessReminderScheduler(scope.get())
}
