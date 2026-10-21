// The file is named after the sheet, which is its entry point; the view model lives beside it.
@file:Suppress("MatchingDeclarationName")

package it.maicol07.gamerlogue.ui.views.userlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.michaelbull.result.get
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.lists__add_to_list
import gamerlogue.sharedui.generated.resources.lists__new
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.AddW500Rounded
import it.maicol07.gamerlogue.core.StateViewModel
import it.maicol07.gamerlogue.data.UserList
import it.maicol07.gamerlogue.data.UserListItem
import it.maicol07.gamerlogue.extensions.allPages
import it.maicol07.gamerlogue.extensions.expressiveSegmentedColors
import it.maicol07.gamerlogue.extensions.expressiveSegmentedShapes
import it.maicol07.gamerlogue.ui.theme.Dimens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import org.koin.core.parameter.parametersOf

/** The signed-in user's lists, each marked when it already holds [gameId]. */
@KoinViewModel
class AddToListViewModel(
    @InjectedParam private val gameId: Int,
) : StateViewModel<AddToListViewModel.UiState>(UiState()) {
    data class UiState(
        val lists: List<UserList> = emptyList(),
        /** Ids of the lists holding the game. */
        val containing: Set<String> = emptySet(),
        /** Ids of the lists being changed. */
        val pending: Set<String> = emptySet(),
        val loading: Boolean = true,
    )

    fun load() = viewModelScope.launch {
        val lists = safeRequest { UserList.scope().allPages() }.get().orEmpty()
        val containing = safeRequest { UserList.where("contains_game", gameId).allPages() }.get().orEmpty()
        update { copy(lists = lists, containing = containing.mapNotNull { it.id }.toSet(), loading = false) }
    }

    fun toggle(list: UserList) = viewModelScope.launch {
        val listId = list.id ?: return@launch
        update { copy(pending = pending + listId) }
        val contained = listId in state.containing
        val done = if (contained) removeFrom(listId) else addTo(list)
        update {
            copy(
                pending = pending - listId,
                containing = if (done) (if (contained) containing - listId else containing + listId) else containing,
            )
        }
    }

    fun createAndAdd(form: UserListForm) = viewModelScope.launch {
        val list = UserList().apply { form.applyTo(this) }
        if (safeRequest { list.save() }.isErr) return@launch
        val added = addTo(list)
        val listId = list.id ?: return@launch
        update { copy(lists = lists + list, containing = if (added) containing + listId else containing) }
    }

    /** Appends the game at the end of [list]: without a position the server puts it last. */
    private suspend fun addTo(list: UserList): Boolean = safeRequest {
        UserListItem().apply {
            listId = checkNotNull(list.id).toInt()
            gameId = this@AddToListViewModel.gameId
        }.save()
    }.isOk

    /** The list items endpoint filters by list only, so the game's item is found among them. */
    private suspend fun removeFrom(listId: String): Boolean = safeRequest {
        UserListItem.where("list", listId).allPages().firstOrNull { it.gameId == gameId }?.destroy()
    }.isOk
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AddToListSheet(gameId: Int, onDismiss: () -> Unit) {
    val viewModel = koinViewModel<AddToListViewModel>(key = "add-to-list-$gameId") { parametersOf(gameId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var formOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ItemGap),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            Text(
                stringResource(Res.string.lists__add_to_list),
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(bottom = Dimens.SectionGap)
            )
            if (uiState.loading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { LoadingIndicator() }
            }
            uiState.lists.forEachIndexed { index, list ->
                val checked = list.id in uiState.containing
                SegmentedListItem(
                    checked = checked,
                    onCheckedChange = { viewModel.toggle(list) },
                    enabled = list.id !in uiState.pending,
                    shapes = ListItemDefaults.expressiveSegmentedShapes(index, uiState.lists.size),
                    colors = ListItemDefaults.expressiveSegmentedColors(),
                    leadingContent = { Checkbox(checked = checked, onCheckedChange = null) },
                ) { Text(list.name) }
            }
            FilledTonalButton(
                onClick = { formOpen = true },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.SectionGap)
            ) {
                Icon(Icons.AddW500Rounded, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Text(
                    stringResource(Res.string.lists__new),
                    modifier = Modifier.padding(start = ButtonDefaults.IconSpacing),
                )
            }
        }
    }
    if (formOpen) {
        UserListFormDialog(
            initial = null,
            onSave = viewModel::createAndAdd,
            onDelete = null,
            onDismiss = { formOpen = false }
        )
    }
}
