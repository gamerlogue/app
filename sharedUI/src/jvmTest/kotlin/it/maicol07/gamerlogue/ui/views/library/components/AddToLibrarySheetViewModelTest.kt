package it.maicol07.gamerlogue.ui.views.library.components

import at.released.igdbclient.IgdbClient
import at.released.igdbclient.IgdbEndpoint
import at.released.igdbclient.IgdbWebhookApi
import at.released.igdbclient.apicalypse.ApicalypseQuery
import at.released.igdbclient.model.Game
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.core.ExceptionReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

// The ViewModel's init block loads game editions via IgdbClient (see AddToLibrarySheetViewModel);
// this fake lets it fail harmlessly (caught by safeRequest) without a real IGDB backend.
private val fakeIgdbClient = object : IgdbClient {
    override val webhookApi: IgdbWebhookApi get() = error("not used in test")
    override suspend fun <T : Any> execute(
        endpoint: IgdbEndpoint<T>,
        query: ApicalypseQuery,
    ): Nothing = error("not used in test")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddToLibrarySheetViewModelTest : StringSpec({
    beforeTest {
        Dispatchers.setMain(StandardTestDispatcher())
        startKoin {
            modules(
                module {
                    single { ExceptionReporter() }
                    single<IgdbClient> { fakeIgdbClient }
                }
            )
        }
    }
    afterTest {
        Dispatchers.resetMain()
        stopKoin()
    }

    "saveEntry without a selected status sets an error and skips save" {
        runTest {
            val viewModel = AddToLibrarySheetViewModel(game = Game(id = 1L), existingEntry = null)
            var savedEntry = false

            viewModel.saveEntry { savedEntry = true }
            advanceUntilIdle()

            viewModel.error.shouldNotBeNull()
            savedEntry shouldBe false
            viewModel.saveLoading shouldBe false
        }
    }
})
