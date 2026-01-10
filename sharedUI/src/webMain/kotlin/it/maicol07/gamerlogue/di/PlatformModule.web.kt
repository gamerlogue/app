package it.maicol07.gamerlogue.di

import it.maicol07.gamerlogue.auth.AuthTokenProvider
import it.maicol07.gamerlogue.auth.WebAuthTokenProvider
import org.koin.dsl.module

actual val platformModule = module {
    single<AuthTokenProvider> { WebAuthTokenProvider() }
}

