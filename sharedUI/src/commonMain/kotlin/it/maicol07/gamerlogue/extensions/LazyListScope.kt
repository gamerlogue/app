package it.maicol07.gamerlogue.extensions

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier

/**
 * Room for the system navigation bar after the last item, so the list scrolls behind the bar yet its end stays
 * reachable. Zero while the app's bottom bar shows, since that bar already keeps clear of it.
 */
fun LazyListScope.navigationBarsSpacer() = item(key = "navigation-bars") { Spacer(Modifier.navigationBarsPadding()) }
