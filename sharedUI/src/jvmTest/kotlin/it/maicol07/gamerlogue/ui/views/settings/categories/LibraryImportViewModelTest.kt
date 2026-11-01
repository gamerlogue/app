package it.maicol07.gamerlogue.ui.views.settings.categories

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import com.russhwolf.settings.PreferencesSettings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.core.ExceptionReporter
import it.maicol07.gamerlogue.core.LibraryChanges
import it.maicol07.gamerlogue.data.ProfileApi
import it.maicol07.gamerlogue.services.GameMatcher
import it.maicol07.gamerlogue.services.LibrarySync
import it.maicol07.gamerlogue.services.TrackerSync
import it.maicol07.gamerlogue.services.connectors.BackloggdConnector
import it.maicol07.gamerlogue.services.connectors.BackloggeryConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.UUID
import java.util.prefs.Preferences

private val noRequestsIgdb = object : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("No IGDB requests expected")
    override suspend fun <T : Any> execute(endpoint: IgdbEndpoint<T>, query: ApicalypseQuery): Nothing =
        error("No IGDB requests expected")
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryImportViewModelTest : StringSpec({
    val preferences = Preferences.userRoot().node("gamerlogue-tests/${UUID.randomUUID()}")
    beforeTest { Dispatchers.setMain(StandardTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }
    afterSpec { preferences.removeNode() }

    for ((connector, shouldPush) in listOf(BackloggdConnector() to true, BackloggeryConnector() to true)) {
        "${connector.service}: an empty incoming wishlist advances only when outgoing writes are supported" {
            runTest {
                val reporter = ExceptionReporter()
                val matcher = GameMatcher(noRequestsIgdb, reporter)
                val library = LibrarySync(matcher, reporter, LibraryChanges(), ProfileApi())
                val tracker = TrackerSync(PreferencesSettings(preferences), matcher, library)
                ImportHandoff.put(connector.service, emptyList())
                val viewModel = LibraryImportViewModel(
                    connector.service, ImportMode.WISHLIST, mapOf(connector.service to connector), matcher, library, tracker,
                )
                try {
                    advanceUntilIdle()
                    viewModel.uiState.value.rows.shouldBeEmpty()
                    viewModel.uiState.value.matching shouldBe false
                    viewModel.hasNextStep shouldBe shouldPush

                    viewModel.confirm()
                    advanceUntilIdle()

                    viewModel.uiState.value.importedCount shouldBe 0
                    viewModel.uiState.value.pushPending shouldBe shouldPush
                    library.pendingWishlistImports[connector.service] shouldBe if (shouldPush) 0 else null
                    reporter.state.value.shouldBeNull()
                } finally {
                    ImportHandoff.take(connector.service)
                    viewModel.viewModelScope.cancel()
                }
            }
        }
    }
})

