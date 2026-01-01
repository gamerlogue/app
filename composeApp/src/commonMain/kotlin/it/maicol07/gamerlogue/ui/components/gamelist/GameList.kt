package it.maicol07.gamerlogue.ui.components.gamelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import at.released.igdbclient.model.Game
import it.maicol07.gamerlogue.ui.components.game.CoverImage

@Composable
fun GameList(games: List<Game>) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
//        modifier = Modifier.fillMaxSize()
    ) {
        items(games) {
            it.CoverImage()
        }
    }
}
