package it.maicol07.gamerlogue.di

import io.ktor.client.HttpClient
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import it.maicol07.gamerlogue.auth.JvmAuthTokenProvider
import it.maicol07.gamerlogue.auth.JvmAuthenticationHandler
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
    )
}
