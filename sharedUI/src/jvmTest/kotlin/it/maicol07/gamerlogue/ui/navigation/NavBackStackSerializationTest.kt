package it.maicol07.gamerlogue.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTree
import it.maicol07.gamerlogue.ui.navigation.rootTree.RootNavTreeLayout
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import it.maicol07.gamerlogue.ui.views.settings.categories.ImportMode
import it.maicol07.gamerlogue.ui.views.settings.categories.ServiceSyncAction
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer

/**
 * The generated keys are plain (non-sealed) [NavKey]s, so restoring the back stack depends on the
 * polymorphic module nav3ksp generates. `NavTreeLayout.stateConfiguration` is internal to the
 * library, so this rebuilds the same module from the generated registrations. A key whose arguments
 * stop being serializable would otherwise only show up as a crash restoring after process death.
 */
private val Configuration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            with(RootNavTreeLayout) { polymorphicSerializationSubClasses() }
        }
    }
}

class NavBackStackSerializationTest : StringSpec({
    "the back stack round-trips through saved state" {
        val backStack = NavBackStack<NavKey>(
            RootNavTree.Discover,
            RootNavTree.GameList(
                section = DiscoverSection.POPULAR,
                eventId = 42,
                eventName = "Summer Games Fest",
            ),
            RootNavTree.GameDetail(gameId = 7, coverImageId = "co1abc", gameName = "Hollow Knight"),
            RootNavTree.LibraryImportPreview(ExternalService.STEAM, ImportMode.WISHLIST),
            RootNavTree.ServiceSync(ExternalService.STEAM, ServiceSyncAction.CONNECT),
        )
        val serializer = serializer<NavBackStack<NavKey>>()

        val restored = decodeFromSavedState(serializer, encodeToSavedState(serializer, backStack, Configuration), Configuration)

        restored.toList() shouldBe backStack.toList()
    }
})
