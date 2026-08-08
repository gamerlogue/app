package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Android scrolls by touch and shows a transient indicator of its own; a persistent scrollbar
// would be off-platform, so both overloads render nothing.

@Composable
actual fun AppVerticalScrollbar(state: LazyListState, modifier: Modifier) = Unit

@Composable
actual fun AppVerticalScrollbar(state: LazyGridState, modifier: Modifier) = Unit
