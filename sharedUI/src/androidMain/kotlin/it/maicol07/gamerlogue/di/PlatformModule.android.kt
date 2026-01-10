package it.maicol07.gamerlogue.di

import it.maicol07.gamerlogue.auth.AndroidAuthTokenProvider
import it.maicol07.gamerlogue.auth.AuthTokenProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single<AuthTokenProvider> { AndroidAuthTokenProvider(androidContext()) }
}

