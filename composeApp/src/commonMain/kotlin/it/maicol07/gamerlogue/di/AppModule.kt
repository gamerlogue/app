package it.maicol07.gamerlogue.di

import it.maicol07.gamerlogue.ui.views.game.GameDetailViewModel
import it.maicol07.gamerlogue.ui.views.home.HomeViewModel
import it.maicol07.gamerlogue.ui.views.list.GameListType
import it.maicol07.gamerlogue.ui.views.list.GameListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { HomeViewModel() }
    viewModel { (type: GameListType) -> GameListViewModel(type) }
    viewModel { (gameId: Int) -> GameDetailViewModel(gameId) }
}
