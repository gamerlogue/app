package it.maicol07.gamerlogue.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.nav__detail_placeholder
import io.github.fopwoc.nav3ksp.annotation.BranchEntryMetadata
import org.jetbrains.compose.resources.stringResource

/**
 * Shared scene metadata for feature destinations. nav3ksp resolves `@Branch(metadata = …)` by
 * type, so each of these has to be a top-level object; they are shared across branches rather than
 * written one per destination.
 */

/** Left pane of the adaptive list-detail scene. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
object ListPaneMetadata : BranchEntryMetadata {
    override fun metadata(): Map<String, Any> = ListDetailSceneStrategy.listPane()
}

/** Right pane of the adaptive list-detail scene. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
object DetailPaneMetadata : BranchEntryMetadata {
    override fun metadata(): Map<String, Any> = ListDetailSceneStrategy.detailPane()
}

/** Discover is the entry point, so it fills the empty detail pane with a prompt. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
object DiscoverPaneMetadata : BranchEntryMetadata {
    override fun metadata(): Map<String, Any> = ListDetailSceneStrategy.listPane(
        detailPlaceholder = { Text(stringResource(Res.string.nav__detail_placeholder)) }
    )
}
