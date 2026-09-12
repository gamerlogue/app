package it.maicol07.gamerlogue.di
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthenticationHandler
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import org.koin.core.scope.Scope

@Suppress("unused")
@Module
@Configuration
expect object PlatformModule {
    @Single
    fun provideAuthTokenProvider(scope: Scope): AuthTokenProvider

    /**
     * The auth client is resolved off the [Scope] rather than declared as a `@Named` parameter: the Koin
     * compiler plugin (1.0.2) drops qualified parameters silently, the same quirk documented on
     * [it.maicol07.gamerlogue.data.AppJsonApiConfig]. `PlatformModuleBindingTest` pins the wiring.
     */
    @Single
    fun provideAuthenticationHandler(scope: Scope): AuthenticationHandler
}
