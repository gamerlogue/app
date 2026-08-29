package it.maicol07.gamerlogue.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.maicol07.gamerlogue.AppNavKey
import it.maicol07.gamerlogue.NavKeys
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import it.maicol07.gamerlogue.ui.views.settings.categories.ImportMode
import it.maicol07.gamerlogue.ui.views.settings.categories.ServiceSyncAction
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/** One instance per [AppNavKey] subclass; the second test is what keeps it exhaustive. */
private val Samples = listOf<AppNavKey>(
    NavKeys.Discover,
    NavKeys.Library,
    NavKeys.Calendar,
    NavKeys.Profile,
    NavKeys.Settings,
    NavKeys.LinkedServices,
    NavKeys.Appearance,
    NavKeys.EventList,
    NavKeys.GameDetail(gameId = 1),
    NavKeys.GameList(),
    NavKeys.ServiceSync(ExternalService.STEAM, ServiceSyncAction.CONNECT),
    NavKeys.LibraryImportPreview(ExternalService.STEAM),
)

/**
 * Every leaf of the [AppNavKey] hierarchy. [KClass.sealedSubclasses] is direct-only, so an intermediate
 * sealed layer would silently shrink what the exhaustiveness check below covers.
 */
private fun KClass<*>.allLeafSubclasses(): Set<KClass<*>> =
    sealedSubclasses.flatMap { if (it.isSealed) it.allLeafSubclasses() else listOf(it) }.toSet()

/**
 * Adding a destination means adding a key and registering its entry; forgetting the second half
 * used to surface only as a crash when navigating there. These fail the build instead.
 */
class NavEntriesCoverageTest : StringSpec({
    "every destination resolves to a registered entry" {
        val backStack = NavBackStack<AppNavKey>()
        val provider = entryProvider<AppNavKey> {
            browseEntries(backStack, {}, {})
            accountEntries(backStack, {})
            settingsEntries(backStack)
            gameEntries {}
        }

        // The default fallback throws on an unregistered key.
        Samples.forEach { key -> shouldNotThrowAny { provider(key) } }
    }

    // No exemption list: every destination has an entry, so a new key that forgets one fails here.
    "every AppNavKey subclass is sampled" {
        Samples.map { it::class }.toSet() shouldBe AppNavKey::class.allLeafSubclasses()
    }

    // Closed polymorphism replaced a hand-written SerializersModule; a regression here would only
    // show up as a crash restoring after process death. Keys with arguments are the risky ones.
    "the back stack round-trips through saved state" {
        val backStack = NavBackStack<AppNavKey>(
            NavKeys.Discover,
            NavKeys.GameList(section = DiscoverSection.POPULAR, eventId = 42, eventName = "Summer Games Fest"),
            NavKeys.GameDetail(gameId = 7, coverImageId = "co1abc", gameName = "Hollow Knight"),
            NavKeys.LibraryImportPreview(ExternalService.STEAM, ImportMode.WISHLIST),
        )
        val serializer = serializer<NavBackStack<AppNavKey>>()

        val restored = decodeFromSavedState(serializer, encodeToSavedState(serializer, backStack))

        restored.toList() shouldBe backStack.toList()
    }
})
