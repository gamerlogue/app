package it.maicol07.gamerlogue.di

import at.released.igdbclient.model.Game
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.GameMatcher
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.connectors.EpicConnector
import it.maicol07.gamerlogue.services.connectors.GogConnector
import it.maicol07.gamerlogue.services.connectors.PsnConnector
import it.maicol07.gamerlogue.services.connectors.SteamConnector
import it.maicol07.gamerlogue.services.connectors.XboxConnector
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import it.maicol07.gamerlogue.ui.views.game.GameDetailViewModel
import it.maicol07.gamerlogue.ui.views.discover.DiscoverViewModel
import it.maicol07.gamerlogue.ui.views.library.LibraryViewModel
import it.maicol07.gamerlogue.ui.views.library.components.AddToLibrarySheetViewModel
import it.maicol07.gamerlogue.ui.views.list.GameListViewModel
import it.maicol07.gamerlogue.ui.views.settings.SettingsViewModel
import it.maicol07.gamerlogue.ui.views.settings.categories.ImportMode
import it.maicol07.gamerlogue.ui.views.settings.categories.LibraryImportViewModel
import it.maicol07.gamerlogue.ui.views.settings.categories.LinkedServicesViewModel
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

    // Linked services (account linking + library/wishlist sync)
    single { GameMatcher(get()) }
    single { LibrarySync(get(), get()) }
    single<Map<ExternalService, ServiceConnector>> {
        mapOf(
            ExternalService.STEAM to SteamConnector(),
            ExternalService.PLAYSTATION to PsnConnector(get()),
            ExternalService.XBOX to XboxConnector(get()),
            ExternalService.GOG to GogConnector(),
            ExternalService.EPIC to EpicConnector(),
        )
    }
    viewModel { LinkedServicesViewModel(get(), get(), get()) }
    viewModel { (service: ExternalService, mode: ImportMode) ->
        LibraryImportViewModel(service, mode, get(), get(), get())
    }
}
