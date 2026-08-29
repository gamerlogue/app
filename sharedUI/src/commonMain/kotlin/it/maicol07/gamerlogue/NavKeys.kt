package it.maicol07.gamerlogue

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import gamerlogue.sharedui.generated.resources.nav__calendar
import gamerlogue.sharedui.generated.resources.nav__discover
import gamerlogue.sharedui.generated.resources.nav__events
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
import org.jetbrains.compose.resources.StringResource

typealias NavBackStack = NavBackStack<AppNavKey>

/** The single app back stack, provided down the composition instead of via DI. */
val LocalNavBackStack = staticCompositionLocalOf<NavBackStack<AppNavKey>> {
    error("LocalNavBackStack not provided")
}

/**
 * Base of every destination. Being `sealed` gives kotlinx.serialization closed polymorphism, so the
 * back stack serializes without a hand-maintained `polymorphic { subclass(...) }` registry.
 *
 * Overrides must use custom getters (no backing field), otherwise serialization would pull [title]
 * in and demand a serializer for [StringResource].
 */
@Serializable
sealed interface AppNavKey : NavKey {
    val title: StringResource? get() = null
    val showBottomBar: Boolean get() = true
}

object NavKeys {
    @Serializable
    data object Discover : AppNavKey {
        override val title get() = Res.string.nav__discover
    }

    @Serializable
    data object Library : AppNavKey {
        override val title get() = Res.string.nav__library
    }

    @Serializable
    data object Calendar : AppNavKey {
        override val title get() = Res.string.nav__calendar
    }

    @Serializable
    data object Profile : AppNavKey {
        override val title get() = Res.string.nav__profile
    }

    @Serializable
    data object Settings : AppNavKey {
        override val title get() = Res.string.nav__settings
    }

    @Serializable
    data object LinkedServices : AppNavKey {
        override val title get() = Res.string.settings__linked_services
        override val showBottomBar get() = false
    }

    @Serializable
    data object Appearance : AppNavKey {
        override val title get() = Res.string.settings__appearance
        override val showBottomBar get() = false
    }

    @Serializable
    data object Login : AppNavKey

    /** [coverImageId] and [gameName] keep the cover transition alive while the detail request loads. */
    @Serializable
    data class GameDetail(
        val gameId: Int,
        val coverImageId: String? = null,
        val gameName: String? = null,
    ) : AppNavKey {
        override val showBottomBar get() = false
    }

    /**
     * The searchable/filterable game grid. A real destination rather than an overlay so covers can
     * share their transition with [GameDetail] and the back stack keeps the list's scroll position.
     */
    @Serializable
    data object EventList : AppNavKey {
        override val title get() = Res.string.nav__events
    }

    /**
     * The searchable/filterable game grid. [eventId] scopes it to the games of an IGDB event, with
     * [eventName] shown as the search bar placeholder so the event is identifiable.
     */
    @Serializable
    data class GameList(
        val section: DiscoverSection? = null,
        val eventId: Int? = null,
        val eventName: String? = null,
    ) : AppNavKey {
        override val title: StringResource? get() = section?.sectionTitle
    }

    @Serializable
    data class ServiceSync(
        val service: ExternalService,
        val action: ServiceSyncAction,
    ) : AppNavKey {
        // Draws its own chrome (a BottomSheetScaffold), so no top-bar title and no bottom bar.
        override val showBottomBar get() = false
    }

    @Serializable
    data class LibraryImportPreview(
        val service: ExternalService,
        val mode: ImportMode = ImportMode.OWNED,
    ) : AppNavKey {
        override val title: StringResource? get() = when (mode) {
            ImportMode.OWNED -> Res.string.settings__import_library_title
            ImportMode.WISHLIST -> Res.string.settings__wishlist_preview_title
        }
        override val showBottomBar get() = false
    }
}
