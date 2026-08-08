package it.maicol07.gamerlogue.ui.components.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.ArrowBackW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CloseW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SearchW500Rounded
import it.maicol07.gamerlogue.ui.components.layout.NetworkErrorAction

private val BarHorizontalPadding = 8.dp
private val BarVerticalPadding = 4.dp

/**
 * Common chrome around a top-bar search field: window insets plus the bar's own padding, with the
 * global network-error action pinned to its right so it stays reachable in both variants.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarShell(modifier: Modifier, bar: @Composable (Modifier) -> Unit) = Row(
    modifier = modifier
        .fillMaxWidth()
        .windowInsetsPadding(SearchBarDefaults.windowInsets)
        .padding(horizontal = BarHorizontalPadding, vertical = BarVerticalPadding),
    verticalAlignment = Alignment.CenterVertically
) {
    bar(Modifier.weight(1f))
    NetworkErrorAction()
}

/**
 * A search bar that behaves as a button: tapping anywhere on it opens the game list destination
 * instead of focusing a field, so the results are a real navigation entry (shared cover transition,
 * predictive back, retained scroll position) rather than an overlay dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSearchButton(
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()

    SearchBarShell(modifier) { barModifier ->
        Box(barModifier) {
            SearchBar(
                state = searchBarState,
                modifier = Modifier.fillMaxWidth(),
                inputField = {
                    SearchBarDefaults.InputField(
                        searchBarState = searchBarState,
                        textFieldState = textFieldState,
                        onSearch = {},
                        enabled = false,
                        placeholder = { Text(placeholder) },
                        leadingIcon = { Icon(Icons.SearchW500Rounded, contentDescription = null) }
                    )
                }
            )
            // The disabled field swallows nothing, so an overlay turns the whole bar into the button.
            Box(Modifier.matchParentSize().clickable(onClick = onClick))
        }
    }
}

/**
 * The editable search bar owned by the game list destination.
 *
 * [autoFocus] only fires the first time the destination is composed: coming back from a game must
 * not pop the keyboard open again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListSearchBar(
    placeholder: String,
    query: String,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    trailingActions: @Composable () -> Unit = {},
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState(query)
    val focusRequester = remember { FocusRequester() }
    var focusedOnce by rememberSaveable { mutableStateOf(false) }

    // Keeps the field in sync when the owner drops the query behind our back (e.g. "reset filters").
    LaunchedEffect(query) {
        if (textFieldState.text.toString() != query) textFieldState.edit { replace(0, length, query) }
    }
    LaunchedEffect(Unit) {
        if (autoFocus && !focusedOnce) {
            focusedOnce = true
            focusRequester.requestFocus()
        }
    }

    SearchBarShell(modifier) { barModifier ->
        SearchBar(
            state = searchBarState,
            modifier = barModifier,
            inputField = {
                SearchBarDefaults.InputField(
                    searchBarState = searchBarState,
                    textFieldState = textFieldState,
                    onSearch = onSearch,
                    modifier = Modifier.focusRequester(focusRequester),
                    placeholder = { Text(placeholder) },
                    leadingIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.ArrowBackW500Rounded, contentDescription = null)
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
                        }
                    }
                )
            }
        )
    }
}
