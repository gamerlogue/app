package it.maicol07.gamerlogue.di

import io.ktor.client.HttpClient
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.auth.WebAuthTokenProvider
import it.maicol07.gamerlogue.auth.WebAuthenticationHandler
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
    )
}
