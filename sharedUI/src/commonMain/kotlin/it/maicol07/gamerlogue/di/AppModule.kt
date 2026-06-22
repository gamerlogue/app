package it.maicol07.gamerlogue.di

import at.released.igdbclient.model.Game
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import it.maicol07.gamerlogue.ui.views.game.GameDetailViewModel
import it.maicol07.gamerlogue.ui.views.discover.DiscoverViewModel
import it.maicol07.gamerlogue.ui.views.library.LibraryViewModel
import it.maicol07.gamerlogue.ui.views.library.components.AddToLibrarySheetViewModel
import it.maicol07.gamerlogue.ui.views.list.GameListViewModel
import it.maicol07.gamerlogue.ui.views.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@OptIn(ExperimentalSettingsApi::class)
val appModule = module {
    single { Settings().makeObservable() }
    viewModel { DiscoverViewModel() }
    viewModel { (section: DiscoverSection) -> GameListViewModel(section) }
    viewModel { (gameId: Int) -> GameDetailViewModel(gameId) }
    viewModel { LibraryViewModel() }
    viewModel { (game: Game, existingEntry: LibraryEntry?) -> AddToLibrarySheetViewModel(game, existingEntry) }
    viewModel { SettingsViewModel(get(), get()) }
}
