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

@ComponentScan("it.maicol07.gamerlogue")
@Configuration
@Module
object AppModule {
    @OptIn(ExperimentalSettingsApi::class)
    @Single
    fun provideSettings(): ObservableSettings = Settings().makeObservable()

    /**
     * Every connector, keyed by the service it declares. Built with `associateBy` rather than a
     * hand-written map: the key is already on the connector, and a map literal lets the two drift —
     * a new [ExternalService] would compile and then crash on the first lookup.
     */
    @Single
    fun provideConnectors(
        psnApi: PsnApi,
        xboxApi: XboxApi,
        epicApi: EpicApi,
        ubisoftApi: UbisoftApi,
    ): Map<ExternalService, ServiceConnector> = listOf(
        SteamConnector(),
        PsnConnector(psnApi),
        XboxConnector(xboxApi),
        GogConnector(),
        EpicConnector(epicApi),
        NintendoConnector(),
        UbisoftConnector(ubisoftApi),
    ).associateBy { it.service }
}
