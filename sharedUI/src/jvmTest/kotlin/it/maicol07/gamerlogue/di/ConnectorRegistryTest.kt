package it.maicol07.gamerlogue.di

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import it.maicol07.gamerlogue.services.EpicApi
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.services.PsnApi
import it.maicol07.gamerlogue.services.UbisoftApi
import it.maicol07.gamerlogue.services.XboxApi

/**
 * `LinkedServicesViewModel.connector()` reads the registry with `getValue`, so a service without a
 * connector is a runtime crash on the first lookup — and adding an [ExternalService] entry compiles
 * perfectly well without one.
 */
class ConnectorRegistryTest : StringSpec({
    // The clients are never called: the test only constructs the connectors to inspect their identity.
    val connectors = AppModule.provideConnectors(
        PsnApi(HttpClient()),
        XboxApi(HttpClient()),
        EpicApi(HttpClient()),
        UbisoftApi(HttpClient()),
    )

    "every external service has a connector" {
        connectors.keys shouldContainExactlyInAnyOrder ExternalService.entries
    }

    "each connector is registered under the service it declares" {
        connectors.forEach { (service, connector) -> connector.service shouldBe service }
    }
})
