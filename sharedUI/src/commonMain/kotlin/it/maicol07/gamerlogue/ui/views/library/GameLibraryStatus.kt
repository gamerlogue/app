package it.maicol07.gamerlogue.ui.views.library

import androidx.compose.ui.graphics.vector.ImageVector
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.library__section_abandoned
import gamerlogue.sharedui.generated.resources.library__section_backlog
import gamerlogue.sharedui.generated.resources.library__section_completed
import gamerlogue.sharedui.generated.resources.library__section_paused
import gamerlogue.sharedui.generated.resources.library__section_playing
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.BookmarkW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.CheckCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.DeleteW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PauseCircleW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.PlayCircleW500Rounded
import org.jetbrains.compose.resources.StringResource

enum class GameLibraryStatus(val displayName: StringResource, val icon: ImageVector) {
    PLAYING(
        Res.string.library__section_playing,
        Icons.PlayCircleW500Rounded
    ),
    COMPLETED(
        Res.string.library__section_completed,
        Icons.CheckCircleW500Rounded
    ),
    PAUSED(
        Res.string.library__section_paused,
        Icons.PauseCircleW500Rounded
    ),
    ABANDONED(
        Res.string.library__section_abandoned,
        Icons.DeleteW500Rounded
    ),
    BACKLOG(
        Res.string.library__section_backlog,
        Icons.BookmarkW500Rounded
    ),
}
