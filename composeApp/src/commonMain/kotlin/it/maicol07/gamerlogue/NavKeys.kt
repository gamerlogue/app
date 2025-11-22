package it.maicol07.gamerlogue

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import gamerlogue.composeapp.generated.resources.Res
import gamerlogue.composeapp.generated.resources.nav__calendar
import gamerlogue.composeapp.generated.resources.nav__discover
import gamerlogue.composeapp.generated.resources.nav__library
import gamerlogue.composeapp.generated.resources.nav__profile
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.StringResource

typealias NavBackStack = androidx.navigation3.runtime.NavBackStack<NavKey>

object NavKeys {
    interface NavKeyWithMeta : NavKey {
        val title: StringResource?
    }

    @Serializable
    data object Discover : NavKeyWithMeta {
        override val title = Res.string.nav__discover
    }

    @Serializable
    data object Library : NavKeyWithMeta {
        override val title = Res.string.nav__library
    }

    @Serializable
    data object Calendar : NavKeyWithMeta {
        override val title = Res.string.nav__calendar
    }

    @Serializable
    data object Profile : NavKeyWithMeta {
        override val title = Res.string.nav__profile
    }

    @Serializable
    data class GameDetail(val gameId: Int) : NavKey

    @Serializable
    data class GameList(val title: String) : NavKey

    val savedStateConfiguration = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                subclass(Discover::class, Discover.serializer())
                subclass(Library::class, Library.serializer())
                subclass(Calendar::class, Calendar.serializer())
                subclass(Profile::class, Profile.serializer())
                subclass(GameDetail::class, GameDetail.serializer())
                subclass(GameList::class, GameList.serializer())
            }
        }
    }
}
