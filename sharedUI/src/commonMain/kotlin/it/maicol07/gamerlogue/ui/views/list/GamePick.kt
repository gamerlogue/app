package it.maicol07.gamerlogue.ui.views.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.core.annotation.Single

/**
 * The game list opened to pick games for another screen: one game, picked by tapping it, or several,
 * confirmed together. The picked IGDB ids go back through [GamePicks] under [request].
 */
@Serializable
data class GamePick(val request: String, val multiple: Boolean)

/**
 * The games picked in a game list, waiting for the screen that asked for them. Navigation 3 has no
 * result passing, and the asking screen may not even be composed while the list is on top.
 */
@Single
class GamePicks {
    private val pending = MutableStateFlow<Map<String, List<Int>>>(emptyMap())

    fun deliver(request: String, gameIds: List<Int>) = pending.update { it + (request to gameIds) }

    /** Hands [request]'s games to [onPicked] once each time they are delivered. */
    suspend fun collect(request: String, onPicked: (List<Int>) -> Unit) = pending
        .mapNotNull { it[request] }
        .collect { ids ->
            pending.update { it - request }
            onPicked(ids)
        }
}

/** Runs [onPicked] with the games picked for [request], also when they were picked while away from here. */
@Composable
fun GamePickEffect(request: String, onPicked: (List<Int>) -> Unit) {
    val picks = koinInject<GamePicks>()
    val currentOnPicked by rememberUpdatedState(onPicked)
    LaunchedEffect(request) { picks.collect(request) { currentOnPicked(it) } }
}
