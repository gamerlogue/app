package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AppVerticalScrollbar(state: LazyListState, modifier: Modifier) =
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier)

@Composable
actual fun AppVerticalScrollbar(state: LazyGridState, modifier: Modifier) =
    VerticalScrollbar(rememberScrollbarAdapter(state), modifier)
