package it.maicol07.gamerlogue.ui.components.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SearchW500Rounded
import it.maicol07.gamerlogue.LocalNavBackStack
import it.maicol07.gamerlogue.NavBackStack
import it.maicol07.gamerlogue.ui.components.layout.NetworkErrorAction
import it.maicol07.gamerlogue.ui.navigation.LocalSharedTransitionScope
import kotlinx.coroutines.launch

private val BarHorizontalPadding = 8.dp
private val BarVerticalPadding = 4.dp

/**
 * Full-width Material 3 search bar used as a screen's top app bar.
 *
 * Every affordance lives inside the input field rather than in surrounding app-bar slots, because
 * [ExpandedFullScreenSearchBar] renders *only* the input field — anything placed beside it would
 * vanish exactly when the results are on screen.
 *
 * Expansion is hoisted: the caller owns [expanded] so other parts of the screen (e.g. a
 * "see all" button) can open the results pane.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameSearchBar(
    placeholder: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    query: String = "",
    backStack: NavBackStack = LocalNavBackStack.current,
    trailingActions: @Composable () -> Unit = {},
    results: @Composable () -> Unit,
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState(query)
    val scope = rememberCoroutineScope()

    // Keeps the field in sync when the owner drops the query behind our back (e.g. "reset filters").
    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) textFieldState.edit { replace(0, length, query) }
    }
    LaunchedEffect(expanded) {
        if (expanded) searchBarState.animateToExpanded() else searchBarState.animateToCollapsed()
    }
    LaunchedEffect(searchBarState) {
        snapshotFlow { searchBarState.targetValue }
            .collect { onExpandedChange(it == SearchBarValue.Expanded) }
    }

    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = onSearch,
            placeholder = { Text(placeholder) },
            leadingIcon = {
                when {
                    searchBarState.targetValue == SearchBarValue.Expanded ->
                        IconButton(onClick = { scope.launch { searchBarState.animateToCollapsed() } }) {
                            Icon(Icons.ArrowBackW500Rounded, contentDescription = null)
                        }

                    backStack.size > 1 ->
                        IconButton(onClick = { backStack.removeAt(backStack.lastIndex) }) {
                            Icon(Icons.ArrowBackW500Rounded, contentDescription = null)
                        }

                    else -> Icon(Icons.SearchW500Rounded, contentDescription = null)
                }
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (textFieldState.text.isNotEmpty()) {
                        IconButton(onClick = {
                            textFieldState.clearText()
                            onSearch("")
                        }) {
                            Icon(Icons.CloseW500Rounded, contentDescription = null)
                        }
                    }
                    trailingActions()
                    NetworkErrorAction()
                }
            }
        )
    }

    // Not AppBarWithSearch: it caps the bar at SearchBarMaxWidth (720.dp) and centers it, so on a
    // wide window it would not span the top bar. Every affordance lives in the input field anyway,
    // which is all the expanded bar renders — its navigationIcon/actions slots would disappear.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(SearchBarDefaults.windowInsets)
            .padding(horizontal = BarHorizontalPadding, vertical = BarVerticalPadding)
    ) {
        SearchBar(state = searchBarState, inputField = inputField, modifier = Modifier.fillMaxWidth())
    }
    ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
        // The expanded bar is its own dialog, outside the SharedTransitionLayout's hierarchy;
        // leaving the scope in place makes shared covers crash with "layouts are not part of the
        // same hierarchy". Null disables the shared-element modifier for this subtree.
        CompositionLocalProvider(LocalSharedTransitionScope provides null) { results() }
    }
}
