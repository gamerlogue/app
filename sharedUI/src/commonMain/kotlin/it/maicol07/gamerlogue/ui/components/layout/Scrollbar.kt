package it.maicol07.gamerlogue.ui.components.layout

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A vertical scrollbar for [state], to be placed on the trailing edge of the scrollable it belongs
 * to (typically `Modifier.align(Alignment.CenterEnd)` inside the same `Box`).
 *
 * Compose only ships scrollbars on the Skiko targets (desktop, web), which is also where a pointer
 * makes them worth having — on Android they are not idiomatic and this renders nothing.
 */
@Composable
expect fun AppVerticalScrollbar(state: LazyListState, modifier: Modifier = Modifier)

@Composable
expect fun AppVerticalScrollbar(state: LazyGridState, modifier: Modifier = Modifier)
