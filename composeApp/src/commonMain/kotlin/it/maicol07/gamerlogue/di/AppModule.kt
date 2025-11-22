package it.maicol07.gamerlogue.di

import it.maicol07.gamerlogue.ui.views.game.GameDetailViewModel
import it.maicol07.gamerlogue.ui.views.home.HomeViewModel
import it.maicol07.gamerlogue.ui.views.list.GameListType
import it.maicol07.gamerlogue.ui.views.list.GameListViewModel
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module

@Module
@Configuration
object AppModule {
    @KoinViewModel
    fun provideHomeViewModel() = HomeViewModel()

    @KoinViewModel
    fun provideGameListViewModel(@InjectedParam type: GameListType) = GameListViewModel(type)

    @KoinViewModel
    fun provideGameDetailViewModel(@InjectedParam gameId: Int) = GameDetailViewModel(gameId)
}

@KoinApplication
object KoinApp

