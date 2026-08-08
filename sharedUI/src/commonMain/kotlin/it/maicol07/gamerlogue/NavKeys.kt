package it.maicol07.gamerlogue

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import gamerlogue.sharedui.generated.resources.nav__calendar
import gamerlogue.sharedui.generated.resources.nav__discover
import gamerlogue.sharedui.generated.resources.nav__library
import gamerlogue.sharedui.generated.resources.nav__profile
import gamerlogue.sharedui.generated.resources.nav__settings
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.settings__appearance
import gamerlogue.sharedui.generated.resources.settings__import_library_title
import gamerlogue.sharedui.generated.resources.settings__linked_services
import gamerlogue.sharedui.generated.resources.settings__wishlist_preview_title
import it.maicol07.gamerlogue.services.ExternalService
import it.maicol07.gamerlogue.ui.views.discover.DiscoverSection
import it.maicol07.gamerlogue.ui.views.settings.categories.ImportMode
import it.maicol07.gamerlogue.ui.views.settings.categories.ServiceSyncAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import org.jetbrains.compose.resources.StringResource

typealias NavBackStack = NavBackStack<NavKey>

/** The single app back stack, provided down the composition instead of via DI. */
val LocalNavBackStack = staticCompositionLocalOf<NavBackStack<NavKey>> {
    error("LocalNavBackStack not provided")
}

object NavKeys {
    abstract class NavKeyWithMeta : NavKey {
        open val title: StringResource? = null
        open val showBottomBar: Boolean = true
    }

    @Serializable
    data object Discover : NavKeyWithMeta() {
        override val title = Res.string.nav__discover
    }

    @Serializable
    data object Library : NavKeyWithMeta() {
        override val title = Res.string.nav__library
    }

    @Serializable
    data object Calendar : NavKeyWithMeta() {
        override val title = Res.string.nav__calendar
    }

    @Serializable
    data object Profile : NavKeyWithMeta() {
        override val title = Res.string.nav__profile
    }

    @Serializable
    data object Settings : NavKeyWithMeta() {
        override val title = Res.string.nav__settings
    }

    @Serializable
    data object LinkedServices : NavKeyWithMeta() {
        override val title = Res.string.settings__linked_services
        override val showBottomBar: Boolean = false
    }

    @Serializable
    data object Appearance : NavKeyWithMeta() {
        override val title = Res.string.settings__appearance
        override val showBottomBar: Boolean = false
    }

    @Serializable
    data object Login : NavKeyWithMeta() {
        override val title: StringResource? = null
    }

    @Serializable
    data class GameDetail(val gameId: Int) : NavKeyWithMeta() {
        override val showBottomBar: Boolean = false
    }

    /**
     * The searchable/filterable game grid. A real destination rather than an overlay so covers can
     * share their transition with [GameDetail] and the back stack keeps the list's scroll position.
     */
    @Serializable
    data class GameList(val section: DiscoverSection? = null) : NavKeyWithMeta() {
        // Custom getter (no backing field) so only `section` is serialized.
        override val title: StringResource? get() = section?.sectionTitle
    }

    @Serializable
    data class ServiceSync(
        val service: ExternalService,
        val action: ServiceSyncAction,
    ) : NavKeyWithMeta() {
        // Draws its own chrome (a BottomSheetScaffold), so no top-bar title and no bottom bar.
        override val title: StringResource? get() = null
        override val showBottomBar: Boolean get() = false
    }

    @Serializable
    data class LibraryImportPreview(
        val service: ExternalService,
        val mode: ImportMode = ImportMode.OWNED,
    ) : NavKeyWithMeta() {
        // Custom getters (no backing field) so only the data fields are serialized.
        override val title: StringResource? get() = when (mode) {
            ImportMode.OWNED -> Res.string.settings__import_library_title
            ImportMode.WISHLIST -> Res.string.settings__wishlist_preview_title
        }
        override val showBottomBar: Boolean get() = false
    }

    // Reified helper so each key is registered once, avoiding the subclass(A::class, B.serializer()) mismatch footgun.
    private inline fun <reified T : NavKey> PolymorphicModuleBuilder<NavKey>.key() =
        subclass(T::class, serializer<T>())

    val savedStateConfiguration = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                key<Discover>()
                key<Library>()
                key<Calendar>()
                key<Profile>()
                key<Settings>()
                key<LinkedServices>()
                key<Appearance>()
                key<Login>()
                key<GameDetail>()
                key<GameList>()
                key<LibraryImportPreview>()
                key<ServiceSync>()
            }
        }
    }
}
