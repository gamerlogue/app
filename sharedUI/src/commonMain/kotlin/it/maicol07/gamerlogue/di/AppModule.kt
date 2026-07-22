package it.maicol07.gamerlogue.di

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import it.maicol07.gamerlogue.services.EpicApi
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.PsnApi
import it.maicol07.gamerlogue.services.ServiceConnector
import it.maicol07.gamerlogue.services.UbisoftApi
import it.maicol07.gamerlogue.services.XboxApi
import it.maicol07.gamerlogue.services.connectors.EpicConnector
import it.maicol07.gamerlogue.services.connectors.GogConnector
import it.maicol07.gamerlogue.services.connectors.NintendoConnector
import it.maicol07.gamerlogue.services.connectors.PsnConnector
import it.maicol07.gamerlogue.services.connectors.SteamConnector
import it.maicol07.gamerlogue.services.connectors.UbisoftConnector
import it.maicol07.gamerlogue.services.connectors.XboxConnector
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Suppress("unused")
@ComponentScan("it.maicol07.gamerlogue")
@Configuration
@Module
object AppModule {
    @OptIn(ExperimentalSettingsApi::class)
    @Single
    fun provideSettings(): ObservableSettings = Settings().makeObservable()

    @Single
    fun provideConnectors(
        psnApi: PsnApi,
        xboxApi: XboxApi,
        epicApi: EpicApi,
        ubisoftApi: UbisoftApi,
    ): Map<ExternalService, ServiceConnector> = mapOf(
        ExternalService.STEAM to SteamConnector(),
        ExternalService.PLAYSTATION to PsnConnector(psnApi),
        ExternalService.XBOX to XboxConnector(xboxApi),
        ExternalService.GOG to GogConnector(),
        ExternalService.EPIC to EpicConnector(epicApi),
        ExternalService.NINTENDO to NintendoConnector(),
        ExternalService.UBISOFT to UbisoftConnector(ubisoftApi),
    )
}
