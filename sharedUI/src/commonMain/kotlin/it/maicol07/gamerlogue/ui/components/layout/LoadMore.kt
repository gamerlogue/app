package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import it.maicol07.gamerlogue.ui.theme.Dimens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

private const val LoadMoreThreshold = 5

/**
 * Calls [onLoadMore] whenever the list is scrolled within a few items of its end, and again each
 * time the item count changes while it stays there: a first page shorter than the screen would
 * otherwise never ask for the second. The loader decides whether there is anything left to load.
 */
@Composable
fun LoadMoreEffect(listState: LazyListState, onLoadMore: () -> Unit) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: 0
            (lastVisible >= layout.totalItemsCount - LoadMoreThreshold) to layout.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (nearEnd, _) -> nearEnd }
            .collect { currentOnLoadMore() }
    }
}

/** A centered loading indicator as the list's last item, while the next page loads. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.loadingItem(visible: Boolean) {
    if (!visible) return
    item(key = "loading") {
        Box(Modifier.fillMaxWidth().padding(Dimens.ScreenPadding), contentAlignment = Alignment.Center) {
            LoadingIndicator()
        }
    }
}
