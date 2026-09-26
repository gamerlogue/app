package it.maicol07.gamerlogue.di

import io.ktor.client.HttpClient
import it.maicol07.gamerlogue.auth.AndroidAuthTokenProvider
import it.maicol07.gamerlogue.auth.AndroidAuthenticationHandler
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
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
    )
}
