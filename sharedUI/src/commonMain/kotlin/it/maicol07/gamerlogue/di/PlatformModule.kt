package it.maicol07.gamerlogue.di
import it.maicol07.gamerlogue.auth.AuthTokenProvider
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
}
