package it.maicol07.gamerlogue.ui.views.social

import androidx.lifecycle.viewModelScope
import at.released.igdbclient.model.Game
import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.core.BaseViewModel
import it.maicol07.gamerlogue.core.PageLoader
import it.maicol07.gamerlogue.data.Activity
import it.maicol07.gamerlogue.data.ApiErrorCodes
import it.maicol07.gamerlogue.data.IgdbLookup
import it.maicol07.gamerlogue.extensions.fetchPage
import it.maicol07.gamerlogue.extensions.gameId
import it.maicol07.spraypaintkt.SortDirection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.inject

/**
 * A page-by-page activity list, with the games its payloads mention: [userId]'s activities on their
 * profile, or with null the feed of the users the signed-in one follows.
 */
@KoinViewModel
class ActivityViewModel(@InjectedParam private val userId: String?) : BaseViewModel() {
    private val lookup by inject<IgdbLookup>()

    val games: StateFlow<Map<Int, Game>>
        field = MutableStateFlow(emptyMap())

    val activities = PageLoader(viewModelScope) { page ->
        safeRequest(ApiErrorCodes.HIDDEN_CONTENT) {
            Activity.scope {
                includes("user", "subject")
                if (userId != null) where("user", userId) else where("feed", "true")
                order("created_at", SortDirection.DESC)
            }.fetchPage(page).also { result ->
                val found = lookup.games(result.items.mapNotNull { it.gameId })
                games.update { it + found }
            }
        }.get()
    }
}
