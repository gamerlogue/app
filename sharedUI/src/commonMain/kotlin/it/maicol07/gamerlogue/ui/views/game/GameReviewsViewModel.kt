package it.maicol07.gamerlogue.ui.views.game

import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.get
import it.maicol07.gamerlogue.core.BaseViewModel
import it.maicol07.gamerlogue.core.PageLoader
import it.maicol07.gamerlogue.data.LibraryEntry
import it.maicol07.gamerlogue.extensions.ResourcePage
import it.maicol07.gamerlogue.extensions.averageRating
import it.maicol07.gamerlogue.extensions.gameReviews
import it.maicol07.gamerlogue.extensions.pageNumber
import it.maicol07.gamerlogue.extensions.totalItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

/** Every user's reviews of [gameId] (the base game), the most liked first. */
@KoinViewModel
class GameReviewsViewModel(@InjectedParam private val gameId: Int) : BaseViewModel() {
    /** Read off the first page: the server computes it over every page. */
    val stats: StateFlow<ReviewStats?>
        field = MutableStateFlow(null)

    val reviews = PageLoader(viewModelScope) { page ->
        safeRequest { LibraryEntry.gameReviews(gameId).pageNumber(page).all() }.get()?.let { result ->
            if (page == 1) stats.value = ReviewStats(result.totalItems ?: result.data.size, result.averageRating)
            ResourcePage(result.data, result.totalItems)
        }
    }
}
