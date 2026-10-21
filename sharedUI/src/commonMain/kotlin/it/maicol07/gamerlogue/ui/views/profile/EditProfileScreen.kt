package it.maicol07.gamerlogue.ui.views.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.released.igdbclient.model.Game
import gamerlogue.sharedui.generated.resources.Res
import gamerlogue.sharedui.generated.resources.profile__banner
import gamerlogue.sharedui.generated.resources.profile__banner_choose
import gamerlogue.sharedui.generated.resources.profile__banner_hint
import gamerlogue.sharedui.generated.resources.profile__banner_remove
import gamerlogue.sharedui.generated.resources.profile__bio
import gamerlogue.sharedui.generated.resources.profile__edit
import gamerlogue.sharedui.generated.resources.profile__edit_invalid
import gamerlogue.sharedui.generated.resources.profile__first_name
import gamerlogue.sharedui.generated.resources.profile__last_name
import gamerlogue.sharedui.generated.resources.profile__nickname
import gamerlogue.sharedui.generated.resources.profile__nickname_rules
import gamerlogue.sharedui.generated.resources.profile__picture_edit
import gamerlogue.sharedui.generated.resources.profile__picture_hint
import gamerlogue.sharedui.generated.resources.profile__picture_provider_only
import gamerlogue.sharedui.generated.resources.profile__platforms
import gamerlogue.sharedui.generated.resources.profile__platforms_edit
import gamerlogue.sharedui.generated.resources.profile__platforms_hint
import gamerlogue.sharedui.generated.resources.profile__platforms_search
import gamerlogue.sharedui.generated.resources.profile__private_hint_edit
import gamerlogue.sharedui.generated.resources.profile__private_switch
import gamerlogue.sharedui.generated.resources.profile__save
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.Icons
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.OpenInNewW500Rounded
import io.github.kingsword09.symbolcraft.symbols.icons.materialsymbols.icons.SaveW500Rounded
import it.maicol07.gamerlogue.data.UserSchema
import it.maicol07.gamerlogue.extensions.openURL
import it.maicol07.gamerlogue.ui.components.game.GameBannerImage
import it.maicol07.gamerlogue.ui.components.game.Image
import it.maicol07.gamerlogue.ui.components.user.UserAvatar
import it.maicol07.gamerlogue.ui.theme.Dimens
import it.maicol07.gamerlogue.ui.views.userlist.SwitchRow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val BannerPreviewHeight = 120.dp
private const val BANNER_ASPECT = 16f / 9f

/**
 * The signed-in user's profile editor; [onSaved] runs after a successful save, before the sheet
 * closes. [onPickBanner] closes the sheet for the game list, keeping the edits for when it reopens.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EditProfileSheet(
    viewModel: EditProfileViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onPickBanner: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState =
        rememberBottomSheetState(SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    val close = {
        viewModel.discard()
        onDismiss()
    }
    ModalBottomSheet(onDismissRequest = close, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            Text(stringResource(Res.string.profile__edit), style = MaterialTheme.typography.headlineSmallEmphasized)
            if (uiState.loading) {
                Box(Modifier.fillMaxWidth().padding(Dimens.ScreenPadding), contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator()
                }
            } else {
                EditProfileForm(uiState, viewModel, onPickBanner)
                Button(
                    onClick = {
                        viewModel.save(
                            onSaved = {
                                onSaved()
                                scope.launch { sheetState.hide() }.invokeOnCompletion { close() }
                            }
                        )
                    },
                    enabled = !uiState.saving && uiState.user != null,
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                    modifier = Modifier.fillMaxWidth().height(ButtonDefaults.MediumContainerHeight),
                ) {
                    val iconSize = ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)
                    AnimatedContent(uiState.saving, transitionSpec = { fadeIn() togetherWith fadeOut() }) { saving ->
                        if (saving) {
                            LoadingIndicator(Modifier.size(iconSize), color = LocalContentColor.current)
                        } else {
                            Icon(Icons.SaveW500Rounded, contentDescription = null, modifier = Modifier.size(iconSize))
                        }
                    }
                    Text(
                        stringResource(Res.string.profile__save),
                        style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight),
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditProfileForm(
    state: EditProfileViewModel.UiState,
    viewModel: EditProfileViewModel,
    onPickBanner: () -> Unit,
) {
    var platformsOpen by remember { mutableStateOf(false) }
    PictureSection(state)
    if (state.invalid) {
        Text(
            stringResource(Res.string.profile__edit_invalid),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    EditProfileFields(state, viewModel)
    EditSection(Res.string.profile__platforms, Res.string.profile__platforms_hint) {
        PlatformChips(state.allPlatforms.filter { it.id.toInt() in state.platformIds })
        LaunchedEffect(state.platformIds) { if (state.platformIds.isNotEmpty()) viewModel.loadPlatforms() }
        FilledTonalButton(
            onClick = {
                viewModel.loadPlatforms()
                platformsOpen = true
            },
            shapes = ButtonDefaults.shapes()
        ) { Text(stringResource(Res.string.profile__platforms_edit)) }
    }
    EditSection(Res.string.profile__banner, Res.string.profile__banner_hint) {
        BannerEditor(state, onChoose = onPickBanner, onRemove = { viewModel.setBanner(null) })
    }
    if (platformsOpen) PlatformsSheet(state, viewModel::togglePlatform, onDismiss = { platformsOpen = false })
    state.bannerGame?.let { game ->
        BannerImagesSheet(
            game,
            state.bannerOptions,
            onSelect = {
                viewModel.setBanner(it)
                viewModel.closeBannerImages()
            },
            onDismiss = viewModel::closeBannerImages
        )
    }
}

/** The plain text inputs of the form, split out to keep [EditProfileForm] readable. */
@Composable
private fun EditProfileFields(state: EditProfileViewModel.UiState, viewModel: EditProfileViewModel) {
    TextField(
        state = viewModel.nickname,
        label = { Text(stringResource(Res.string.profile__nickname)) },
        supportingText = { Text(stringResource(Res.string.profile__nickname_rules)) },
        lineLimits = TextFieldLineLimits.SingleLine,
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
        TextField(
            state = viewModel.firstName,
            label = { Text(stringResource(Res.string.profile__first_name)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.weight(1f)
        )
        TextField(
            state = viewModel.lastName,
            label = { Text(stringResource(Res.string.profile__last_name)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier.weight(1f)
        )
    }
    TextField(
        state = viewModel.bio,
        label = { Text(stringResource(Res.string.profile__bio)) },
        supportingText = { Text("${viewModel.bio.text.length}/${UserSchema.MAX_BIO_LENGTH}") },
        isError = viewModel.bio.text.length > UserSchema.MAX_BIO_LENGTH,
        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 6),
        modifier = Modifier.fillMaxWidth()
    )
    Column {
        SwitchRow(stringResource(Res.string.profile__private_switch), state.private, viewModel::setPrivate)
        Text(
            stringResource(Res.string.profile__private_hint_edit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The picture comes from the identity provider: it is changed there, not here. */
@Composable
private fun PictureSection(state: EditProfileViewModel.UiState) {
    val uriHandler = LocalUriHandler.current
    val editUrl = state.user?.pictureEditUrl
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SectionGap)
    ) {
        UserAvatar(state.user, 72.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val hint = if (editUrl != null) Res.string.profile__picture_hint else Res.string.profile__picture_provider_only
            Text(
                stringResource(hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (editUrl != null) {
                OutlinedButton(onClick = { uriHandler.openURL(editUrl) }, shapes = ButtonDefaults.shapes()) {
                    Icon(
                        Icons.OpenInNewW500Rounded,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Text(
                        stringResource(Res.string.profile__picture_edit),
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                    )
                }
            }
        }
    }
}

/** A titled block of the editor, with a line telling what it is for. */
@Composable
private fun EditSection(title: StringResource, hint: StringResource, content: @Composable () -> Unit) =
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMediumEmphasized)
        Text(
            stringResource(hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }

@Composable
private fun BannerEditor(state: EditProfileViewModel.UiState, onChoose: () -> Unit, onRemove: () -> Unit) {
    val banner = state.banner
    if (banner != null) {
        val previewModifier = Modifier.fillMaxWidth().height(BannerPreviewHeight).clip(MaterialTheme.shapes.large)
        GameBannerImage(
            banner.imageId,
            modifier = previewModifier,
            loadingModifier = previewModifier,
            contentScale = ContentScale.Crop
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
        FilledTonalButton(onClick = onChoose, shapes = ButtonDefaults.shapes()) {
            Text(stringResource(Res.string.profile__banner_choose))
        }
        if (banner != null) {
            TextButton(onClick = onRemove, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(Res.string.profile__banner_remove))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlatformsSheet(state: EditProfileViewModel.UiState, onToggle: (Int) -> Unit, onDismiss: () -> Unit) {
    val query = rememberTextFieldState()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Dimens.ScreenPadding).navigationBarsPadding()) {
            TextField(
                state = query,
                placeholder = { Text(stringResource(Res.string.profile__platforms_search)) },
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.fillMaxWidth()
            )
            val text = query.text.toString()
            // Owned platforms first, so they stay reachable in a list of a few hundred.
            val platforms = state.allPlatforms
                .filter {
                    text.isBlank() ||
                        it.name.contains(text, ignoreCase = true) ||
                        it.abbreviation.contains(text, ignoreCase = true)
                }
                .sortedByDescending { it.id.toInt() in state.platformIds }
            if (state.allPlatforms.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(Dimens.ScreenPadding),
                    contentAlignment = Alignment.Center
                ) { LoadingIndicator() }
            }
            LazyColumn {
                items(platforms, key = { it.id }) { platform ->
                    val checked = platform.id.toInt() in state.platformIds
                    ListItem(
                        leadingContent = {
                            Checkbox(checked = checked, onCheckedChange = { onToggle(platform.id.toInt()) })
                        },
                        trailingContent = { platform.Image(Modifier.size(24.dp)) },
                        modifier = Modifier.clickable { onToggle(platform.id.toInt()) }
                    ) { Text(platform.name) }
                }
            }
        }
    }
}

/** The images of the game picked in the game list, offered as the banner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BannerImagesSheet(
    game: Game,
    options: List<BannerOption>,
    onSelect: (BannerOption) -> Unit,
    onDismiss: () -> Unit,
) = ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
        Modifier.padding(horizontal = Dimens.ScreenPadding).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)
    ) {
        Text(game.name, style = MaterialTheme.typography.titleMediumEmphasized)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap),
        ) {
            items(options, key = { it.imageId }) { option ->
                val imageModifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(BANNER_ASPECT)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onSelect(option) }
                GameBannerImage(
                    option.imageId,
                    modifier = imageModifier,
                    loadingModifier = imageModifier,
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
