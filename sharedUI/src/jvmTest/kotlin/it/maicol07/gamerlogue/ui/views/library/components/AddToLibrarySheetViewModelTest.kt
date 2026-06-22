package it.maicol07.gamerlogue.ui.views.library.components

import at.released.igdbclient.model.Game
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AddToLibrarySheetViewModelTest : StringSpec({
    beforeTest { Dispatchers.setMain(StandardTestDispatcher()) }
    afterTest { Dispatchers.resetMain() }

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
